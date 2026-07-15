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
</div>
</template>

<script lang="ts">
import { WaterEntity as Lake } from '@/pojos/BackendPojos';
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import Constants from '@/services/Constants';
import GeolocationService from '@/services/GeolocationService';

import maplibregl, { Map as MlMap, Marker, LngLatBoundsLike, StyleSpecification } from 'maplibre-gl';
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
    baseLayer: BaseLayer = 'plan';
    mapIsLoading = true;

    mounted() {
        if (this.isVisible) {
            this.initMap();
        }
    }

    beforeDestroy() {
        this.destroyMarkers();
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

    private initMap() {
        const container = this.$refs.mapContainer as HTMLElement;
        if (!container || this.map) {
            return;
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

        // Tap sur le réseau : on privilégie une entité hydro sous le doigt, sinon
        // on pose un pin libre (consommé par la validation de trip, #9).
        map.on('click', (e) => {
            const features = map.queryRenderedFeatures(e.point, {
                layers: ['hydro-surface', 'hydro-river-persistent', 'hydro-river-intermittent'],
            });
            if (features.length > 0) {
                const props = features[0].properties || {};
                const id = props.water_entity_id as string;
                if (id) {
                    this.$emit('selectLake', id);
                    return;
                }
            }
            this.setPin(e.lngLat.lng, e.lngLat.lat);
            this.$emit('map-click', { lng: e.lngLat.lng, lat: e.lngLat.lat });
        });

        // Curseur « pointer » au survol d'une entité cliquable.
        const hoverLayers = ['hydro-surface', 'hydro-river-persistent', 'hydro-river-intermittent'];
        hoverLayers.forEach((layer) => {
            map.on('mouseenter', layer, () => { map.getCanvas().style.cursor = 'pointer'; });
            map.on('mouseleave', layer, () => { map.getCanvas().style.cursor = ''; });
        });
    }

    // Construit le style MapLibre de A à Z (aucune URL de style externe : plus
    // simple à figer et compatible avec un futur mode hors-ligne #10).
    private buildStyle(): StyleSpecification {
        return {
            version: 8,
            sources: {
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
            },
            layers: [
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
            ],
        };
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
            this.pinMarker = new maplibregl.Marker({ color: '#e2725b' })
                .setLngLat([lng, lat])
                .addTo(this.map);
        }
    }

    private setUserMarker(lng: number, lat: number) {
        if (!this.map) {
            return;
        }
        if (this.userMarker) {
            this.userMarker.setLngLat([lng, lat]);
        } else {
            this.userMarker = new maplibregl.Marker({ color: '#1e9bc4' })
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

    // Cadrage à l'ouverture (ou à la réapparition) : sur l'entité sélectionnée si
    // elle a un centroïde, sinon sur les favoris, sinon emprise par défaut.
    private fitInitial() {
        if (!this.map) {
            return;
        }
        if (this.selectedHasCoords()) {
            this.map.flyTo({ center: this.selectedCenter(), zoom: 13 });
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
</style>
