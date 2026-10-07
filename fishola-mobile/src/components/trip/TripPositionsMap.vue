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
  <div v-if="hasAnyPosition || editable" class="trip-positions-map" :class="{ 'capture-mode': captureMode }">
    <div ref="mapContainer" class="trip-positions-map-container" />
    <BaseLayerToggle :baseLayer="baseLayer" @toggle="toggleBase" />
    <ul v-if="showLegend" class="trip-positions-map-legend">
      <li v-if="hasBeginPosition">
        <span class="legend-dot legend-dot-begin" /> Début
      </li>
      <li v-if="hasEndPosition || editable">
        <span class="legend-dot legend-dot-end" /> Fin
      </li>
      <li v-if="hasCatches">
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
import BaseLayerToggle from '@/components/common/BaseLayerToggle.vue';
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
// Encombrement à l'écran du pin de capture (68×92 px en pixelRatio 2, ancré en
// bas) et des marqueurs début/fin : réservé dans les marges de cadrage pour
// qu'aucun pin ne soit coupé ni collé au bord (#193).
const PIN_HEIGHT_PX = 46;
const PIN_HALF_WIDTH_PX = 17;
// Marge de cadrage relative à la plus petite dimension de la carte (#193).
const FRAMING_MARGIN_RATIO = 0.12;
const MAX_FIT_ZOOM = 15;
// Sécurité : une capture ne doit pas bloquer l'export si `idle` n'arrive pas.
const CAPTURE_TIMEOUT_MS = 10000;

export interface TripPositionsCatchPoint {
  lat: number;
  lng: number;
}

@Component({ components: { BaseLayerToggle } })
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
  // Résolue une fois le style chargé, l'icône de capture enregistrée et les
  // couches ajoutées : préalable au cadrage final et à la capture (#193).
  private ready: Promise<void> | null = null;
  baseLayer: BaseLayer = 'plan';

  get hasBeginPosition(): boolean {
    return this.beginLatitude != null && this.beginLongitude != null;
  }

  get hasEndPosition(): boolean {
    return this.endLatitude != null && this.endLongitude != null;
  }

  get hasCatches(): boolean {
    return !!this.catches && this.catches.length > 0;
  }

  /** Une sortie dont la seule position est une capture a aussi sa carte (#193). */
  get hasAnyPosition(): boolean {
    return this.hasBeginPosition || this.hasEndPosition || this.hasCatches;
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
    const map = this.map;
    // Le pin de capture (goutte bleue, même style que MyTripsMap.vue) est
    // enregistré AVANT d'ajouter la couche symbole : chargé à la demande
    // (`styleimagemissing`), il pouvait manquer à la capture PDF (#193).
    this.ready = new Promise((resolve) => {
      map.once('load', () => {
        addCatchPinIcon(map, CATCH_PIN_ID, CATCH_COLOR, 'fish').then(() => {
          if (this.map !== map) {
            return;
          }
          map.resize();
          this.addEndpointLayer();
          this.refreshMarkers();
          this.addCatchLayer();
          this.fitToMarkers();
          resolve();
        });
      });
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

  /** Début, fin et captures : l'emprise de la zone pêchée (#193). */
  private allPoints(): [number, number][] {
    const points: [number, number][] = [];
    if (this.hasBeginPosition) {
      points.push([this.beginLongitude!, this.beginLatitude!]);
    }
    if (this.hasEndPosition) {
      points.push([this.endLongitude!, this.endLatitude!]);
    }
    (this.catches || []).forEach((c) => points.push([c.lng, c.lat]));
    return points;
  }

  private pointsBounds(points: [number, number][]): [[number, number], [number, number]] {
    const lngs = points.map((p) => p[0]);
    const lats = points.map((p) => p[1]);
    return [
      [Math.min(...lngs), Math.min(...lats)],
      [Math.max(...lngs), Math.max(...lats)],
    ];
  }

  private initialCenter(): [number, number] {
    const points = this.allPoints();
    if (points.length) {
      const [[west, south], [east, north]] = this.pointsBounds(points);
      return [(west + east) / 2, (south + north) / 2];
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
    if (this.captureMode) {
      this.refreshEndpointLayer();
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

  // Les marqueurs DOM ne font pas partie du canvas WebGL : en mode capture,
  // début et fin sont dessinés en couche cercle pour figurer dans le PDF (#193).
  private endpointsGeoJson(): any {
    const features: any[] = [];
    if (this.hasBeginPosition) {
      features.push(this.endpointFeature(this.beginLongitude!, this.beginLatitude!, BEGIN_COLOR));
    }
    if (this.hasEndPosition) {
      features.push(this.endpointFeature(this.endLongitude!, this.endLatitude!, END_COLOR));
    }
    return { type: 'FeatureCollection', features };
  }

  private endpointFeature(lng: number, lat: number, color: string): any {
    return {
      type: 'Feature',
      geometry: { type: 'Point', coordinates: [lng, lat] },
      properties: { color },
    };
  }

  private addEndpointLayer() {
    if (!this.map || !this.captureMode || this.map.getSource('trip-endpoints')) {
      return;
    }
    this.map.addSource('trip-endpoints', { type: 'geojson', data: this.endpointsGeoJson() });
    this.map.addLayer({
      id: 'trip-endpoint-points',
      type: 'circle',
      source: 'trip-endpoints',
      paint: {
        'circle-radius': 9,
        'circle-color': ['get', 'color'],
        'circle-stroke-width': 3,
        'circle-stroke-color': '#ffffff',
      },
    });
  }

  private refreshEndpointLayer() {
    const source = this.map?.getSource('trip-endpoints') as GeoJSONSource | undefined;
    source?.setData(this.endpointsGeoJson());
  }

  // Marges de cadrage proportionnelles à la taille de la carte (et non plus
  // 40 px fixes), plus la place des pins ancrés par le bas (#193).
  private framingPadding(width: number, height: number) {
    const margin = Math.round(FRAMING_MARGIN_RATIO * Math.min(width, height));
    return {
      top: margin + PIN_HEIGHT_PX,
      bottom: margin,
      left: margin + PIN_HALF_WIDTH_PX,
      right: margin + PIN_HALF_WIDTH_PX,
    };
  }

  // Cadre l'emprise début/fin/captures (#173, #193). Un point unique est
  // centré (décalé de la moitié du pin pour centrer le pin lui-même). Sans
  // animation en mode capture : le cadrage doit être appliqué avant `idle`.
  private fitToMarkers() {
    if (!this.map) {
      return;
    }
    const points = this.allPoints();
    const { clientWidth: width, clientHeight: height } = this.map.getContainer();
    if (!points.length || !width || !height) {
      return;
    }
    const animate = !this.captureMode;
    if (points.length === 1) {
      this.map.easeTo({
        center: points[0],
        zoom: MAX_FIT_ZOOM,
        offset: [0, PIN_HEIGHT_PX / 2],
        duration: animate ? 500 : 0,
      });
      return;
    }
    const bounds: LngLatBoundsLike = this.pointsBounds(points);
    this.map.fitBounds(bounds, {
      padding: this.framingPadding(width, height),
      maxZoom: MAX_FIT_ZOOM,
      animate,
    });
  }

  toggleBase() {
    this.baseLayer = this.baseLayer === 'plan' ? 'satellite' : 'plan';
    if (this.map) {
      setBaseLayer(this.map, this.baseLayer);
    }
  }

  // #173 (export PDF) : nécessite `captureMode` (sinon le tampon WebGL est
  // effacé et l'image est vide). #193 : une fois la carte prête, on la
  // redimensionne à la taille finale de son conteneur PUIS on recadre, sans
  // animation, et l'on attend `idle` (tuiles chargées) avant de lire le canvas.
  async captureImage(): Promise<string | null> {
    const map = this.map;
    if (!map || !this.ready) {
      return null;
    }
    await this.ready;
    if (this.map !== map) {
      return null;
    }
    return new Promise((resolve) => {
      const capture = () => {
        clearTimeout(timeout);
        resolve(map.getCanvas().toDataURL('image/png'));
      };
      const timeout = setTimeout(() => {
        map.off('idle', capture);
        capture();
      }, CAPTURE_TIMEOUT_MS);
      map.resize();
      this.fitToMarkers();
      map.once('idle', capture);
      map.triggerRepaint();
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

// Capture PDF (#193) : la carte est rendue au ratio exact du cadre du PDF
// (TripPdfExportCard, MAP_ASPECT_RATIO = 3/2) au lieu d'un bandeau de 220 px
// de haut sur toute la largeur. Repli sur 220 px sans `aspect-ratio`.
.trip-positions-map.capture-mode {
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;

  @supports (aspect-ratio: 3 / 2) {
    height: auto;
    aspect-ratio: 3 / 2;
  }
}

.trip-positions-map-container {
  width: 100%;
  height: 100%;
}


.trip-positions-map-legend {
  position: absolute;
  top: 10px;
  left: 50px;
  z-index: 500;
  margin: 0;
  padding: 4px 8px;
  list-style: none;
  background-color: @surface-overlay;
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

// Au-dessus de la bascule Plan/Satellite, posée en bas à gauche (#199).
.trip-positions-map-hint {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 66px;
  z-index: 500;
  background-color: @surface-overlay;
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
