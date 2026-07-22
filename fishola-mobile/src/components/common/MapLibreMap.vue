<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #L%
  -->
<!--
  Carte de sélection d'une entité hydro (#8). Remplace LakesMap (Leaflet) : au
  lieu d'afficher un marqueur par centroïde de plan d'eau, on rend le RÉSEAU
  hydrographique réel (cours d'eau + surfaces) servi par le backend en tuiles
  vectorielles MVT (ST_AsMVT), au-dessus des fonds IGN Géoplateforme (Plan IGN v2
  ou BD Ortho). L'utilisateur tape un tronçon/une surface pour sélectionner
  l'entité sous-jacente.

  MapLibre n'a pas de wrapper Vue 2 maintenu : intégration impérative (init dans
  initMap(), destruction dans beforeDestroy()). Le conteneur étant masqué par un
  v-show tant que la carte n'est pas ouverte, on initialise paresseusement à la
  première ouverture (une carte MapLibre créée dans un conteneur de taille nulle
  ne se rend pas correctement).
  -->
<template>
<div class="map">
    <i class="icon-error close-button" @click="closeMap" />
    <div ref="mapContainer" class="maplibre-container" />
    <div class="map-controls">
        <button type="button" class="map-btn" @click="toggleBaseLayer">
            {{ baseLayer === 'plan' ? 'Satellite' : 'Plan' }}
        </button>
        <button type="button" class="map-btn" @click="locateMe" title="Ma position">
            Ma position
        </button>
    </div>
    <div v-if="mapIsLoading" class="is-loading">
        <div class="loader" />
        Chargement ...
    </div>
    <div v-if="useOffline && offlineHint" class="offline-hint">
        <i class="icon-error" /> {{ offlineHint }}
    </div>
</div>
</template>

<script lang="ts">
import { WaterEntity as Lake } from '@/pojos/BackendPojos';
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import Constants from '@/services/Constants';
import GeolocationService from '@/services/GeolocationService';
import NetworkStatusService from '@/services/NetworkStatusService';
import OfflineAreasService from '@/services/OfflineAreasService';

import maplibregl, { Map as MlMap, Marker, Popup, LngLatLike, LngLatBoundsLike, MapGeoJSONFeature, StyleSpecification } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';

// Flux WMTS raster ouverts de la Géoplateforme IGN (sans clé pour les couches
// essentielles). TileMatrixSet PM = Web Mercator, aligné sur la projection par
// défaut de MapLibre. Attribution « © IGN » obligatoire (portée par les sources).
const IGN_ATTRIBUTION = '© <a href="https://www.ign.fr/" target="_blank" rel="noopener">IGN</a>';
const IGN_WMTS = 'https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0'
    + '&STYLE=normal&TILEMATRIXSET=PM&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}';
const IGN_PLAN_URL = `${IGN_WMTS}&LAYER=GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2&FORMAT=image/png`;
const IGN_ORTHO_URL = `${IGN_WMTS}&LAYER=ORTHOIMAGERY.ORTHOPHOTOS&FORMAT=image/jpeg`;

// Endpoint tuiles vectorielles du réseau hydro (public, #8 backend). Les
// placeholders {z}/{x}/{y} sont ceux attendus par MapLibre.
const HYDRO_TILES_URL = `${Constants.baseApiUrl()}/v1/tiles/hydro/{z}/{x}/{y}.pbf`;

// Emprise par défaut (Haute-Savoie, base de recette 74) quand aucune entité
// n'est présélectionnée.
const DEFAULT_CENTER: [number, number] = [6.13, 45.9];
// Doit rester ≥ minzoom de la source hydro (10) : sous ce zoom, aucune tuile
// vectorielle n'est chargée, donc rien à afficher ni à taper à l'ouverture.
const DEFAULT_ZOOM = 10;

type BaseLayer = 'plan' | 'satellite';

@Component
export default class MapLibreMap extends Vue {
    @Prop() lakes: Lake[];
    @Prop() favoriteLakes: Lake[];
    @Prop() selectedLake: Lake;
    @Prop() isVisible: boolean;

    private map: MlMap | null = null;
    private favoriteMarkers: Marker[] = [];
    private pinMarker: Marker | null = null;
    private userMarker: Marker | null = null;
    private hoverPopup: Popup | null = null;
    baseLayer: BaseLayer = 'plan';
    mapIsLoading = true;

    // Mode hors-ligne (#54) : quand la connexion manque à l'ouverture, on rend le
    // réseau hydro depuis les packs départementaux téléchargés (source GeoJSON
    // locale) au lieu des tuiles vectorielles du backend, injoignables. Le fond
    // IGN reste, lui, indisponible (non téléchargé, hors périmètre) → carte nue.
    private useOffline = false;
    private offlineData: any = null;
    // Bannière d'info affichée hors-ligne (aucun pack, ou rappel fond absent).
    offlineHint = '';

    mounted() {
        if (this.isVisible) {
            this.initMap();
        }
    }

    beforeDestroy() {
        this.destroyMarkers();
        this.hoverPopup?.remove();
        this.hoverPopup = null;
        if (this.map) {
            this.map.remove();
            this.map = null;
        }
    }

    @Watch('isVisible')
    onVisibilityChange() {
        if (!this.isVisible) {
            return;
        }
        if (!this.map) {
            this.initMap();
        } else {
            // Le conteneur vient de réapparaître (v-show) : forcer le recalcul de
            // taille puis recadrer sur la sélection courante.
            this.$nextTick(() => {
                this.map?.resize();
                this.fitInitial();
            });
        }
    }

    @Watch('selectedLake')
    onSelectedLakeChange() {
        this.applySelectionHighlight();
        // On ne recadre que si l'entité sélectionnée est hors du champ visible
        // (évite le « saut » quand on tape une entité déjà à l'écran, et tout
        // recadrage parasite lors d'une désélection).
        this.recenterOnSelectedIfOffscreen();
    }

    @Watch('favoriteLakes')
    onFavoritesChange() {
        this.refreshFavoriteMarkers();
    }

    private async initMap() {
        const container = this.$refs.mapContainer as HTMLElement;
        if (!container || this.map) {
            return;
        }

        // Décision online/offline figée à l'ouverture : hors-ligne, on précharge
        // les entités hydro téléchargées pour les injecter en source GeoJSON.
        this.useOffline = NetworkStatusService.isOffline();
        if (this.useOffline) {
            try {
                this.offlineData = await OfflineAreasService.mergedFeatureCollection();
            } catch (e) {
                this.offlineData = { type: 'FeatureCollection', features: [] };
            }
            const count = (this.offlineData.features || []).length;
            this.offlineHint = count > 0
                ? "Hors-ligne : réseau téléchargé affiché (fond de carte indisponible)."
                : "Hors-ligne : aucune zone téléchargée. Réglages → Zones hors-ligne.";
            // Le conteneur a pu être détruit pendant l'attente (fermeture rapide).
            if (!this.$refs.mapContainer || this.map) {
                return;
            }
        }

        const map = new maplibregl.Map({
            container,
            style: this.buildStyle(),
            center: this.selectedCenter(),
            zoom: this.selectedHasCoords() ? 12 : DEFAULT_ZOOM,
            attributionControl: { compact: true },
        });
        this.map = map;

        map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-left');

        map.on('load', () => {
            this.mapIsLoading = false;
            this.applySelectionHighlight();
            this.refreshFavoriteMarkers();
            this.fitInitial();
        });

        // Tap sur le réseau (avec tolérance, cf. queryHydroAt). On pose TOUJOURS
        // un pin au point cliqué : il matérialise la position de départ de la
        // sortie (retour recette « pas de pin au clic sur le segment »).
        map.on('click', (e) => {
            const features = this.queryHydroAt(e.point);
            const props = features.length > 0 ? (features[0].properties || {}) : {};
            const id = props.water_entity_id as string;
            this.setPin(e.lngLat.lng, e.lngLat.lat);
            if (id) {
                // Tap direct sur une entité : sélection immédiate ET le point
                // cliqué devient la position de départ (pas besoin du flux
                // d'attribution/confirmation, l'entité est explicite). Le pin
                // reste affiché (la carte n'est pas refermée automatiquement).
                this.$emit('selectLake', id);
                this.$emit('point-picked', { lng: e.lngLat.lng, lat: e.lngLat.lat });
                return;
            }
            // Point libre : flux d'attribution (proposition + confirmation, #9).
            this.$emit('map-click', { lng: e.lngLat.lng, lat: e.lngLat.lat });
        });

        // Survol d'une entité : curseur « pointer » + infobulle (nom + type).
        // Requête map-level (pas par couche) pour appliquer une tolérance de
        // ciblage : un trait de rivière fait 1–3 px, difficile à survoler pile
        // dessus. queryHydroAt élargit la zone de détection.
        map.on('mousemove', (e) => {
            const features = this.queryHydroAt(e.point);
            if (features.length > 0) {
                const feature = features[0];
                const name = (feature.properties && feature.properties.name) || 'Sans nom';
                map.getCanvas().style.cursor = 'pointer';
                this.showHoverPopup(e.lngLat, name as string, this.hydroTypeLabel(feature));
            } else {
                map.getCanvas().style.cursor = '';
                this.hideHoverPopup();
            }
        });
    }

    // Interroge les entités hydro autour d'un point écran, avec une tolérance de
    // ciblage exprimée en pixels et ADAPTÉE AU ZOOM : dézoomé, les traits sont
    // fins (1 px) → tolérance large ; zoomé, ils s'épaississent → tolérance
    // resserrée pour rester précis. Une boîte (au lieu du point exact) rend le
    // survol/clic des cours d'eau bien plus facile.
    private queryHydroAt(point: { x: number; y: number }) {
        if (!this.map) {
            return [];
        }
        const zoom = this.map.getZoom();
        const tol = zoom < 13 ? 10 : (zoom < 15 ? 7 : 5);
        const box: [[number, number], [number, number]] = [
            [point.x - tol, point.y - tol],
            [point.x + tol, point.y + tol],
        ];
        // On interroge les couches présentes (les couches hors-ligne n'existent
        // que si un pack est chargé, les couches tuiles qu'en ligne).
        const candidates = [
            'hydro-surface', 'hydro-river-persistent', 'hydro-river-intermittent',
            'hydro-offline-fill', 'hydro-offline-line',
        ];
        const layers = candidates.filter((l) => this.map!.getLayer(l));
        return this.map.queryRenderedFeatures(box, { layers });
    }

    // Libellé de type à partir de la couche d'origine de la feature.
    private hydroTypeLabel(feature: MapGeoJSONFeature): string {
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

    // Infobulle de survol (desktop) : nom du lieu + type. `pointer-events: none`
    // (porté par la CSS de .hydro-hover-popup) pour ne jamais intercepter le clic
    // de sélection. Sans effet sur tactile (pas d'événement de survol).
    private showHoverPopup(lngLat: LngLatLike, name: string, typeLabel: string) {
        if (!this.map) {
            return;
        }
        const html = `<div class="hydro-tip"><strong>${this.escapeHtml(name)}</strong>`
            + `<span>${typeLabel}</span></div>`;
        if (!this.hoverPopup) {
            this.hoverPopup = new maplibregl.Popup({
                closeButton: false,
                closeOnClick: false,
                offset: 12,
                className: 'hydro-hover-popup',
            });
        }
        this.hoverPopup.setLngLat(lngLat).setHTML(html).addTo(this.map);
    }

    private hideHoverPopup() {
        this.hoverPopup?.remove();
    }

    // Les noms viennent de la base, mais on échappe par principe (infobulle en
    // innerHTML).
    private escapeHtml(value: string): string {
        return value.replace(/[&<>"']/g, (c) => (
            { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
        ));
    }

    // Construit le style MapLibre de A à Z (aucune URL de style externe : plus
    // simple à figer et compatible avec le mode hors-ligne #54).
    private buildStyle(): StyleSpecification {
        const sources: any = {
            'ign-plan': {
                type: 'raster',
                tiles: [IGN_PLAN_URL],
                tileSize: 256,
                maxzoom: 19,
                attribution: IGN_ATTRIBUTION,
            },
            'ign-ortho': {
                type: 'raster',
                tiles: [IGN_ORTHO_URL],
                tileSize: 256,
                maxzoom: 19,
                attribution: IGN_ATTRIBUTION,
            },
            hydro: {
                type: 'vector',
                tiles: [HYDRO_TILES_URL],
                // Les tuiles sont vides sous le zoom 10 (garde perf côté
                // backend) ; on laisse MapLibre surzoomer au-delà de 16.
                minzoom: 10,
                maxzoom: 16,
                attribution: IGN_ATTRIBUTION,
            },
        };

        const layers: any[] = [
            {
                id: 'ign-plan',
                type: 'raster',
                source: 'ign-plan',
                layout: { visibility: this.baseLayer === 'plan' ? 'visible' : 'none' },
            },
            {
                id: 'ign-ortho',
                type: 'raster',
                source: 'ign-ortho',
                layout: { visibility: this.baseLayer === 'satellite' ? 'visible' : 'none' },
            },
            {
                id: 'hydro-surface',
                type: 'fill',
                source: 'hydro',
                'source-layer': 'water_surface',
                paint: {
                    'fill-color': '#1e9bc4',
                    'fill-opacity': 0.35,
                    'fill-outline-color': '#1478a0',
                },
            },
            {
                id: 'hydro-surface-selected',
                type: 'fill',
                source: 'hydro',
                'source-layer': 'water_surface',
                filter: ['==', ['get', 'water_entity_id'], '__none__'],
                paint: {
                    'fill-color': '#e2725b',
                    'fill-opacity': 0.55,
                },
            },
            {
                // Cours d'eau permanents : trait plein.
                id: 'hydro-river-persistent',
                type: 'line',
                source: 'hydro',
                'source-layer': 'river_section',
                filter: ['==', ['to-boolean', ['get', 'persistent']], true],
                paint: {
                    'line-color': '#1e9bc4',
                    'line-width': ['interpolate', ['linear'], ['zoom'], 10, 1, 16, 3],
                },
            },
            {
                // Cours d'eau intermittents : pointillés (préfigure le warning #9).
                id: 'hydro-river-intermittent',
                type: 'line',
                source: 'hydro',
                'source-layer': 'river_section',
                filter: ['!=', ['to-boolean', ['get', 'persistent']], true],
                paint: {
                    'line-color': '#1e9bc4',
                    'line-width': ['interpolate', ['linear'], ['zoom'], 10, 1, 16, 2.5],
                    'line-dasharray': [2, 2],
                },
            },
            {
                id: 'hydro-river-selected',
                type: 'line',
                source: 'hydro',
                'source-layer': 'river_section',
                filter: ['==', ['get', 'water_entity_id'], '__none__'],
                paint: {
                    'line-color': '#e2725b',
                    'line-width': ['interpolate', ['linear'], ['zoom'], 10, 3, 16, 6],
                },
            },
        ];

        // Mode hors-ligne : le réseau hydro provient d'une source GeoJSON locale
        // (packs départementaux, #54). Une feature par entité, géométrie mixte
        // (polygone/point pour un plan d'eau, ligne pour un cours d'eau) → une
        // couche fill (plans d'eau) + une couche line (cours d'eau + contours),
        // avec leurs variantes « sélection ». `generateId` évite l'avertissement
        // MapLibre sur les id non entiers : la sélection passe, comme en ligne,
        // par un filtre sur la propriété `water_entity_id`.
        if (this.offlineData && (this.offlineData.features || []).length > 0) {
            sources['hydro-offline'] = {
                type: 'geojson',
                data: this.offlineData,
                generateId: true,
                attribution: IGN_ATTRIBUTION,
            };
            layers.push(
                {
                    id: 'hydro-offline-fill',
                    type: 'fill',
                    source: 'hydro-offline',
                    filter: ['==', ['get', 'kind'], 'STILL'],
                    paint: {
                        'fill-color': '#1e9bc4',
                        'fill-opacity': 0.35,
                        'fill-outline-color': '#1478a0',
                    },
                },
                {
                    id: 'hydro-offline-fill-selected',
                    type: 'fill',
                    source: 'hydro-offline',
                    filter: ['==', ['get', 'water_entity_id'], '__none__'],
                    paint: {
                        'fill-color': '#e2725b',
                        'fill-opacity': 0.55,
                    },
                },
                {
                    id: 'hydro-offline-line',
                    type: 'line',
                    source: 'hydro-offline',
                    paint: {
                        'line-color': '#1e9bc4',
                        'line-width': ['interpolate', ['linear'], ['zoom'], 8, 1, 16, 3],
                    },
                },
                {
                    id: 'hydro-offline-line-selected',
                    type: 'line',
                    source: 'hydro-offline',
                    filter: ['==', ['get', 'water_entity_id'], '__none__'],
                    paint: {
                        'line-color': '#e2725b',
                        'line-width': ['interpolate', ['linear'], ['zoom'], 8, 3, 16, 6],
                    },
                }
            );
        }

        return { version: 8, sources, layers };
    }

    // Met en évidence toutes les géométries de l'entité sélectionnée (un plan
    // d'eau ou un cours d'eau peut couvrir plusieurs features/tuiles) via un
    // filtre plutôt que feature-state, qui ne survit pas au découpage en tuiles.
    private applySelectionHighlight() {
        if (!this.map || !this.map.isStyleLoaded()) {
            return;
        }
        const id = this.selectedLake ? this.selectedLake.id : '__none__';
        const filter: any = ['==', ['get', 'water_entity_id'], id];
        this.map.setFilter('hydro-surface-selected', filter);
        this.map.setFilter('hydro-river-selected', filter);
        // Couches « sélection » hors-ligne (présentes seulement si un pack est
        // chargé).
        if (this.map.getLayer('hydro-offline-fill-selected')) {
            this.map.setFilter('hydro-offline-fill-selected', filter);
        }
        if (this.map.getLayer('hydro-offline-line-selected')) {
            this.map.setFilter('hydro-offline-line-selected', filter);
        }
    }

    toggleBaseLayer() {
        this.baseLayer = this.baseLayer === 'plan' ? 'satellite' : 'plan';
        if (!this.map) {
            return;
        }
        this.map.setLayoutProperty('ign-plan', 'visibility', this.baseLayer === 'plan' ? 'visible' : 'none');
        this.map.setLayoutProperty('ign-ortho', 'visibility', this.baseLayer === 'satellite' ? 'visible' : 'none');
    }

    async locateMe() {
        try {
            const position = await GeolocationService.checkWatchAndGetPositionUntilTimeout(3000);
            const lng = position.coords.longitude;
            const lat = position.coords.latitude;
            this.setUserMarker(lng, lat);
            this.map?.flyTo({ center: [lng, lat], zoom: 14 });
        } catch (err) {
            console.warn('Position indisponible', err);
        }
    }

    private setPin(lng: number, lat: number) {
        if (!this.map) {
            return;
        }
        if (this.pinMarker) {
            this.pinMarker.setLngLat([lng, lat]);
        } else {
            // Goutte terra cotta = point de départ CHOISI (distinct de la pastille
            // « Ma position », #18).
            this.pinMarker = new maplibregl.Marker({ color: '#e2725b' })
                .setLngLat([lng, lat])
                .addTo(this.map);
            this.pinMarker.getElement().setAttribute('title', 'Point de départ');
        }
    }

    // Position de l'utilisateur (#18) : pastille ronde bleue pulsante — la
    // convention « vous êtes ici » — DISTINCTE de la goutte terra cotta du point
    // de départ posé sur la carte (`pinMarker`). On dissocie ainsi par la forme
    // (dot = moi / goutte = point choisi), pas seulement par la couleur.
    private setUserMarker(lng: number, lat: number) {
        if (!this.map) {
            return;
        }
        if (this.userMarker) {
            this.userMarker.setLngLat([lng, lat]);
        } else {
            const el = document.createElement('div');
            el.className = 'user-position-marker';
            el.title = 'Ma position';
            this.userMarker = new maplibregl.Marker({ element: el })
                .setLngLat([lng, lat])
                .addTo(this.map);
        }
    }

    private refreshFavoriteMarkers() {
        if (!this.map) {
            return;
        }
        this.favoriteMarkers.forEach((m) => m.remove());
        this.favoriteMarkers = [];
        (this.favoriteLakes || []).forEach((lake) => {
            if (lake.latitude == null || lake.longitude == null) {
                return;
            }
            const el = document.createElement('div');
            el.className = 'favorite-marker';
            const marker = new maplibregl.Marker({ element: el })
                .setLngLat([lake.longitude, lake.latitude])
                .addTo(this.map!);
            el.title = lake.name;
            el.addEventListener('click', () => this.$emit('selectLake', lake.id));
            this.favoriteMarkers.push(marker);
        });
    }

    private destroyMarkers() {
        this.favoriteMarkers.forEach((m) => m.remove());
        this.favoriteMarkers = [];
        this.pinMarker?.remove();
        this.pinMarker = null;
        this.userMarker?.remove();
        this.userMarker = null;
    }

    private selectedHasCoords(): boolean {
        return !!(this.selectedLake
            && this.selectedLake.longitude != null
            && this.selectedLake.latitude != null);
    }

    private selectedCenter(): [number, number] {
        if (this.selectedHasCoords()) {
            return [this.selectedLake.longitude, this.selectedLake.latitude];
        }
        return DEFAULT_CENTER;
    }

    // Cadrage à l'ouverture (ou à la réapparition), par ordre de priorité (#18) :
    //   1. l'entité sélectionnée si elle a un centroïde ;
    //   2. sinon la position de l'utilisateur si la géoloc est disponible ;
    //   3. sinon les favoris, sinon l'emprise par défaut.
    private fitInitial() {
        if (!this.map) {
            return;
        }
        if (this.selectedHasCoords()) {
            this.map.flyTo({ center: this.selectedCenter(), zoom: 13 });
            return;
        }
        // Pas de sélection : on tente la position utilisateur, puis on retombe
        // sur les favoris / l'emprise par défaut si indisponible.
        GeolocationService.checkWatchAndGetPositionUntilTimeout(3000).then(
            (position) => {
                // Une sélection a pu arriver entre-temps (ou la carte être
                // fermée) → ne pas écraser le cadrage.
                if (!this.map || this.selectedHasCoords()) {
                    return;
                }
                const lng = position.coords.longitude;
                const lat = position.coords.latitude;
                this.setUserMarker(lng, lat);
                this.map.flyTo({ center: [lng, lat], zoom: 13 });
            },
            () => { this.fitFavoritesOrDefault(); }
        );
    }

    private fitFavoritesOrDefault() {
        if (!this.map || this.selectedHasCoords()) {
            return;
        }
        const favs = (this.favoriteLakes || []).filter((l) => l.longitude != null && l.latitude != null);
        if (favs.length === 1) {
            this.map.flyTo({ center: [favs[0].longitude, favs[0].latitude], zoom: 12 });
        } else if (favs.length > 1) {
            const lngs = favs.map((l) => l.longitude);
            const lats = favs.map((l) => l.latitude);
            const bounds: LngLatBoundsLike = [
                [Math.min(...lngs), Math.min(...lats)],
                [Math.max(...lngs), Math.max(...lats)],
            ];
            this.map.fitBounds(bounds, { padding: 40, maxZoom: 11 });
        }
        // sinon : on reste sur l'emprise par défaut (déjà posée à l'init).
    }

    // Recentre uniquement si l'entité sélectionnée a un centroïde ET qu'il est
    // hors du champ visible : préserve la vue quand on tape une entité déjà à
    // l'écran, et n'engendre aucun recadrage lors d'une désélection (null).
    private recenterOnSelectedIfOffscreen() {
        if (!this.map || !this.selectedHasCoords()) {
            return;
        }
        const center: [number, number] = [this.selectedLake.longitude, this.selectedLake.latitude];
        if (!this.map.getBounds().contains(center)) {
            this.map.flyTo({ center, zoom: 13 });
        }
    }

    closeMap() {
        this.$emit('close');
    }
}
</script>

<!-- Styles alignés sur ceux de l'ancien LakesMap pour une bascule transparente. -->
<style scoped lang="less">
.map {
    width: 100%;
    height: 100%;
    position: fixed;
    z-index: 1500;
    bottom: @footer-height;
    max-height: 70vh; // fallback si dvh non supporté
    max-height: calc(100dvh - @header-height - @secondary-header-height - @footer-height - 10px);
    left: 0;
    background-color: @black-alpha-90;
    transition: opacity 0.3s ease;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;

    @media screen and (min-width: @desktop-min-width) {
        position: absolute;
        top: 70px;
        left: 0;
        width: 100%;
        height: 500px !important;
        border: 1px solid #aaa;
        border-radius: 4px;
        box-shadow: 0px 2px 5px #0002;
    }
}

.maplibre-container {
    width: 100%;
    height: 100%;
}

.map-controls {
    position: absolute;
    top: 20px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 99999;
    display: flex;
    gap: 10px;
}

.map-btn {
    background-color: @pelorous;
    color: white;
    border: none;
    border-radius: 20px;
    padding: 8px 16px;
    font-size: 0.9rem;
    cursor: pointer;
    box-shadow: 0 0 2px #0002;

    &:hover {
        background-color: @terra-cotta;
    }
}

.is-loading {
    position: absolute;
    left: 0;
    top: 0;
    z-index: 9999999;
    background: #fffa;
    display: flex;
    flex-direction: column;
    gap: 30px;
    align-items: center;
    justify-content: center;
    font-size: 1.2rem;
    font-weight: bold;
    color: #777;
    height: 100%;
    width: 100%;
}

.offline-hint {
    position: absolute;
    bottom: 12px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 99999;
    max-width: 90%;
    display: flex;
    align-items: center;
    gap: 8px;
    background: #fff4e5;
    border: 1px solid @carrot-orange;
    color: @gunmetal;
    border-radius: 20px;
    padding: 8px 16px;
    font-size: 0.8rem;
    box-shadow: 0 1px 4px #0003;

    i {
        color: @carrot-orange;
        flex-shrink: 0;
    }
}

.close-button {
    position: absolute;
    top: 20px;
    right: 20px;
    background-color: @pelorous;
    color: white;
    z-index: 99999;
    font-size: 40px;
    box-shadow: 0 0 2px #0002;
    border-radius: 50%;
    cursor: pointer;

    &:hover {
        background-color: @terra-cotta;
    }

    @media screen and (min-width: @desktop-min-width) {
        top: 10px;
        right: 10px;
        font-size: 30px;
    }
}
</style>
<style lang="less">
/* Marqueur favori (élément DOM custom MapLibre) : pastille cœur. */
.favorite-marker {
    width: 22px;
    height: 22px;
    background: url("/img/heart.svg") no-repeat center / contain;
    cursor: pointer;
}

/* « Ma position » (#18) : pastille ronde bleue pulsante (« vous êtes ici »),
   volontairement différente de la goutte du point de départ. */
.user-position-marker {
    width: 16px;
    height: 16px;
    background: #1e9bc4;
    border: 3px solid white;
    border-radius: 50%;
    box-shadow: 0 0 0 0 rgba(30, 155, 196, 0.5);
    animation: user-position-pulse 2s infinite;
    cursor: default;
}

@keyframes user-position-pulse {
    0% { box-shadow: 0 0 0 0 rgba(30, 155, 196, 0.5); }
    70% { box-shadow: 0 0 0 12px rgba(30, 155, 196, 0); }
    100% { box-shadow: 0 0 0 0 rgba(30, 155, 196, 0); }
}

/* Infobulle de survol des entités hydro (nom + type). Rendue par MapLibre hors
   du DOM scopé du composant → styles globaux. `pointer-events: none` pour ne
   jamais capter le clic de sélection sous le curseur. */
.hydro-hover-popup {
    pointer-events: none;

    .maplibregl-popup-content {
        padding: 6px 10px;
        border-radius: 6px;
        box-shadow: 0 1px 4px #0003;
    }
    .hydro-tip {
        display: flex;
        flex-direction: column;
        line-height: 1.2;

        strong {
            color: @gunmetal;
            font-size: 0.9rem;
        }
        span {
            color: @pale-sky;
            font-size: 0.75rem;
        }
    }
}
</style>
