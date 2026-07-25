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
  Carte MapLibre d'une position unique (#33). Remplace les cartes Leaflet à un
  seul marqueur : marqueur déplaçable si `editable` (avec option clic-pour-placer),
  au-dessus des fonds IGN + réseau hydro (« nos layers »). Émet la nouvelle
  position via `dragend` / `position` ({ lat, lng }).
  -->
<template>
    <div class="maplibre-position">
        <div ref="mapContainer" class="mlpos-container" />
        <button type="button" class="mlpos-base-btn" @click="toggleBase">
            {{ baseLayer === 'plan' ? 'Satellite' : 'Plan' }}
        </button>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import maplibregl, { Map as MlMap, Marker } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { BaseLayer, createFisholaMap, setBaseLayer } from '@/components/common/maplibreStyle';

@Component
export default class MapLibrePositionMap extends Vue {
    @Prop({ default: null }) lat: number | null;
    @Prop({ default: null }) lng: number | null;
    @Prop({ default: false }) editable: boolean;
    @Prop({ default: 13 }) zoom: number;
    /** Si vrai (et editable), un clic sur la carte déplace le marqueur. */
    @Prop({ default: false }) clickToPlace: boolean;

    private map: MlMap | null = null;
    private marker: Marker | null = null;
    baseLayer: BaseLayer = 'plan';

    mounted() {
        // Le conteneur doit être dimensionné avant l'init MapLibre.
        this.$nextTick(() => this.init());
    }

    beforeDestroy() {
        this.marker?.remove();
        this.marker = null;
        this.map?.remove();
        this.map = null;
    }

    private hasCoords(): boolean {
        return this.lat != null && this.lng != null;
    }

    private init() {
        const container = this.$refs.mapContainer as HTMLElement;
        if (!container || this.map) {
            return;
        }
        const center: [number, number] | undefined = this.hasCoords()
            ? [this.lng as number, this.lat as number]
            : undefined;
        this.map = createFisholaMap(container, { center, zoom: this.zoom, baseLayer: this.baseLayer });
        this.map.on('load', () => this.map?.resize());
        if (this.hasCoords()) {
            this.setMarker(this.lng as number, this.lat as number);
        }
        if (this.clickToPlace && this.editable) {
            this.map.on('click', (e) => {
                this.setMarker(e.lngLat.lng, e.lngLat.lat);
                this.emitPosition(e.lngLat.lng, e.lngLat.lat);
            });
        }
    }

    private setMarker(lng: number, lat: number) {
        if (!this.map) {
            return;
        }
        if (this.marker) {
            this.marker.setLngLat([lng, lat]);
            return;
        }
        this.marker = new maplibregl.Marker({ color: '#e2725b', draggable: this.editable })
            .setLngLat([lng, lat])
            .addTo(this.map);
        if (this.editable) {
            this.marker.on('dragend', () => {
                const p = this.marker!.getLngLat();
                this.emitPosition(p.lng, p.lat);
            });
        }
    }

    private emitPosition(lng: number, lat: number) {
        this.$emit('dragend', { lat, lng });
        this.$emit('position', { lat, lng });
    }

    @Watch('lat')
    @Watch('lng')
    onCoordsChange() {
        if (!this.map || !this.hasCoords()) {
            return;
        }
        this.setMarker(this.lng as number, this.lat as number);
        this.map.flyTo({ center: [this.lng as number, this.lat as number], zoom: this.zoom });
    }

    toggleBase() {
        this.baseLayer = this.baseLayer === 'plan' ? 'satellite' : 'plan';
        if (this.map) {
            setBaseLayer(this.map, this.baseLayer);
        }
    }
}
</script>

<style scoped lang="less">
.maplibre-position {
    position: relative;
    width: 100%;
    height: 100%;
}

.mlpos-container {
    width: 100%;
    height: 100%;
}

.mlpos-base-btn {
    position: absolute;
    top: 10px;
    right: 10px;
    z-index: 500;
    background-color: @pelorous;
    color: white;
    border: none;
    border-radius: 20px;
    padding: 6px 14px;
    font-size: 0.85rem;
    cursor: pointer;
    box-shadow: 0 0 2px #0002;

    &:hover {
        background-color: @terra-cotta;
    }
}
</style>
