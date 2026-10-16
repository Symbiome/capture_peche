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
import {
  BadgeBean,
  BadgeSouvenirBean,
  SouvenirCatchBean,
  SouvenirTripBean,
} from "@/pojos/BackendPojos";
import moment from "moment";

/** `generic` : carte actuelle (#146), sans prise associée ou souvenir indisponible. */
export type SouvenirLayout = "generic" | "catch" | "mosaic" | "trips";

export interface SouvenirMapPoint {
  lat: number;
  lng: number;
}

const MOSAIC_RULE_TYPES = ["species_diversity", "photo_count"];
const MOSAIC_MAX_COLUMNS = 3;
const MAX_LISTED_PLACES = 3;
// Au-delà (ex. métropole et outre-mer), une carte à l'échelle du globe n'apprend
// plus rien et peine à se charger : pas de mini-carte.
const MAX_MAP_SPAN_DEGREES = 12;
// Toponymes importés avant V1.9.0, suffixés de l'identifiant BD TOPO
// (« Lac Blanc_PLANDEAU0000… ») ou réduits à lui pour un milieu sans nom.
const BDTOPO_ID_SUFFIX = /_?[A-Z]+\d{10,}$/;

/**
 * Contenu de la carte souvenir d'un badge (#275) : mise en page et phrases
 * d'accroche (figées dans l'application) déduites du badge et des prises qui
 * l'ont débloqué. Sans souvenir exploitable, retombe sur le contenu générique
 * de la carte de partage (#146, #90).
 */
export default class BadgeSouvenir {
  constructor(readonly badge: BadgeBean, readonly souvenir: BadgeSouvenirBean | null) {}

  get layout(): SouvenirLayout {
    const souvenir = this.souvenir;
    if (!souvenir || this.badge.competitionName) {
      return "generic";
    }
    if (MOSAIC_RULE_TYPES.includes(this.badge.ruleType) && souvenir.catches.length > 1) {
      return "mosaic";
    }
    if (souvenir.catches.length) {
      return "catch";
    }
    return souvenir.trips.length ? "trips" : "generic";
  }

  /** La prise mise en avant : la première preuve (ex. celle qui a franchi le palier). */
  get heroCatch(): SouvenirCatchBean | null {
    return (this.souvenir && this.souvenir.catches[0]) || null;
  }

  get heroTrip(): SouvenirTripBean | null {
    if (!this.souvenir) {
      return null;
    }
    const hero = this.heroCatch;
    const heroTrip = hero ? this.souvenir.trips.find((t) => t.id === hero.tripId) : undefined;
    return heroTrip || this.souvenir.trips[0] || null;
  }

  get mosaicColumns(): number {
    const count = this.souvenir ? this.souvenir.catches.length : 0;
    return count <= 4 ? 2 : MOSAIC_MAX_COLUMNS;
  }

  get mosaicRows(): number {
    return Math.max(1, Math.ceil(this.mosaicCatches.length / this.mosaicColumns));
  }

  get mosaicCatches(): SouvenirCatchBean[] {
    const columns = this.mosaicColumns;
    return this.souvenir ? this.souvenir.catches.slice(0, columns * columns) : [];
  }

  /** Phrase principale, ex. « Brochet de 82 cm ». */
  get headline(): string {
    switch (this.layout) {
      case "mosaic":
        return this.mosaicHeadline();
      case "catch":
        return this.catchHeadline(this.heroCatch!);
      case "trips":
        return this.tripsHeadline();
      default:
        return this.badge.competitionName || this.badge.description;
    }
  }

  /** Ligne secondaire : plus belle prise, ou milieux d'une carte multi-sorties. */
  detail(hidePlace: boolean): string {
    const souvenir = this.souvenir;
    if (!souvenir) {
      return "";
    }
    if (this.layout === "catch" && this.isSessionRecord()) {
      return `Plus belle prise : ${this.catchLabel(this.heroCatch!)}`;
    }
    if (this.layout === "catch" && this.badge.ruleType === "species_annual_tier" && souvenir.catches.length > 1) {
      return `Plus belle de l'année : ${this.catchLabel(souvenir.catches[1])}`;
    }
    if (this.layout === "trips" && souvenir.trips.length > 1 && !hidePlace) {
      return this.listedPlaces(souvenir.trips);
    }
    return "";
  }

  /** « Lac du Bourget, Aix-les-Bains — 14/09/2026 » ; la date seule si le lieu est masqué. */
  placeLine(hidePlace: boolean): string {
    if (this.layout === "generic") {
      return this.competitionDate();
    }
    const trip = this.heroTrip;
    if (!trip || this.layout === "mosaic" || (this.layout === "trips" && this.souvenir!.trips.length > 1)) {
      return "";
    }
    const date = this.formatDate(this.heroCatch?.caughtAt || trip.beginTimestamp);
    const place = hidePlace ? "" : this.placeLabel(trip);
    return [place, date].filter((part) => !!part).join(" — ");
  }

  get hasPlace(): boolean {
    return this.layout !== "generic" && (this.mapPoints.length > 0 || !!(this.heroTrip && this.placeLabel(this.heroTrip)));
  }

  /** Positions pour la mini-carte (rendue approximative) ; centroïde du milieu à défaut. */
  get mapPoints(): SouvenirMapPoint[] {
    if (this.layout === "generic" || !this.souvenir) {
      return [];
    }
    const points: SouvenirMapPoint[] = [];
    this.souvenir.trips.forEach((trip) => points.push(...this.tripPoints(trip)));
    this.souvenir.catches.forEach((c) => this.pushPoint(points, c.latitude, c.longitude));
    return this.spanDegrees(points) <= MAX_MAP_SPAN_DEGREES ? points : [];
  }

  private spanDegrees(points: SouvenirMapPoint[]): number {
    if (!points.length) {
      return 0;
    }
    const lats = points.map((p) => p.lat);
    const lngs = points.map((p) => p.lng);
    return Math.max(Math.max(...lats) - Math.min(...lats), Math.max(...lngs) - Math.min(...lngs));
  }

  private tripPoints(trip: SouvenirTripBean): SouvenirMapPoint[] {
    const points: SouvenirMapPoint[] = [];
    this.pushPoint(points, trip.beginLatitude, trip.beginLongitude);
    this.pushPoint(points, trip.endLatitude, trip.endLongitude);
    (trip.catchPositions || []).forEach((p) => this.pushPoint(points, p.lat, p.lng));
    if (!points.length) {
      this.pushPoint(points, trip.waterEntityLatitude, trip.waterEntityLongitude);
    }
    return points;
  }

  private pushPoint(points: SouvenirMapPoint[], lat?: number | null, lng?: number | null) {
    if (lat != null && lng != null) {
      points.push({ lat, lng });
    }
  }

  private mosaicHeadline(): string {
    const count = this.souvenir!.catches.length;
    return this.badge.ruleType === "photo_count" ? `${count} prises en photo` : `${count} espèces au tableau`;
  }

  private catchHeadline(hero: SouvenirCatchBean): string {
    if (this.isSessionRecord()) {
      const count = this.badge.context && (this.badge.context as any).catchCount;
      return count ? `${count} prises en une sortie` : this.catchLabel(hero);
    }
    if (this.badge.ruleType === "session_time_window" && hero.caughtAt) {
      return `${this.catchLabel(hero)} à ${moment(hero.caughtAt).format("HH:mm")}`;
    }
    return this.catchLabel(hero);
  }

  private tripsHeadline(): string {
    const trips = this.souvenir!.trips;
    const trip = trips[0];
    switch (this.badge.ruleType) {
      case "altitude_threshold":
        return this.altitudeHeadline(trip);
      case "multi_department":
        return `${trips.length} départements explorés`;
      case "basin_diversity":
        return `${trips.length} bassins explorés`;
      case "season_coverage":
        return "Une sortie à chaque saison";
      default:
        return trips.length > 1
          ? `${trips.length} milieux pêchés`
          : `Sortie du ${this.formatDate(trip.beginTimestamp)}`;
    }
  }

  private altitudeHeadline(trip: SouvenirTripBean): string {
    const name = this.waterEntityName(trip) || "Lac d'altitude";
    return trip.waterEntityAltitude ? `${name} — ${Math.round(trip.waterEntityAltitude)} m d'altitude` : name;
  }

  private isSessionRecord(): boolean {
    return this.badge.code === "RECORD_NOMBRE";
  }

  /** « Brochet de 82 cm », « Carpe commune de 8,5 kg » ; le poids d'abord pour le record de poids. */
  private catchLabel(aCatch: SouvenirCatchBean): string {
    const species = aCatch.speciesName || "Belle prise";
    const size = aCatch.size ? `${aCatch.size} cm` : "";
    const weight = aCatch.weight ? this.formatWeight(aCatch.weight) : "";
    const measure = this.badge.code === "RECORD_POIDS" ? weight || size : size || weight;
    return measure ? `${species} de ${measure}` : species;
  }

  private formatWeight(grams: number): string {
    if (grams < 1000) {
      return `${grams} g`;
    }
    const kilograms = Math.round(grams / 100) / 10;
    return `${String(kilograms).replace(".", ",")} kg`;
  }

  private waterEntityName(trip: SouvenirTripBean): string {
    return (trip.waterEntityName || "").replace(BDTOPO_ID_SUFFIX, "").trim();
  }

  private placeLabel(trip: SouvenirTripBean): string {
    const area = trip.commune || trip.departmentName;
    return [this.waterEntityName(trip), area].filter((part) => !!part).join(", ");
  }

  private listedPlaces(trips: SouvenirTripBean[]): string {
    const names: string[] = [];
    trips.forEach((t) => {
      const name = this.waterEntityName(t);
      if (name && names.indexOf(name) === -1) {
        names.push(name);
      }
    });
    const listed = names.slice(0, MAX_LISTED_PLACES).join(", ");
    return names.length > MAX_LISTED_PLACES ? `${listed}…` : listed;
  }

  // #90, même conversion défensive que Badges.vue : LocalDate arrive en [y, m(1-12), d].
  private competitionDate(): string {
    const d: any = this.badge.competitionDate;
    if (!this.badge.competitionName || !d) {
      return "";
    }
    if (Array.isArray(d)) {
      const [year, month, day] = d;
      return moment([year, month - 1, day]).format("DD/MM/YYYY");
    }
    return moment(d).format("DD/MM/YYYY");
  }

  private formatDate(isoDateTime: any): string {
    return isoDateTime ? moment(isoDateTime).format("DD/MM/YYYY") : "";
  }
}
