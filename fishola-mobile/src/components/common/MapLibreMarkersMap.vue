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
  Carte MapLibre affichant une liste de marqueurs avec infobulle (#33). Remplace
  les cartes Leaflet à marqueurs simples, au-dessus des fonds IGN + réseau hydro.
  -->
<template>
    <div class="maplibre-markers">
        <div ref="mapContainer" class="mlm-container" />
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import maplibregl, { Map as MlMap, Marker, Popup, LngLatBounds } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { attachHydroHover, createFisholaMap } from '@/components/common/maplibreStyle';

export interface MapMarker {
    lat: number;
    lng: number;
    popupHtml?: string;
    title?: string;
}

@Component
export default class MapLibreMarkersMap extends Vue {
    @Prop({ default: () => [] }) markers: MapMarker[];
    @Prop({ default: 9 }) zoom: number;
    /** Centre [lng, lat] par défaut si aucun marqueur. */
    @Prop({ default: null }) center: [number, number] | null;
    @Prop({ default: true }) fit: boolean;

    private map: MlMap | null = null;
    private markerObjects: Marker[] = [];
    private detachHydroHover: (() => void) | null = null;

    mounted() {
        this.$nextTick(() => this.init());
    }

    beforeDestroy() {
        this.clearMarkers();
        this.detachHydroHover?.();
        this.detachHydroHover = null;
        this.map?.remove();
        this.map = null;
    }

    private init() {
        const container = this.$refs.mapContainer as HTMLElement;
        if (!container || this.map) {
            return;
        }
        this.map = createFisholaMap(container, {
            center: this.center || undefined,
            zoom: this.zoom,
        });
        this.map.on('load', () => {
            this.map?.resize();
            this.refreshMarkers();
        });
        // Informations du réseau hydro au survol, comme à la saisie.
        this.detachHydroHover = attachHydroHover(this.map);
    }

    private clearMarkers() {
        this.markerObjects.forEach((m) => m.remove());
        this.markerObjects = [];
    }

    private refreshMarkers() {
        if (!this.map) {
            return;
        }
        this.clearMarkers();
        const valid = (this.markers || []).filter((m) => m.lat != null && m.lng != null);
        valid.forEach((m) => {
            const marker = new maplibregl.Marker({ color: '#1e9bc4' }).setLngLat([m.lng, m.lat]);
            if (m.title) {
                marker.getElement().setAttribute('title', m.title);
            }
            if (m.popupHtml) {
                marker.setPopup(new maplibregl.Popup({ offset: 24 }).setHTML(m.popupHtml));
            }
            marker.addTo(this.map!);
            this.markerObjects.push(marker);
        });
        if (this.fit && valid.length > 0) {
            this.fitToMarkers(valid);
        }
    }

    private fitToMarkers(valid: MapMarker[]) {
        if (!this.map) {
            return;
        }
        if (valid.length === 1) {
            this.map.flyTo({ center: [valid[0].lng, valid[0].lat], zoom: this.zoom });
            return;
        }
        const bounds = new maplibregl.LngLatBounds();
        valid.forEach((m) => bounds.extend([m.lng, m.lat] as [number, number]));
        this.map.fitBounds(bounds, { padding: 40, maxZoom: 11 });
    }

    @Watch('markers')
    onMarkersChange() {
        if (this.map && this.map.isStyleLoaded()) {
            this.refreshMarkers();
        }
    }
}
</script>

<style scoped lang="less">
.maplibre-markers {
    width: 100%;
    height: 100%;
}

.mlm-container {
    width: 100%;
    height: 100%;
}
</style>
