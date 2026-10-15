/*-
 * #%L
 * Fishola :: Admin
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
import type { MapPosition } from "@/components/WaterEntityMapPicker.vue";

/** Référentiel fermé partagé avec le carnet volontaire (SurveySchema.MODES_PECHE, #145). */
export const FISHING_MODES = ["bateau", "float tube/canoë", "bord itinérant", "bord statique"];
export const DAY_PERIODS = ["matin", "après-midi", "journée entière", "soirée"];

/** Modalités d'« Espèce recherchée » sans espèce précise (#236), « Toutes espèces » par défaut. */
export const ALL_SPECIES = "__ALL__";
export const NO_SPECIES = "__NONE__";

export const SURVEY_SESSIONS_URI = "/v1/admin/manual-entries/survey/sessions";

/** Session d'enquête renvoyée par le backend (SurveySessionView). */
export interface SurveySession {
  id: string;
  code: string;
  day: string;
  startTime: string | null;
  endTime: string | null;
  waterEntityId: string | null;
  waterEntityName: string | null;
  operatorName: string | null;
  status: "ouverte" | "cloturee";
  unsurveyedShoreAnglers: number | null;
  unsurveyedBoatAnglers: number | null;
  sortiesCount: number;
  anglersCount: number;
}

/** Erreur de saisie renvoyée par le backend (ManualError). */
export interface SurveyError {
  index: number | null;
  field: string | null;
  message: string;
}

/** Nouvelle capture vide, individuelle par défaut. */
export function newCapture() {
  return { speciesId: null, quantity: 1, size: null, lotMinSize: null, lotMaxSize: null, kept: false };
}

/** Nouveau bloc souvenir vide, avec une capture à renseigner. */
export function newSouvenir() {
  return {
    day: "",
    dayPeriod: DAY_PERIODS[0],
    waterEntityId: null,
    position: null as MapPosition | null,
    fishingMode: FISHING_MODES[0],
    techniqueId: null,
    rodCount: 1,
    baitOrLure: "",
    expectedSpecies: ALL_SPECIES,
    bredouille: false,
    captures: [newCapture()]
  };
}

/** Nouveau pêcheur interrogé vide, sans souvenir. */
export function newAngler() {
  return {
    origin: "",
    fishingMode: FISHING_MODES[0],
    techniqueId: null,
    rodCount: 1,
    baitOrLure: "",
    expectedSpecies: ALL_SPECIES,
    bredouille: false,
    captures: [newCapture()],
    hasSouvenir: false,
    souvenir: newSouvenir()
  };
}

/** Une capture de plus d'un individu est un lot, décrit par ses tailles min et max. */
export function isLot(capture: any): boolean {
  return Number(capture.quantity) > 1;
}

/** Entier saisi, ou null si le champ est vide ou invalide. */
export function toIntOrNull(value: any): number | null {
  if (value === "" || value === null || value === undefined) return null;
  const n = Number(value);
  return Number.isFinite(n) ? Math.trunc(n) : null;
}

/** Identifiant d'espèce envoyé au backend : null pour « Toutes espèces » et « Aucune ». */
export function expectedSpeciesId(choice: string | null): string | null {
  return !choice || choice === ALL_SPECIES || choice === NO_SPECIES ? null : choice;
}

function cleanCapture(capture: any) {
  const lot = isLot(capture);
  return {
    speciesId: capture.speciesId,
    quantity: toIntOrNull(capture.quantity),
    size: lot ? null : toIntOrNull(capture.size),
    lotMinSize: lot ? toIntOrNull(capture.lotMinSize) : null,
    lotMaxSize: lot ? toIntOrNull(capture.lotMaxSize) : null,
    kept: !!capture.kept
  };
}

function cleanSouvenir(angler: any) {
  if (!angler.hasSouvenir) return null;
  const s = angler.souvenir;
  return {
    day: s.day || null,
    dayPeriod: s.dayPeriod,
    waterEntityId: s.waterEntityId,
    latitude: s.position ? s.position.lat : null,
    longitude: s.position ? s.position.lng : null,
    fishingMode: s.fishingMode,
    techniqueId: s.techniqueId,
    rodCount: toIntOrNull(s.rodCount),
    baitOrLure: s.baitOrLure || null,
    expectedSpeciesId: expectedSpeciesId(s.expectedSpecies),
    bredouille: !!s.bredouille,
    captures: s.bredouille ? [] : s.captures.map(cleanCapture)
  };
}

/** Pêcheur saisi, au format attendu par le backend (SurveyAnglerBean). */
export function cleanAngler(angler: any) {
  return {
    origin: angler.origin || null,
    fishingMode: angler.fishingMode,
    techniqueId: angler.techniqueId,
    rodCount: toIntOrNull(angler.rodCount),
    baitOrLure: angler.baitOrLure || null,
    expectedSpeciesId: expectedSpeciesId(angler.expectedSpecies),
    bredouille: !!angler.bredouille,
    captures: angler.bredouille ? [] : angler.captures.map(cleanCapture),
    souvenir: cleanSouvenir(angler)
  };
}

/** Erreurs renvoyées par le backend, ou message générique si la requête a échoué autrement. */
export function errorsOf(rejected: any): SurveyError[] {
  const content = rejected && rejected.content;
  if (content && content.errors && content.errors.length) {
    return content.errors;
  }
  return [{ index: null, field: null, message: "Échec de l'enregistrement (réessayer)." }];
}

/** Date AAAA-MM-JJ affichée en JJ/MM/AAAA. */
export function frenchDate(iso: string | null): string {
  if (!iso) return "";
  const [year, month, day] = iso.split("-");
  return `${day}/${month}/${year}`;
}
