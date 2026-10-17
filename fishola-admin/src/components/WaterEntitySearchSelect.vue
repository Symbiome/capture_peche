<!--
  #%L
  Carnet de pêche :: Admin
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
    <b-field :addons="withMap || withFavorites">
      <b-autocomplete
        v-model="search"
        :data="suggestions"
        field="label"
        :placeholder="placeholder"
        :loading="loading"
        :open-on-focus="withFavorites"
        icon="magnify"
        clearable
        expanded
        @typing="onTyping"
        @select="onSelect"
      >
        <template #default="{ option }">
          <span v-if="isFavorite(option.id)" class="favorite-mark">★ </span>{{ option.label }}
        </template>
        <template #empty>Aucun résultat</template>
      </b-autocomplete>
      <p v-if="withFavorites" class="control">
        <b-button :icon-left="isFavorite(modelValue) ? 'star' : 'star-outline'" :disabled="!modelValue"
          :title="isFavorite(modelValue) ? 'Retirer des favoris' : 'Ajouter aux favoris'" @click="toggleFavorite" />
      </p>
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
import { waterEntityLabels } from "@/utils/utils";
import WaterEntityMapPicker, { MapPick, MapPosition } from "@/components/WaterEntityMapPicker.vue";
import { BAutocomplete } from "buefy";
import { nextTick, ref, watch } from "vue";

interface WaterEntityName {
  id: string;
  name: string;
  department?: string;
  commune?: string;
}

interface WaterEntityOption extends WaterEntityName {
  /** Nom, suffixé du département pour les homonymes (#230). */
  label: string;
}

interface Props {
  modelValue?: string | null;
  /** Point saisi sur la carte (#189), en v-model:position ; nul si l'entité est choisie par son nom. */
  position?: MapPosition | null;
  /** Affiche le bouton « Carte » pour saisir la position de la pêche. */
  withMap?: boolean;
  /** Secteurs favoris du compte (#236) : étoile, et favoris proposés avant toute saisie. */
  withFavorites?: boolean;
  /** Libellé de l'entité quand le parent impose modelValue (ex. secteur pré-rempli depuis la session). */
  initialLabel?: string | null;
  placeholder?: string;
}

// #189 : sans ce réglage, les attributs non déclarés retombaient sur le <b-autocomplete>
// racine et écrasaient son propre v-model (le libellé sélectionné disparaissait).
defineOptions({ inheritAttrs: false });

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  position: null,
  withMap: false,
  withFavorites: false,
  initialLabel: null,
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

const search = ref(props.modelValue && props.initialLabel ? props.initialLabel : "");
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
      suggestions.value = favoriteOptions();
    } else if (props.initialLabel) {
      search.value = props.initialLabel;
    }
  }
);
const suggestions = ref<WaterEntityOption[]>([]);
const loading = ref(false);
const favorites = ref<WaterEntityName[]>([]);

const FAVORITES_URI = "/v1/admin/favorite-water-entities";

/** Le secteur fait-il partie des favoris du compte ? */
function isFavorite(id: string | null | undefined): boolean {
  return !!id && favorites.value.some((favorite) => favorite.id === id);
}

/** Favoris proposés avant toute saisie, vide sans withFavorites. */
function favoriteOptions(): WaterEntityOption[] {
  return props.withFavorites ? withLabels(favorites.value) : [];
}

/** Charge les favoris du compte ; une erreur laisse simplement la liste vide. */
async function loadFavorites() {
  try {
    favorites.value = await BackendService.backendGet(FAVORITES_URI);
  } catch {
    favorites.value = [];
  }
  if ((search.value || "").trim().length < 2) {
    suggestions.value = favoriteOptions();
  }
}

/** Ajoute ou retire des favoris le secteur sélectionné. */
async function toggleFavorite() {
  const id = props.modelValue;
  if (!id) {
    return;
  }
  if (isFavorite(id)) {
    await BackendService.backendDelete(FAVORITES_URI + "/" + id);
    favorites.value = favorites.value.filter((favorite) => favorite.id !== id);
  } else {
    await BackendService.backendPut(FAVORITES_URI + "/" + id, null);
    favorites.value = [...favorites.value, { id, name: search.value }];
  }
}

if (props.withFavorites) {
  loadFavorites();
}

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
    suggestions.value = favoriteOptions();
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
        suggestions.value = withLabels(results);
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

function withLabels(results: WaterEntityName[]): WaterEntityOption[] {
  const labels = waterEntityLabels(results);
  return results.map((result) => ({ ...result, label: labels.get(result.id) || result.name }));
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
    search.value = option.label;
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
.favorite-mark {
  color: #e0a800;
}
.position-label {
  font-size: 0.85rem;
  margin-top: 0.25rem;
}
</style>
