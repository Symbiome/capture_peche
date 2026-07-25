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

// Style MapLibre partagé « nos layers » : fonds IGN Géoplateforme (Plan IGN v2 /
// BD Ortho) + réseau hydrographique en tuiles vectorielles MVT servies par le
// backend Quarkus (#8). Mutualisé par les cartes migrées de Leaflet vers MapLibre
// (#33) — la carte de sélection d'entité (MapLibreMap) garde son propre style
// (couches de sélection en plus).

import maplibregl, { Map as MlMap, StyleSpecification } from 'maplibre-gl';
import Constants from '@/services/Constants';

export const IGN_ATTRIBUTION =
    '© <a href="https://www.ign.fr/" target="_blank" rel="noopener">IGN</a>';

const IGN_WMTS = 'https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0'
    + '&STYLE=normal&TILEMATRIXSET=PM&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}';
const IGN_PLAN_URL = `${IGN_WMTS}&LAYER=GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2&FORMAT=image/png`;
const IGN_ORTHO_URL = `${IGN_WMTS}&LAYER=ORTHOIMAGERY.ORTHOPHOTOS&FORMAT=image/jpeg`;

// Tuiles vectorielles du réseau hydro (public, #8 backend).
const HYDRO_TILES_URL = `${Constants.baseApiUrl()}/v1/tiles/hydro/{z}/{x}/{y}.pbf`;

// Emprise par défaut (Haute-Savoie, base de recette 74).
export const DEFAULT_CENTER: [number, number] = [6.13, 45.9];
export const DEFAULT_ZOOM = 10;

export type BaseLayer = 'plan' | 'satellite';

/** Style MapLibre autoportant (aucune URL externe) : IGN + réseau hydro. */
export function buildFisholaStyle(baseLayer: BaseLayer = 'plan'): StyleSpecification {
    return {
        version: 8,
        sources: {
            'ign-plan': {
                type: 'raster', tiles: [IGN_PLAN_URL], tileSize: 256, maxzoom: 19,
                attribution: IGN_ATTRIBUTION,
            },
            'ign-ortho': {
                type: 'raster', tiles: [IGN_ORTHO_URL], tileSize: 256, maxzoom: 19,
                attribution: IGN_ATTRIBUTION,
            },
            hydro: {
                type: 'vector', tiles: [HYDRO_TILES_URL], minzoom: 10, maxzoom: 16,
                attribution: IGN_ATTRIBUTION,
            },
        },
        layers: [
            {
                id: 'ign-plan', type: 'raster', source: 'ign-plan',
                layout: { visibility: baseLayer === 'plan' ? 'visible' : 'none' },
            },
            {
                id: 'ign-ortho', type: 'raster', source: 'ign-ortho',
                layout: { visibility: baseLayer === 'satellite' ? 'visible' : 'none' },
            },
            {
                id: 'hydro-surface', type: 'fill', source: 'hydro', 'source-layer': 'water_surface',
                paint: { 'fill-color': '#1e9bc4', 'fill-opacity': 0.35, 'fill-outline-color': '#1478a0' },
            },
            {
                id: 'hydro-river-persistent', type: 'line', source: 'hydro', 'source-layer': 'river_section',
                filter: ['==', ['to-boolean', ['get', 'persistent']], true],
                paint: { 'line-color': '#1e9bc4', 'line-width': ['interpolate', ['linear'], ['zoom'], 10, 1, 16, 3] },
            },
            {
                id: 'hydro-river-intermittent', type: 'line', source: 'hydro', 'source-layer': 'river_section',
                filter: ['!=', ['to-boolean', ['get', 'persistent']], true],
                paint: {
                    'line-color': '#1e9bc4',
                    'line-width': ['interpolate', ['linear'], ['zoom'], 10, 1, 16, 2.5],
                    'line-dasharray': [2, 2],
                },
            },
        ],
    };
}

/** Toggle Plan/Satellite sur une carte construite avec {@link buildFisholaStyle}. */
export function setBaseLayer(map: MlMap, baseLayer: BaseLayer) {
    map.setLayoutProperty('ign-plan', 'visibility', baseLayer === 'plan' ? 'visible' : 'none');
    map.setLayoutProperty('ign-ortho', 'visibility', baseLayer === 'satellite' ? 'visible' : 'none');
}

/** Crée une carte MapLibre « Fishola » (style IGN + hydro, contrôle de navigation). */
export function createFisholaMap(
    container: HTMLElement,
    opts: { center?: [number, number]; zoom?: number; baseLayer?: BaseLayer } = {},
): MlMap {
    const map = new maplibregl.Map({
        container,
        style: buildFisholaStyle(opts.baseLayer || 'plan'),
        center: opts.center || DEFAULT_CENTER,
        zoom: opts.zoom != null ? opts.zoom : DEFAULT_ZOOM,
        attributionControl: { compact: true },
    });
    map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-left');
    return map;
}
