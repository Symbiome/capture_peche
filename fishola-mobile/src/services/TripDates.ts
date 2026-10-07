/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import moment from "moment";
import Helpers from "@/services/Helpers";

/** Champs de dates d'une sortie, communs aux modèles mobiles (TripMeta, TripSummary, TripBean…). */
export interface TripPeriodFields {
  date?: Date;
  startedAt?: string;
  /** Date de fin AAAA-MM-JJ (#237) ; absente pour une sortie saisie avant #237. */
  endDate?: string;
  finishedAt?: string;
}

/** Champs d'horodatage d'une capture. */
export interface CatchTimeFields {
  /** Jour de la capture AAAA-MM-JJ (#237). */
  caughtOn?: string;
  caughtAt?: string;
}

/** Erreurs de saisie des dates et heures d'une sortie, vides si le champ est valide. */
export interface TripPeriodErrors {
  dateError: string;
  startedAtError: string;
  endDateError: string;
  finishedAtError: string;
}

/**
 * Règles de dates des sorties de plusieurs jours (#237), alignées sur le backend
 * (TripTimestamps) : une sortie a une date et une heure de début, une date et une
 * heure de fin, et chaque capture doit tomber entre les deux.
 */
export default class TripDates {
  static readonly CATCH_OUT_OF_TRIP =
    "La date et l'heure de la capture doivent être comprises entre le début et la fin de la sortie";
  static readonly DEFAULT_MAX_PLAUSIBLE_TRIP_DAYS = 7;

  /** Date locale au format AAAA-MM-JJ (jamais en UTC, cf. #237). */
  static toIsoDate(date: Date): string {
    return Helpers.formatToDate(date);
  }

  /** Date locale à minuit depuis AAAA-MM-JJ. */
  static parseIsoDate(isoDate: string): Date {
    const [year, month, day] = isoDate.split("-").map(Number);
    return new Date(year, month - 1, day);
  }

  /** Date AAAA-MM-JJ depuis le format du backend ([année, mois, jour]) ou une chaîne ISO. */
  static fromBackendDate(value?: number[] | string | null): string | undefined {
    if (!value) {
      return undefined;
    }
    if (Array.isArray(value)) {
      return TripDates.toIsoDate(new Date(value[0], value[1] - 1, value[2]));
    }
    return value.substring(0, 10);
  }

  /** Instant local d'un jour AAAA-MM-JJ à une heure HH:mm ou HH:mm:ss. */
  static atTime(isoDate: string, time: string): moment.Moment {
    return moment(`${isoDate}T${time}`);
  }

  static begin(trip: TripPeriodFields): moment.Moment | undefined {
    if (!trip.date || !trip.startedAt) {
      return undefined;
    }
    return TripDates.atTime(TripDates.toIsoDate(trip.date), trip.startedAt);
  }

  /**
   * Fin de la sortie ; sans date de fin (sortie saisie avant #237), règle historique
   * « heure de fin avant l'heure de début → lendemain ».
   */
  static end(trip: TripPeriodFields): moment.Moment | undefined {
    const begin = TripDates.begin(trip);
    if (!begin || !trip.finishedAt) {
      return undefined;
    }
    if (trip.endDate) {
      return TripDates.atTime(trip.endDate, trip.finishedAt);
    }
    const sameDay = TripDates.atTime(TripDates.toIsoDate(trip.date!), trip.finishedAt);
    return sameDay.isBefore(begin) ? sameDay.add(1, "day") : sameDay;
  }

  /** Date de fin AAAA-MM-JJ, calculée par la règle historique si elle n'est pas renseignée. */
  static endIsoDate(trip: TripPeriodFields): string | undefined {
    const end = TripDates.end(trip);
    return end ? end.format("YYYY-MM-DD") : trip.endDate;
  }

  /** Durée en secondes ; une sortie en cours (sans fin) dure jusqu'à {@code now}. */
  static durationSeconds(trip: TripPeriodFields, now: moment.Moment = moment()): number {
    const begin = TripDates.begin(trip);
    if (!begin) {
      return 0;
    }
    const end = TripDates.end(trip) || now;
    return Math.max(0, end.diff(begin, "seconds"));
  }

  /** Durée affichée (« 1d 16h 30min »), jusqu'à maintenant pour une sortie en cours. */
  static formatDuration(trip: TripPeriodFields, includeSeconds: boolean = false): string {
    return Helpers.formatDuration(moment.duration(TripDates.durationSeconds(trip), "seconds"), includeSeconds);
  }

  /** La sortie finit (ou, en cours, se poursuit à {@code now}) un autre jour que celui où elle commence. */
  static isMultiDay(trip: TripPeriodFields, now: moment.Moment = moment()): boolean {
    const begin = TripDates.begin(trip);
    const end = TripDates.end(trip) || now;
    return !!begin && !end.isSame(begin, "day");
  }

  /** « du 12/09 18:00 au 14/09 10:00 » pour une sortie terminée de plusieurs jours, vide sinon. */
  static formatMultiDayPeriod(trip: TripPeriodFields): string {
    if (!TripDates.end(trip) || !TripDates.isMultiDay(trip)) {
      return "";
    }
    const pattern = "DD/MM HH:mm";
    return `du ${TripDates.begin(trip)!.format(pattern)} au ${TripDates.end(trip)!.format(pattern)}`;
  }

  /**
   * Instant de la capture dans la sortie : jour de capture s'il est connu, sinon premier
   * jour de la sortie où l'heure tombe dans la sortie (comme le backend). Une sortie en
   * cours, sans fin, s'étend jusqu'à {@code now}. Vide si la capture est hors de la sortie.
   */
  static catchMoment(trip: TripPeriodFields, aCatch: CatchTimeFields,
                     now: moment.Moment = moment()): moment.Moment | undefined {
    const begin = TripDates.begin(trip);
    if (!begin || !aCatch.caughtAt) {
      return undefined;
    }
    const end = TripDates.end(trip) || now;
    const isWithin = (instant: moment.Moment) => !instant.isBefore(begin) && !instant.isAfter(end);
    if (aCatch.caughtOn) {
      const candidate = TripDates.atTime(aCatch.caughtOn, aCatch.caughtAt);
      return isWithin(candidate) ? candidate : undefined;
    }
    for (const day = begin.clone().startOf("day"); !day.isAfter(end, "day"); day.add(1, "day")) {
      const candidate = TripDates.atTime(day.format("YYYY-MM-DD"), aCatch.caughtAt);
      if (isWithin(candidate)) {
        return candidate;
      }
    }
    return undefined;
  }

  /** Une capture sans heure est toujours acceptée ; sinon elle doit tomber dans la sortie. */
  static isCatchWithinTrip(trip: TripPeriodFields, aCatch: CatchTimeFields,
                           now: moment.Moment = moment()): boolean {
    return !aCatch.caughtAt || !!TripDates.catchMoment(trip, aCatch, now);
  }

  /** Message d'une capture hors de la sortie, avec les bornes de la sortie. */
  static catchOutOfTripMessage(trip: TripPeriodFields): string {
    const begin = TripDates.begin(trip);
    if (!begin) {
      return TripDates.CATCH_OUT_OF_TRIP;
    }
    const end = TripDates.end(trip);
    const pattern = "DD/MM HH:mm";
    const until = end ? `au ${end.format(pattern)}` : "à maintenant";
    return `${TripDates.CATCH_OUT_OF_TRIP} (du ${begin.format(pattern)} ${until})`;
  }

  /** Nombre de captures qui ne tombent pas dans la sortie. */
  static countCatchsOutsideTrip(trip: TripPeriodFields, catchs?: CatchTimeFields[]): number {
    return (catchs || []).filter((aCatch) => !TripDates.isCatchWithinTrip(trip, aCatch)).length;
  }

  /**
   * Contrôle des dates et heures saisies : toutes obligatoires, dates pas dans le futur,
   * fin strictement après le début, et captures déjà saisies toujours dans la sortie.
   */
  static validatePeriod(trip: TripPeriodFields, catchs?: CatchTimeFields[],
                        today: moment.Moment = moment()): TripPeriodErrors {
    const errors: TripPeriodErrors = { dateError: "", startedAtError: "", endDateError: "", finishedAtError: "" };
    const isInFuture = (isoDate: string) => moment(isoDate).isAfter(today, "day");
    if (!trip.date) {
      errors.dateError = "Vous devez renseigner la date de début";
    } else if (isInFuture(TripDates.toIsoDate(trip.date))) {
      errors.dateError = "La date ne peut être dans le futur";
    }
    if (!trip.startedAt) {
      errors.startedAtError = "Vous devez renseigner l'heure de début";
    }
    if (!trip.endDate) {
      errors.endDateError = "Vous devez renseigner la date de fin";
    } else if (isInFuture(trip.endDate)) {
      errors.endDateError = "La date ne peut être dans le futur";
    }
    if (!trip.finishedAt) {
      errors.finishedAtError = "Vous devez renseigner l'heure de fin";
    }
    const hasError = Object.values(errors).some((error) => !!error);
    if (hasError) {
      return errors;
    }
    if (!TripDates.end(trip)!.isAfter(TripDates.begin(trip)!)) {
      errors.finishedAtError = "La fin doit être après le début";
      return errors;
    }
    const outside = TripDates.countCatchsOutsideTrip(trip, catchs);
    if (outside > 0) {
      errors.finishedAtError = outside == 1
        ? "Une capture est en dehors de la sortie : corrigez sa date et son heure ou celles de la sortie"
        : `${outside} captures sont en dehors de la sortie : corrigez leur date et leur heure ou celles de la sortie`;
    }
    return errors;
  }

  /** La sortie dure plus que la durée plausible paramétrée : à faire confirmer. */
  static exceedsPlausibleDuration(trip: TripPeriodFields, maxPlausibleTripDays: number): boolean {
    return TripDates.durationSeconds(trip) > maxPlausibleTripDays * 24 * 3600;
  }
}
