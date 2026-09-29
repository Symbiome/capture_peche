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
<template>
  <div class="water-entity-search-select">
    <b-field :addons="withMap">
      <b-autocomplete
        v-model="search"
        :data="suggestions"
        field="name"
        :placeholder="placeholder"
        :loading="loading"
        icon="magnify"
        clearable
        expanded
        @typing="onTyping"
        @select="onSelect"
      >
        <template #empty>Aucun résultat</template>
      </b-autocomplete>
      <p v-if="withMap" class="control">
        <b-button icon-left="map-marker" @click="mapOpen = true">Carte</b-button>
      </p>
    </b-field>
    <p v-if="withMap && position" class="position-label">
      Position : {{ position.lat.toFixed(5) }}, {{ position.lng.toFixed(5) }}
    </p>
    <b-modal v-if="withMap" v-model="mapOpen" has-modal-card trap-focus>
      <WaterEntityMapPicker :initial-position="position" @picked="onMapPicked" @close="mapOpen = false" />
    </b-modal>
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import WaterEntityMapPicker, { MapPick, MapPosition } from "@/components/WaterEntityMapPicker.vue";
import { BAutocomplete } from "buefy";
import { nextTick, ref, watch } from "vue";

interface WaterEntityOption {
  id: string;
  name: string;
}

interface Props {
  modelValue?: string | null;
  /** Point saisi sur la carte (#189), en v-model:position ; nul si l'entité est choisie par son nom. */
  position?: MapPosition | null;
  /** Affiche le bouton « Carte » pour saisir la position de la pêche. */
  withMap?: boolean;
  placeholder?: string;
}

// #189 : sans ce réglage, les attributs non déclarés retombaient sur le <b-autocomplete>
// racine et écrasaient son propre v-model (le libellé sélectionné disparaissait).
defineOptions({ inheritAttrs: false });

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  position: null,
  withMap: false,
  placeholder: "Rechercher un plan d'eau, une rivière..."
});

const emit = defineEmits<{
  (e: "update:modelValue", value: string | null): void;
  (e: "update:position", value: MapPosition | null): void;
}>();

const mapOpen = ref(false);
// Vrai le temps que Buefy réagisse au libellé posé par un choix sur la carte : il émet
// alors select(null) (texte ≠ ancienne sélection), à ne pas confondre avec une désélection.
let applyingMapPick = false;

const search = ref("");
// Dernière valeur émise : distingue nos propres émissions d'un changement imposé par le parent.
let emittedValue: string | null = props.modelValue ?? null;

// Remise à zéro par le parent (ex. formulaire réinitialisé après soumission) : on vide l'input.
watch(
  () => props.modelValue,
  (value) => {
    const normalized = value ?? null;
    if (normalized === emittedValue) {
      return;
    }
    emittedValue = normalized;
    if (normalized === null) {
      search.value = "";
      suggestions.value = [];
    }
  }
);
const suggestions = ref<WaterEntityOption[]>([]);
const loading = ref(false);

// Le référentiel hydro national (~181 000 lignes) ne doit jamais être chargé ni
// rendu en une fois ici : au-delà d'un plan d'eau déjà sélectionné (v-for sur un
// <select> exhaustif), Vue met plusieurs secondes à monter les options et gèle
// l'onglet. La recherche passe donc exclusivement par le endpoint serveur
// (mêmes principes que fishola-mobile/LakeSelection.vue), débouncée pour ne pas
// spammer la BDD à chaque frappe.
let searchTimer: ReturnType<typeof setTimeout> | null = null;
let searchSeq = 0;

function onTyping(term: string) {
  if (searchTimer) {
    clearTimeout(searchTimer);
  }

  const trimmed = (term || "").trim();
  if (trimmed.length < 2) {
    suggestions.value = [];
    loading.value = false;
    return;
  }

  loading.value = true;
  searchTimer = setTimeout(async () => {
    const seq = ++searchSeq;
    try {
      const results = await BackendService.backendGet(
        "/v1/referential/waterEntities/names/search?q=" + encodeURIComponent(trimmed)
      );
      if (seq === searchSeq) {
        suggestions.value = results;
      }
    } catch {
      if (seq === searchSeq) {
        suggestions.value = [];
      }
    } finally {
      if (seq === searchSeq) {
        loading.value = false;
      }
    }
  }, 250);
}

// Buefy émet select(null) dès qu'on retape après une sélection ou qu'on vide le champ :
// on remet alors le modèle à null sans toucher au texte en cours de saisie.
function onSelect(option: WaterEntityOption | null) {
  if (!option && applyingMapPick) {
    return;
  }
  emittedValue = option ? option.id : null;
  emit("update:modelValue", emittedValue);
  // Entité choisie ou retirée par la recherche : le point de la carte ne s'y rattache plus.
  if (props.position) {
    emit("update:position", null);
  }
  if (option) {
    search.value = option.name;
  }
}

async function onMapPicked(pick: MapPick) {
  applyingMapPick = true;
  emittedValue = pick.waterEntityId;
  emit("update:modelValue", pick.waterEntityId);
  emit("update:position", { lat: pick.lat, lng: pick.lng });
  search.value = pick.name;
  suggestions.value = [];
  await nextTick();
  await nextTick();
  applyingMapPick = false;
}
</script>

<style scoped lang="less">
.position-label {
  font-size: 0.85rem;
  margin-top: 0.25rem;
}
</style>
