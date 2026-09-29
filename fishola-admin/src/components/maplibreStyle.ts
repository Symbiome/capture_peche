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

// Fond de carte des saisies manuelles staff (#189) : même rendu que la carte de
// l'app pêcheur (fishola-mobile/src/components/common/maplibreStyle.ts) — IGN
// Plan/Satellite + réseau hydro vectoriel servi par le backend.

import { StyleSpecification } from "maplibre-gl";
import Constants from "@/services/Constants";

const IGN_ATTRIBUTION =
  '© <a href="https://www.ign.fr/" target="_blank" rel="noopener">IGN</a>';

const IGN_WMTS = "https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0"
  + "&STYLE=normal&TILEMATRIXSET=PM&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}";
const IGN_PLAN_URL = `${IGN_WMTS}&LAYER=GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2&FORMAT=image/png`;
const IGN_ORTHO_URL = `${IGN_WMTS}&LAYER=ORTHOIMAGERY.ORTHOPHOTOS&FORMAT=image/jpeg`;
const HYDRO_TILES_URL = `${Constants.baseApiUrl()}/v1/tiles/hydro/{z}/{x}/{y}.pbf`;

/** Emprise par défaut quand aucun point n'est encore saisi (Haute-Savoie, comme l'app pêcheur). */
export const DEFAULT_CENTER: [number, number] = [6.13, 45.9];
export const DEFAULT_ZOOM = 10;

export type BaseLayer = "plan" | "satellite";

/** Style MapLibre autoportant : IGN + réseau hydro. */
export function buildFisholaStyle(baseLayer: BaseLayer = "plan"): StyleSpecification {
  return {
    version: 8,
    sources: {
      "ign-plan": { type: "raster", tiles: [IGN_PLAN_URL], tileSize: 256, maxzoom: 19, attribution: IGN_ATTRIBUTION },
      "ign-ortho": { type: "raster", tiles: [IGN_ORTHO_URL], tileSize: 256, maxzoom: 19, attribution: IGN_ATTRIBUTION },
      hydro: { type: "vector", tiles: [HYDRO_TILES_URL], minzoom: 10, maxzoom: 16, attribution: IGN_ATTRIBUTION }
    },
    layers: [
      { id: "ign-plan", type: "raster", source: "ign-plan", layout: { visibility: baseLayer === "plan" ? "visible" : "none" } },
      { id: "ign-ortho", type: "raster", source: "ign-ortho", layout: { visibility: baseLayer === "satellite" ? "visible" : "none" } },
      {
        id: "hydro-surface", type: "fill", source: "hydro", "source-layer": "water_surface",
        paint: { "fill-color": "#1e9bc4", "fill-opacity": 0.35, "fill-outline-color": "#1478a0" }
      },
      {
        id: "hydro-river", type: "line", source: "hydro", "source-layer": "river_section",
        paint: { "line-color": "#1e9bc4", "line-width": ["interpolate", ["linear"], ["zoom"], 10, 1, 16, 3] }
      }
    ]
  };
}
