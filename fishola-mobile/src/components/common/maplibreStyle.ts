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

// ─── Marqueur « sortie » (pin poisson) ───────────────────────────────────────

/**
 * Pin en forme de goutte portant une silhouette de poisson, dessiné en SVG et
 * enregistré comme image MapLibre (`icon-image`). Un simple cercle ne disait pas
 * de quoi il s'agissait ; la goutte donne en plus un point d'ancrage précis
 * (la pointe est posée sur la coordonnée, cf. `icon-anchor: 'bottom'`).
 *
 * @param color couleur de remplissage (code maillage : bleu maillée / orange sinon)
 */
function buildCatchPinSvg(color: string): string {
    // Dessiné à 2× (96×130 px) pour rester net sur écran haute densité : associé à
    // `pixelRatio: 2` à l'enregistrement, le pin s'affiche à 48×65 px sur la carte.
    return `<svg xmlns="http://www.w3.org/2000/svg" width="96" height="130" viewBox="0 0 34 46">
  <path d="M17 1C8.7 1 2 7.7 2 16c0 10.5 13 26.5 14.1 27.8a1.2 1.2 0 0 0 1.8 0C19 42.5 32 26.5 32 16 32 7.7 25.3 1 17 1z"
        fill="${color}" stroke="#ffffff" stroke-width="2.5" stroke-linejoin="round"/>
  <g fill="#ffffff">
    <path d="M20.8 15.9c0 2.6-2.7 4.7-6 4.7-2.3 0-4.4-1-5.4-2.6-.2-.3-.2-.7 0-1 1-1.6 3.1-2.6 5.4-2.6 3.3 0 6 2.1 6 4.7z"/>
    <path d="M21.2 13.6c.5-.3 1.1 0 1.1.6v3.4c0 .6-.6.9-1.1.6l-2.4-1.4c-.4-.3-.4-.9 0-1.2l2.4-2z"/>
    <circle cx="13.2" cy="14.9" r="0.9" fill="${color}"/>
  </g>
</svg>`;
}

/**
 * Charge le pin « sortie » dans la carte sous l'identifiant `id`.
 * À appeler avant d'ajouter la couche `symbol` qui l'utilise.
 */
export function addCatchPinIcon(map: MlMap, id: string, color: string): Promise<void> {
    return new Promise((resolve) => {
        if (map.hasImage(id)) {
            resolve();
            return;
        }
        const img = new Image(96, 130);
        img.onload = () => {
            if (!map.hasImage(id)) {
                map.addImage(id, img, { pixelRatio: 2 });
            }
            resolve();
        };
        // En cas d'échec on n'empêche pas l'affichage de la carte.
        img.onerror = () => resolve();
        img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(buildCatchPinSvg(color));
    });
}

// ─── Entités hydro : ciblage et infobulle de survol ──────────────────────────
// Mutualisé entre toutes les cartes (sélection d'entité, position, marqueurs)
// pour que le réseau hydro donne partout les mêmes informations.

/** Couches hydro interrogeables (les couches hors-ligne n'existent qu'avec un pack chargé). */
const HYDRO_QUERY_LAYERS = [
    'hydro-surface', 'hydro-river-persistent', 'hydro-river-intermittent',
    'hydro-offline-fill', 'hydro-offline-line',
];

/**
 * Interroge les entités hydro autour d'un point écran, avec une tolérance de
 * ciblage en pixels ADAPTÉE AU ZOOM : dézoomé les traits sont fins (1 px) →
 * tolérance large ; zoomé ils s'épaississent → tolérance resserrée. Une boîte
 * (au lieu du point exact) rend le survol/clic des cours d'eau bien plus facile.
 */
export function queryHydroAt(map: MlMap, point: { x: number; y: number }) {
    const zoom = map.getZoom();
    const tol = zoom < 13 ? 10 : (zoom < 15 ? 7 : 5);
    const box: [[number, number], [number, number]] = [
        [point.x - tol, point.y - tol],
        [point.x + tol, point.y + tol],
    ];
    const layers = HYDRO_QUERY_LAYERS.filter((l) => map.getLayer(l));
    return map.queryRenderedFeatures(box, { layers });
}

/** Libellé de type à partir de la couche d'origine de la feature. */
export function hydroTypeLabel(feature: any): string {
    const layer = feature.layer && feature.layer.id;
    // Hors-ligne, la couche ne distingue pas plan d'eau/cours d'eau : on
    // s'appuie sur la propriété `kind` portée par le pack.
    if (layer === 'hydro-offline-fill' || layer === 'hydro-offline-line') {
        const kind = feature.properties && feature.properties.kind;
        return kind === 'STILL' ? "Plan d'eau" : "Cours d'eau";
    }
    if (layer === 'hydro-surface') {
        return "Plan d'eau";
    }
    if (layer === 'hydro-river-intermittent') {
        return "Cours d'eau intermittent";
    }
    return "Cours d'eau";
}

function escapeHtml(value: string): string {
    return value.replace(/[&<>"']/g, (c) => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
    ));
}

/**
 * Branche l'infobulle de survol des entités hydro (nom + type) sur une carte.
 * `pointer-events: none` (CSS `.hydro-hover-popup`) pour ne jamais capter le
 * clic sous le curseur. Sans effet sur tactile (pas d'événement de survol).
 * Retourne une fonction de nettoyage à appeler à la destruction de la carte.
 */
export function attachHydroHover(map: MlMap): () => void {
    let popup: Popup | null = null;

    const onMouseMove = (e: any) => {
        const features = queryHydroAt(map, e.point);
        if (features.length > 0) {
            const feature = features[0];
            const name = (feature.properties && feature.properties.name) || 'Sans nom';
            map.getCanvas().style.cursor = 'pointer';
            const html = `<div class="hydro-tip"><strong>${escapeHtml(name as string)}</strong>`
                + `<span>${hydroTypeLabel(feature)}</span></div>`;
            if (!popup) {
                popup = new maplibregl.Popup({
                    closeButton: false,
                    closeOnClick: false,
                    offset: 12,
                    className: 'hydro-hover-popup',
                });
            }
            popup.setLngLat(e.lngLat).setHTML(html).addTo(map);
        } else {
            map.getCanvas().style.cursor = '';
            popup?.remove();
        }
    };

    map.on('mousemove', onMouseMove);
    return () => {
        map.off('mousemove', onMouseMove);
        popup?.remove();
        popup = null;
    };
}
