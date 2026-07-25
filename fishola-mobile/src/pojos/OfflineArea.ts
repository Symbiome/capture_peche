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

/**
 * Un pack hydrographique départemental téléchargé pour l'usage hors-ligne (#54,
 * « télécharger un département façon Komoot »). On ne stocke QUE les entités
 * hydro (pas de fond de carte / tuiles, hors périmètre) : un FeatureCollection
 * GeoJSON (une feature par entité, géométrie simplifiée) rendu par MapLibre
 * comme source locale quand les tuiles vectorielles ne sont pas joignables.
 */
export default interface OfflineArea {
  /** Code département (clé Dexie) : « 74 », « 2A », « 971 »… */
  code: string;
  /** Nom lisible (« Haute-Savoie »). */
  name: string;
  /** FeatureCollection GeoJSON des entités hydro du département. */
  geojson: any;
  /** Nombre d'entités (= features), pour l'affichage. */
  entityCount: number;
  /** Taille approximative du pack en octets (JSON sérialisé), pour l'affichage. */
  bytes: number;
  /** Horodatage du téléchargement (epoch ms). */
  downloadedAt: number;
}
