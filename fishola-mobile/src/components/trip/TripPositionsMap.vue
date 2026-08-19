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
  Petite carte de lecture seule (#86) affichant le point de début (vert) et/ou
  le point de fin (rouge) d'une sortie. Contrairement à MapLibreMap, pas de
  couche hydro ni d'interaction de sélection : juste un fond IGN et deux
  marqueurs de position.
  -->
<template>
  <div v-if="hasAnyPosition" class="trip-positions-map">
    <div ref="mapContainer" class="trip-positions-map-container" />
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import maplibregl, { Map as MlMap, Marker, LngLatBoundsLike } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';

const IGN_ATTRIBUTION = '© <a href="https://www.ign.fr/" target="_blank" rel="noopener">IGN</a>';
const IGN_PLAN_URL = 'https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0'
    + '&STYLE=normal&TILEMATRIXSET=PM&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}'
    + '&LAYER=GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2&FORMAT=image/png';

const BEGIN_COLOR = '#44BD32';
const END_COLOR = '#D62137';

@Component
export default class TripPositionsMap extends Vue {
  @Prop() beginLatitude?: number;
  @Prop() beginLongitude?: number;
  @Prop() endLatitude?: number;
  @Prop() endLongitude?: number;

  private map: MlMap | null = null;
  private beginMarker: Marker | null = null;
  private endMarker: Marker | null = null;

  get hasBeginPosition(): boolean {
    return this.beginLatitude != null && this.beginLongitude != null;
  }

  get hasEndPosition(): boolean {
    return this.endLatitude != null && this.endLongitude != null;
  }

  get hasAnyPosition(): boolean {
    return this.hasBeginPosition || this.hasEndPosition;
  }

  mounted() {
    if (this.hasAnyPosition) {
      this.$nextTick(() => this.initMap());
    }
  }

  beforeDestroy() {
    this.beginMarker?.remove();
    this.endMarker?.remove();
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  @Watch('beginLatitude')
  @Watch('beginLongitude')
  @Watch('endLatitude')
  @Watch('endLongitude')
  onPositionsChanged() {
    if (!this.hasAnyPosition) {
      return;
    }
    if (!this.map) {
      this.$nextTick(() => this.initMap());
      return;
    }
    this.refreshMarkers();
  }

  private initMap() {
    const container = this.$refs.mapContainer as HTMLElement;
    if (!container || this.map) {
      return;
    }

    const map = new maplibregl.Map({
      container,
      style: {
        version: 8,
        sources: {
          'ign-plan': {
            type: 'raster',
            tiles: [IGN_PLAN_URL],
            tileSize: 256,
            maxzoom: 19,
            attribution: IGN_ATTRIBUTION,
          },
        },
        layers: [
          { id: 'ign-plan', type: 'raster', source: 'ign-plan' },
        ],
      },
      center: this.initialCenter(),
      zoom: this.hasBeginPosition && this.hasEndPosition ? 12 : 13,
      attributionControl: { compact: true },
    });
    this.map = map;
    map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-left');
    map.on('load', () => {
      this.refreshMarkers();
      this.fitToMarkers();
    });
  }

  private initialCenter(): [number, number] {
    if (this.hasBeginPosition) {
      return [this.beginLongitude!, this.beginLatitude!];
    }
    return [this.endLongitude!, this.endLatitude!];
  }

  private refreshMarkers() {
    if (!this.map) {
      return;
    }
    if (this.hasBeginPosition) {
      if (this.beginMarker) {
        this.beginMarker.setLngLat([this.beginLongitude!, this.beginLatitude!]);
      } else {
        this.beginMarker = new maplibregl.Marker({ color: BEGIN_COLOR })
          .setLngLat([this.beginLongitude!, this.beginLatitude!])
          .addTo(this.map);
        this.beginMarker.getElement().setAttribute('title', 'Point de départ');
      }
    }
    if (this.hasEndPosition) {
      if (this.endMarker) {
        this.endMarker.setLngLat([this.endLongitude!, this.endLatitude!]);
      } else {
        this.endMarker = new maplibregl.Marker({ color: END_COLOR })
          .setLngLat([this.endLongitude!, this.endLatitude!])
          .addTo(this.map);
        this.endMarker.getElement().setAttribute('title', 'Point de fin');
      }
    }
  }

  private fitToMarkers() {
    if (!this.map) {
      return;
    }
    if (this.hasBeginPosition && this.hasEndPosition) {
      const bounds: LngLatBoundsLike = [
        [Math.min(this.beginLongitude!, this.endLongitude!), Math.min(this.beginLatitude!, this.endLatitude!)],
        [Math.max(this.beginLongitude!, this.endLongitude!), Math.max(this.beginLatitude!, this.endLatitude!)],
      ];
      this.map.fitBounds(bounds, { padding: 40, maxZoom: 15 });
    }
  }
}
</script>

<style scoped lang="less">
.trip-positions-map {
  width: 100%;
  height: 220px;
  margin: @vertical-margin-small 0;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid @pale-sky;
}

.trip-positions-map-container {
  width: 100%;
  height: 100%;
}
</style>
