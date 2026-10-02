<!--
  #%L
  Fishola :: Admin
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
  Carte de saisie d'une position par le staff (#189), sur le modèle de la saisie d'une
  sortie par le pêcheur : un clic pose le point, le backend propose l'entité hydro la plus
  proche et des alternatives (limitées au périmètre du compte), le choix rattache le point.
  -->
<template>
  <div class="modal-card water-entity-map-picker">
    <header class="modal-card-head">
      <p class="modal-card-title">Positionner la pêche sur la carte</p>
      <button type="button" class="delete" aria-label="Fermer" @click="emit('close')" />
    </header>
    <section class="modal-card-body">
      <div class="map-wrapper">
        <div ref="mapContainer" class="map-container" data-cy="map-picker-map" />
        <button type="button" class="button is-small base-layer-button" @click="toggleBaseLayer">
          {{ baseLayer === "plan" ? "Satellite" : "Plan" }}
        </button>
      </div>

      <p v-if="!clickedPoint" class="help-text">Cliquez sur la carte à l'endroit de la pêche.</p>
      <p v-else-if="loading" class="help-text">Recherche des entités hydrographiques…</p>
      <p v-else-if="!candidates.length" class="help-text has-text-danger">
        Aucune entité hydrographique de votre périmètre à moins de 5 km de ce point.
      </p>
      <div v-else class="candidates">
        <p>Rattacher ce point à :</p>
        <b-button
          v-for="(candidate, i) in candidates"
          :key="candidate.waterEntityId"
          :type="i === 0 ? 'is-primary' : 'is-light'"
          @click="choose(candidate)"
        >
          {{ candidate.name }} ({{ formatDistance(candidate.distanceM) }})
        </b-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import { BaseLayer, buildFisholaStyle, DEFAULT_CENTER, DEFAULT_ZOOM } from "@/components/maplibreStyle";
import maplibregl, { Map as MlMap, Marker } from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";

export interface MapPosition {
  lat: number;
  lng: number;
}

export interface MapPick extends MapPosition {
  waterEntityId: string;
  name: string;
}

interface AttributionCandidate {
  waterEntityId: string;
  name: string;
  distanceM: number;
}

const props = defineProps<{ initialPosition?: MapPosition | null }>();

const emit = defineEmits<{
  (e: "picked", value: MapPick): void;
  (e: "close"): void;
}>();

const mapContainer = ref<HTMLElement | null>(null);
const baseLayer = ref<BaseLayer>("plan");
const clickedPoint = ref<MapPosition | null>(null);
const candidates = ref<AttributionCandidate[]>([]);
const loading = ref(false);

let map: MlMap | null = null;
let marker: Marker | null = null;
let attributionSeq = 0;

onMounted(async () => {
  await nextTick();
  if (!mapContainer.value) {
    return;
  }
  const initial = props.initialPosition ?? null;
  map = new maplibregl.Map({
    container: mapContainer.value,
    style: buildFisholaStyle(baseLayer.value),
    center: initial ? [initial.lng, initial.lat] : DEFAULT_CENTER,
    zoom: initial ? 14 : DEFAULT_ZOOM
  });
  map.addControl(new maplibregl.NavigationControl({ showCompass: false }), "top-left");
  map.on("load", () => map?.resize());
  map.on("click", (e) => onMapClick({ lat: e.lngLat.lat, lng: e.lngLat.lng }));
  if (initial) {
    setMarker(initial);
  }
});

onBeforeUnmount(() => {
  marker?.remove();
  map?.remove();
  map = null;
});

function setMarker(point: MapPosition) {
  if (!map) {
    return;
  }
  if (!marker) {
    marker = new maplibregl.Marker({ color: "#D62137" });
  }
  marker.setLngLat([point.lng, point.lat]).addTo(map);
}

async function onMapClick(point: MapPosition) {
  clickedPoint.value = point;
  setMarker(point);
  loading.value = true;
  const seq = ++attributionSeq;
  try {
    const response = await BackendService.backendGet(
      `/v1/referential/waterEntities/attribution?lat=${point.lat}&lng=${point.lng}`
    );
    if (seq === attributionSeq) {
      candidates.value = [response.proposal, ...(response.alternatives || [])].filter(Boolean);
    }
  } catch {
    if (seq === attributionSeq) {
      candidates.value = [];
    }
  } finally {
    if (seq === attributionSeq) {
      loading.value = false;
    }
  }
}

function choose(candidate: AttributionCandidate) {
  if (!clickedPoint.value) {
    return;
  }
  emit("picked", {
    waterEntityId: candidate.waterEntityId,
    name: candidate.name,
    lat: clickedPoint.value.lat,
    lng: clickedPoint.value.lng
  });
  emit("close");
}

function toggleBaseLayer() {
  baseLayer.value = baseLayer.value === "plan" ? "satellite" : "plan";
  if (map) {
    map.setLayoutProperty("ign-plan", "visibility", baseLayer.value === "plan" ? "visible" : "none");
    map.setLayoutProperty("ign-ortho", "visibility", baseLayer.value === "satellite" ? "visible" : "none");
  }
}

function formatDistance(distanceM: number): string {
  return distanceM < 1000 ? `${Math.round(distanceM)} m` : `${(distanceM / 1000).toFixed(1)} km`;
}
</script>

<style scoped lang="less">
.water-entity-map-picker {
  width: min(900px, 95vw);
}

.map-wrapper {
  position: relative;
}

.map-container {
  height: 55vh;
  min-height: 320px;
}

.base-layer-button {
  position: absolute;
  top: 10px;
  right: 10px;
}

.help-text {
  margin-top: 0.75rem;
}

.candidates {
  margin-top: 0.75rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}
</style>
