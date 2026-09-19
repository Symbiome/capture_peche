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
  Carte du point de début (vert), du point de fin (rouge) et, si fournis, des
  points de capture (bleu) d'une sortie (#86, captures ajoutées en #173 pour
  l'export PDF). Même fond de carte que la saisie d'une prise (fonds IGN +
  réseau hydro, bascule Plan/Satellite). En mode `editable`, l'utilisateur
  place lui-même le point de fin en touchant la carte ou en déplaçant le
  marqueur ; la nouvelle position est émise via `end-position-picked`
  ({ lat, lng }). Le point de début n'est jamais déplaçable ici (il est fixé
  à la création de la sortie).
  -->
<template>
  <div v-if="hasAnyPosition || editable" class="trip-positions-map">
    <div ref="mapContainer" class="trip-positions-map-container" />
    <button type="button" class="trip-positions-map-base-btn" @click="toggleBase">
      {{ baseLayer === 'plan' ? 'Satellite' : 'Plan' }}
    </button>
    <ul v-if="showLegend" class="trip-positions-map-legend">
      <li v-if="hasBeginPosition">
        <span class="legend-dot legend-dot-begin" /> Début
      </li>
      <li v-if="hasEndPosition || editable">
        <span class="legend-dot legend-dot-end" /> Fin
      </li>
      <li v-if="catches && catches.length">
        <span class="legend-dot legend-dot-catch" /> Capture{{ catches.length > 1 ? 's' : '' }}
      </li>
    </ul>
    <span v-if="editable" class="trip-positions-map-hint">
      <i class="icon-map" />
      Touchez la carte pour indiquer où vous avez terminé votre session
    </span>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import maplibregl, { Map as MlMap, Marker, GeoJSONSource, LngLatBoundsLike } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import {
  addCatchPinIcon,
  attachHydroHover,
  BaseLayer,
  createFisholaMap,
  DEFAULT_CENTER,
  DEFAULT_ZOOM,
  setBaseLayer,
} from '@/components/common/maplibreStyle';

const BEGIN_COLOR = '#44BD32';
const END_COLOR = '#D62137';
const CATCH_COLOR = '#1e9bc4';
const CATCH_PIN_ID = 'trip-positions-catch-pin';

export interface TripPositionsCatchPoint {
  lat: number;
  lng: number;
}

@Component
export default class TripPositionsMap extends Vue {
  @Prop() beginLatitude?: number;
  @Prop() beginLongitude?: number;
  @Prop() endLatitude?: number;
  @Prop() endLongitude?: number;
  /** Si vrai, le point de fin est plaçable/déplaçable par l'utilisateur (#86). */
  @Prop({ default: false }) editable: boolean;
  /** Centre de repli quand aucune position n'existe encore (ex. centroïde du plan d'eau). */
  @Prop() centerLat?: number;
  @Prop() centerLng?: number;
  /** Points de capture (#173, export PDF) : un pin par prise géolocalisée. */
  @Prop({ default: () => [] }) catches: TripPositionsCatchPoint[];
  /** #173 : préserve le tampon WebGL pour permettre `captureImage()` (export PDF). */
  @Prop({ default: false }) captureMode: boolean;

  private map: MlMap | null = null;
  private beginMarker: Marker | null = null;
  private endMarker: Marker | null = null;
  private detachHydroHover: (() => void) | null = null;
  baseLayer: BaseLayer = 'plan';

  get hasBeginPosition(): boolean {
    return this.beginLatitude != null && this.beginLongitude != null;
  }

  get hasEndPosition(): boolean {
    return this.endLatitude != null && this.endLongitude != null;
  }

  get hasAnyPosition(): boolean {
    return this.hasBeginPosition || this.hasEndPosition;
  }

  private get hasCenter(): boolean {
    return this.centerLat != null && this.centerLng != null;
  }

  get showLegend(): boolean {
    return this.hasBeginPosition || this.hasEndPosition || this.editable;
  }

  mounted() {
    if (this.hasAnyPosition || this.editable) {
      this.$nextTick(() => this.initMap());
    }
  }

  beforeDestroy() {
    this.beginMarker?.remove();
    this.endMarker?.remove();
    this.detachHydroHover?.();
    this.detachHydroHover = null;
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
    if (!this.hasAnyPosition && !this.editable) {
      return;
    }
    if (!this.map) {
      this.$nextTick(() => this.initMap());
      return;
    }
    this.refreshMarkers();
  }

  @Watch('catches')
  onCatchesChanged() {
    if (!this.map) {
      return;
    }
    this.refreshCatchLayer();
    this.fitToMarkers();
  }

  private initMap() {
    const container = this.$refs.mapContainer as HTMLElement;
    if (!container || this.map) {
      return;
    }
    this.map = createFisholaMap(container, {
      center: this.initialCenter(),
      zoom: this.initialZoom(),
      baseLayer: this.baseLayer,
      preserveDrawingBuffer: this.captureMode,
    });
    this.detachHydroHover = attachHydroHover(this.map);
    // Le pin de capture (goutte bleue, même style que MyTripsMap.vue) n'est
    // enregistré qu'à la demande de la couche symbole (#173).
    this.map.on('styleimagemissing', (e: any) => {
      if (this.map && e.id === CATCH_PIN_ID) {
        addCatchPinIcon(this.map, CATCH_PIN_ID, CATCH_COLOR, 'fish');
      }
    });
    this.map.on('load', () => {
      this.map?.resize();
      this.refreshMarkers();
      this.addCatchLayer();
      this.fitToMarkers();
    });
    if (this.editable) {
      this.map.on('click', (e) => {
        this.setEndMarker(e.lngLat.lng, e.lngLat.lat);
        this.emitEndPosition(e.lngLat.lng, e.lngLat.lat);
      });
    }
  }

  private catchesGeoJson(): any {
    return {
      type: 'FeatureCollection',
      features: (this.catches || []).map((c) => ({
        type: 'Feature',
        geometry: { type: 'Point', coordinates: [c.lng, c.lat] },
        properties: {},
      })),
    };
  }

  private addCatchLayer() {
    if (!this.map || this.map.getSource('trip-catches')) {
      return;
    }
    this.map.addSource('trip-catches', { type: 'geojson', data: this.catchesGeoJson() });
    this.map.addLayer({
      id: 'trip-catch-points',
      type: 'symbol',
      source: 'trip-catches',
      layout: {
        'icon-image': CATCH_PIN_ID,
        'icon-size': 1,
        'icon-anchor': 'bottom',
        'icon-allow-overlap': true,
      },
    });
  }

  private refreshCatchLayer() {
    if (!this.map) {
      return;
    }
    const source = this.map.getSource('trip-catches') as GeoJSONSource | undefined;
    if (source) {
      source.setData(this.catchesGeoJson());
    } else {
      this.addCatchLayer();
    }
  }

  private initialCenter(): [number, number] {
    if (this.hasBeginPosition) {
      return [this.beginLongitude!, this.beginLatitude!];
    }
    if (this.hasEndPosition) {
      return [this.endLongitude!, this.endLatitude!];
    }
    if (this.hasCenter) {
      return [this.centerLng!, this.centerLat!];
    }
    return DEFAULT_CENTER;
  }

  private initialZoom(): number {
    if (this.hasBeginPosition && this.hasEndPosition) {
      return 12;
    }
    if (this.hasAnyPosition || this.hasCenter) {
      return 13;
    }
    return DEFAULT_ZOOM;
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
      this.setEndMarker(this.endLongitude!, this.endLatitude!);
    }
  }

  private setEndMarker(lng: number, lat: number) {
    if (!this.map) {
      return;
    }
    if (this.endMarker) {
      this.endMarker.setLngLat([lng, lat]);
      return;
    }
    this.endMarker = new maplibregl.Marker({ color: END_COLOR, draggable: this.editable })
      .setLngLat([lng, lat])
      .addTo(this.map);
    this.endMarker.getElement().setAttribute('title', 'Point de fin');
    if (this.editable) {
      this.endMarker.on('dragend', () => {
        const p = this.endMarker!.getLngLat();
        this.emitEndPosition(p.lng, p.lat);
      });
    }
  }

  private emitEndPosition(lng: number, lat: number) {
    this.$emit('end-position-picked', { lat, lng });
  }

  // Cadre l'ensemble début/fin/captures (#173) : au moins 2 points requis,
  // sinon le cadrage initial (initialCenter/initialZoom) suffit déjà.
  private fitToMarkers() {
    if (!this.map) {
      return;
    }
    const points: [number, number][] = [];
    if (this.hasBeginPosition) {
      points.push([this.beginLongitude!, this.beginLatitude!]);
    }
    if (this.hasEndPosition) {
      points.push([this.endLongitude!, this.endLatitude!]);
    }
    (this.catches || []).forEach((c) => points.push([c.lng, c.lat]));
    if (points.length < 2) {
      return;
    }
    const lngs = points.map((p) => p[0]);
    const lats = points.map((p) => p[1]);
    const bounds: LngLatBoundsLike = [
      [Math.min(...lngs), Math.min(...lats)],
      [Math.max(...lngs), Math.max(...lats)],
    ];
    this.map.fitBounds(bounds, { padding: 40, maxZoom: 15 });
  }

  toggleBase() {
    this.baseLayer = this.baseLayer === 'plan' ? 'satellite' : 'plan';
    if (this.map) {
      setBaseLayer(this.map, this.baseLayer);
    }
  }

  // #173 (export PDF) : nécessite `captureMode` (sinon le tampon WebGL est
  // effacé et l'image est vide). On attend l'évènement `idle` avant de lire
  // le canvas, sinon la capture peut tomber pendant l'animation de
  // `fitBounds` et rendre une carte pas encore centrée/chargée.
  captureImage(): Promise<string | null> {
    const map = this.map;
    if (!map) {
      return Promise.resolve(null);
    }
    return new Promise((resolve) => {
      const capture = () => resolve(map.getCanvas().toDataURL('image/png'));
      if (map.loaded() && !map.isMoving() && !map.isZooming()) {
        requestAnimationFrame(capture);
      } else {
        map.once('idle', capture);
      }
    });
  }
}
</script>

<style scoped lang="less">
.trip-positions-map {
  position: relative;
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

.trip-positions-map-base-btn {
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

.trip-positions-map-legend {
  position: absolute;
  top: 10px;
  left: 50px;
  z-index: 500;
  margin: 0;
  padding: 4px 8px;
  list-style: none;
  background-color: rgba(255, 255, 255, 0.92);
  border-radius: 4px;
  box-shadow: 0 0 2px #0002;
  font-size: 0.78rem;
  color: @gunmetal;

  li {
    display: flex;
    align-items: center;
  }

  .legend-dot {
    display: inline-block;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    margin-right: 6px;
    border: 1px solid rgba(0, 0, 0, 0.25);
  }

  .legend-dot-begin {
    background-color: #44bd32;
  }

  .legend-dot-end {
    background-color: #d62137;
  }

  .legend-dot-catch {
    background-color: #1e9bc4;
  }
}

.trip-positions-map-hint {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 10px;
  z-index: 500;
  background-color: rgba(255, 255, 255, 0.92);
  border-radius: 4px;
  padding: 6px 10px;
  font-size: 0.8rem;
  font-style: italic;
  color: @gunmetal;
  box-shadow: 0 0 2px #0002;

  i {
    margin-right: @margin-x-small;
  }
}
</style>
