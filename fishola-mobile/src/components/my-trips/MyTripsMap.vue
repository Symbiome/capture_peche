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
  Carte des captures du pêcheur (#33) : migration Leaflet → MapLibre. Les captures
  sont rendues via une source GeoJSON avec clustering natif MapLibre, au-dessus des
  fonds IGN + réseau hydro (« nos layers »). Un point non regroupé est coloré selon
  si sa sortie a au moins une capture enregistrée ; un clic ouvre une infobulle avec
  un accès à la sortie.
  -->
<template>
    <div class="pane " v-if="visible">
        <span v-if="mapIsLoading" class="is-loading">
            <div class="loader" />
            Chargement de la carte...
        </span>
        <div id="info" class="info" v-if="validMarkers.length > 0" v-show="showPersonnalMapWarning">
            Cette carte n'est pas visible par les autres pêcheurs.
            Pour rappel, vous pouvez autoriser ou non la géolocalisation des prises dans les paramètres système de l'application.
            <i class="icon icon-plus close" @click="showPersonnalMapWarning = false"></i>
        </div>
        <div class="map" v-if="validMarkers.length > 0">
            <div ref="mapContainer" class="mtm-container" />
        </div>
        <div class="legend" v-if="validMarkers.length > 0">
            <div class="legend-item">
                <svg class="legend-swatch" viewBox="0 0 20 20" width="20" height="20" aria-hidden="true">
                    <circle cx="10" cy="10" r="9" fill="#1e9bc4" />
                    <path d="M13.3 9.9c0 1.5-1.6 2.8-3.5 2.8-1.4 0-2.6-.6-3.2-1.5-.1-.2-.1-.4 0-.6.6-.9 1.8-1.5 3.2-1.5 1.9 0 3.5 1.3 3.5 2.8z" fill="#fff" />
                    <path d="M13.5 8.5c.3-.2.6 0 .6.3v2c0 .3-.3.5-.6.3l-1.4-.8c-.2-.2-.2-.5 0-.7l1.4-1.1z" fill="#fff" />
                </svg>
                Sortie avec capture(s)
            </div>
            <div class="legend-item">
                <svg class="legend-swatch" viewBox="0 0 20 20" width="20" height="20" aria-hidden="true">
                    <circle cx="10" cy="10" r="9" fill="#8a8f98" />
                    <path d="M6 14 L14 6" stroke="#fff" stroke-width="1.4" stroke-linecap="round" />
                    <path d="M12.8 7.2 Q10 10.5 9.5 13.5" stroke="#fff" stroke-width="1.4" fill="none" stroke-linecap="round" />
                    <circle cx="9.5" cy="14" r="0.9" fill="#fff" />
                </svg>
                Sortie sans capture enregistrée
            </div>
        </div>
        <div class="error-markers" v-if="invalidMarkers.length > 0">
            <b>{{ invalidMarkers.length }}</b> prises sans position renseignée
        </div>
        <div v-if="!mapIsLoading && validMarkers.length == 0 && invalidMarkers.length == 0">
            Aucune sortie enregistrée
        </div>
    </div>
</template>

<script lang="ts">
import { CatchMarker } from '@/pojos/BackendPojos';
import TripsService from '@/services/TripsService';
import Helpers from '@/services/Helpers';
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';

import maplibregl, { Map as MlMap, GeoJSONSource, MapGeoJSONFeature } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { addCatchPinIcon, attachHydroHover, buildFisholaStyle, DEFAULT_CENTER, DEFAULT_ZOOM } from '@/components/common/maplibreStyle';

// Serveur de glyphes public (compteurs de clusters). Sans lui, les nombres ne
// s'affichent pas mais la carte reste fonctionnelle (dégradation silencieuse).
const GLYPHS_URL = 'https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf';

// Identifiants des pins « sortie » enregistrés dans la carte (cf. addCatchPinIcon).
// Couleur du pin = la sortie a au moins une capture enregistrée, ou aucune (#33).
// Doit rester synchronisé avec les couleurs `.legend-swatch` du template.
const CATCH_PIN_AVEC_CAPTURE = 'catch-pin-avec-capture';
const CATCH_PIN_SANS_CAPTURE = 'catch-pin-sans-capture';

@Component
export default class MyTripsMapView extends Vue {
    @Prop() visible: boolean;

    validMarkers: CatchMarker[] = [];
    invalidMarkers: CatchMarker[] = [];
    showPersonnalMapWarning = true;
    mapIsLoading = false;

    private map: MlMap | null = null;
    private detachHydroHover: (() => void) | null = null;

    mounted() {
        this.computeMapIfVisible();
    }

    beforeDestroy() {
        this.detachHydroHover?.();
        this.detachHydroHover = null;
        this.map?.remove();
        this.map = null;
    }

    @Watch('visible')
    computeMapIfVisible() {
        if (this.visible && this.validMarkers.length === 0) {
            this.mapIsLoading = true;
            TripsService.catchMarkers().then(
                (markers) => {
                    this.validMarkers = markers.filter((m: CatchMarker) => m.hasValidCoordinates);
                    this.invalidMarkers = markers.filter((m: CatchMarker) => !m.hasValidCoordinates);
                    this.$nextTick(() => this.initOrUpdateMap());
                },
                (error: Error) => {
                    console.error(error);
                    this.mapIsLoading = false;
                },
            );
        } else if (this.visible) {
            this.$nextTick(() => {
                this.map?.resize();
                this.fitToMarkers();
            });
        }
    }

    private buildGeoJson(): any {
        return {
            type: 'FeatureCollection',
            features: this.validMarkers.map((c) => ({
                type: 'Feature',
                geometry: { type: 'Point', coordinates: [c.longitude, c.latitude] },
                properties: {
                    id: c.id || null,
                    tripId: c.tripId,
                    tripName: c.tripName,
                    hasCatch: !!c.id,
                    specieName: c.specieName || null,
                    maillage: c.maillage || null,
                    lakeName: c.waterEntityName,
                    dateLabel: this.formattedDate(c.date),
                },
            })),
        };
    }

    private initOrUpdateMap() {
        if (this.validMarkers.length === 0) {
            this.mapIsLoading = false;
            return;
        }
        if (this.map) {
            const source = this.map.getSource('catches') as GeoJSONSource | undefined;
            source?.setData(this.buildGeoJson());
            this.fitToMarkers();
            this.mapIsLoading = false;
            return;
        }
        const container = this.$refs.mapContainer as HTMLElement;
        if (!container) {
            this.mapIsLoading = false;
            return;
        }
        const style = buildFisholaStyle('plan');
        style.glyphs = GLYPHS_URL;
        this.map = new maplibregl.Map({
            container,
            style,
            center: DEFAULT_CENTER,
            zoom: DEFAULT_ZOOM,
            attributionControl: { compact: true },
        });
        this.map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-left');
        // Informations du réseau hydro au survol, comme sur les autres cartes.
        this.detachHydroHover = attachHydroHover(this.map);
        // Les pins sont fournis à la demande : les couches restent ainsi ajoutées
        // de façon SYNCHRONE dans « load » (une création différée de la source
        // empêchait le regroupement en clusters de se mettre en place).
        this.map.on('styleimagemissing', (e: any) => {
            if (!this.map) {
                return;
            }
            if (e.id === CATCH_PIN_AVEC_CAPTURE) {
                addCatchPinIcon(this.map, CATCH_PIN_AVEC_CAPTURE, '#1e9bc4', 'fish');
            } else if (e.id === CATCH_PIN_SANS_CAPTURE) {
                addCatchPinIcon(this.map, CATCH_PIN_SANS_CAPTURE, '#8a8f98', 'rod');
            }
        });
        this.map.on('load', () => {
            this.addCatchLayers();
            this.fitToMarkers();
            this.mapIsLoading = false;
        });
    }

    private addCatchLayers() {
        if (!this.map) {
            return;
        }
        this.map.addSource('catches', {
            type: 'geojson',
            data: this.buildGeoJson(),
            cluster: true,
            clusterRadius: 50,
            clusterMaxZoom: 14,
        });
        this.map.addLayer({
            id: 'catch-clusters', type: 'circle', source: 'catches', filter: ['has', 'point_count'],
            paint: {
                'circle-color': '#1e9bc4',
                'circle-opacity': 0.85,
                'circle-radius': ['step', ['get', 'point_count'], 16, 10, 22, 50, 30],
            },
        });
        this.map.addLayer({
            id: 'catch-cluster-count', type: 'symbol', source: 'catches', filter: ['has', 'point_count'],
            layout: { 'text-field': ['get', 'point_count_abbreviated'], 'text-size': 13, 'text-font': ['Open Sans Regular'] },
            paint: { 'text-color': '#ffffff' },
        });
        // Sortie isolée : pin « poisson » (capture) ou « canne à pêche » (sortie
        // sans capture) plutôt qu'une pastille, plus explicite et ancré par sa
        // pointe sur la position exacte (cf. légende sous la carte).
        this.map.addLayer({
            id: 'catch-unclustered', type: 'symbol', source: 'catches', filter: ['!', ['has', 'point_count']],
            layout: {
                'icon-image': ['case', ['get', 'hasCatch'], CATCH_PIN_AVEC_CAPTURE, CATCH_PIN_SANS_CAPTURE],
                'icon-size': 1,
                'icon-anchor': 'bottom',
                'icon-allow-overlap': true,
            },
        });

        this.map.on('click', 'catch-clusters', (e) => this.onClusterClick(e.point));
        this.map.on('click', 'catch-unclustered', (e) => this.onPointClick(e.features && e.features[0]));
        ['catch-clusters', 'catch-unclustered'].forEach((layer) => {
            this.map!.on('mouseenter', layer, () => { this.map!.getCanvas().style.cursor = 'pointer'; });
            this.map!.on('mouseleave', layer, () => { this.map!.getCanvas().style.cursor = ''; });
        });
    }

    private onClusterClick(point: { x: number; y: number }) {
        if (!this.map) {
            return;
        }
        const feature = this.map.queryRenderedFeatures(point, { layers: ['catch-clusters'] })[0];
        if (!feature) {
            return;
        }
        const clusterId = feature.properties!.cluster_id;
        const source = this.map.getSource('catches') as GeoJSONSource;
        source.getClusterExpansionZoom(clusterId).then((zoom) => {
            const coords = (feature.geometry as any).coordinates as [number, number];
            this.map?.easeTo({ center: coords, zoom });
        });
    }

    private onPointClick(feature?: MapGeoJSONFeature) {
        if (!this.map || !feature) {
            return;
        }
        const p = feature.properties || {};
        const coords = (feature.geometry as any).coordinates as [number, number];
        const hasCatch = !!p.hasCatch;
        const maillageLabel = p.maillage === 'MAILLEE' ? ' (maillé)'
            : (p.maillage && p.maillage !== 'NON_DEFINI' ? ' (non maillé)' : '');
        const speciesLine = hasCatch
            ? `<p><i class="fish icon-fish"></i> ${this.escapeHtml(p.specieName)}${maillageLabel}</p>`
            : `<p><i class="fish icon-fish"></i> Aucune capture enregistrée</p>`;
        const html = `<div class="catch-marker">`
            + `<p class="title">${this.escapeHtml(p.tripName)}</p>`
            + speciesLine
            + `<p class="infos"><span class="trip-date">${this.escapeHtml(p.dateLabel)}</span> - ${this.escapeHtml(p.lakeName)}</p>`
            + `<button type="button" class="button">Voir la sortie</button>`
            + `</div>`;
        const popup = new maplibregl.Popup({ offset: 16 }).setLngLat(coords).setHTML(html).addTo(this.map);
        const btn = popup.getElement().querySelector('button');
        btn?.addEventListener('click', () => {
            popup.remove();
            if (hasCatch) {
                this.showCatch(p.tripId as string, p.id as string);
            } else {
                this.showTrip(p.tripId as string);
            }
        });
    }

    private fitToMarkers() {
        if (!this.map || this.validMarkers.length === 0) {
            return;
        }
        if (this.validMarkers.length === 1) {
            const m = this.validMarkers[0];
            this.map.flyTo({ center: [m.longitude, m.latitude], zoom: 13 });
            return;
        }
        const bounds = new maplibregl.LngLatBounds();
        this.validMarkers.forEach((m) => bounds.extend([m.longitude, m.latitude] as [number, number]));
        this.map.fitBounds(bounds, { padding: 50, maxZoom: 13 });
    }

    private escapeHtml(value: string): string {
        return (value || '').replace(/[&<>"']/g, (c) => (
            { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
        ));
    }

    formattedDate(tripDate: Date): string {
        const dayOptions: Intl.DateTimeFormatOptions = { month: 'numeric', day: 'numeric', year: 'numeric' };
        // @ts-ignore
        const date = Helpers.parseLocalDate(tripDate);
        return date.toLocaleDateString('fr-FR', dayOptions);
    }

    showCatch(tripId: string, catchId: string) {
        this.$router.push({ name: 'catch', params: { tripId, catchId } });
    }

    showTrip(tripId: string) {
        this.$router.push({ name: 'trip', params: { id: tripId } });
    }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.info {
    position: absolute;
    right: 50px;
    width: 70vw;
    z-index: 997;
    padding: 20px;
    margin-top: 10px;
    margin-bottom: 10px;
    border: 2px solid @very-light-grey;
    background-color: @white-smoke;
    border-radius: 10px;

    .close {
        transform: rotate(45deg);
        cursor: pointer;
        position: absolute;
        top: 5px;
        right: 5px;
    }
}

.map {
    height: 80vh;
    z-index: 995;
}

.mtm-container {
    width: 100%;
    height: 100%;
}

.legend {
    display: flex;
    flex-wrap: wrap;
    gap: @margin-medium;
    margin-top: @margin-small;
}

.legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
}

.legend-swatch {
    flex-shrink: 0;
}

.error-markers {
    width: 100%;
    color: @cardinal;
    text-align: right;
    margin-top: @margin-small;
}

.is-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 30px;
    margin: 70px;
}
</style>

<!-- Infobulle de capture : rendue par MapLibre hors du DOM scopé → styles globaux. -->
<style lang="less">
.catch-marker {
    .title {
        font-size: 18px;
        color: @pelorous;
    }

    .trip-date {
        font-weight: bolder;
    }

    .fish {
        color: @pelorous;
    }

    .infos {
        padding-bottom: 10px;
    }

    .button {
        height: 44px;
        border-radius: 44px;
        font-weight: bold;
        border: 0;
        padding-left: @margin-medium;
        padding-right: @margin-medium;
        background-color: @terra-cotta;
        color: @white;
        cursor: pointer;

        &:hover {
            background-color: @white;
            color: @terra-cotta;
            border: 2px solid @terra-cotta;
        }
    }
}
</style>
