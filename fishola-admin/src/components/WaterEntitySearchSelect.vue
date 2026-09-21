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
  <b-autocomplete
    v-model="search"
    :data="suggestions"
    field="name"
    :placeholder="placeholder"
    :loading="loading"
    icon="magnify"
    clearable
    @typing="onTyping"
    @select="onSelect"
  >
    <template #empty>Aucun résultat</template>
  </b-autocomplete>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import { BAutocomplete } from "buefy";
import { ref } from "vue";

interface WaterEntityOption {
  id: string;
  name: string;
}

interface Props {
  placeholder?: string;
}

withDefaults(defineProps<Props>(), {
  placeholder: "Rechercher un plan d'eau, une rivière..."
});

const emit = defineEmits<{
  (e: "update:modelValue", value: string | null): void
}>();

const search = ref("");
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

function onSelect(option: WaterEntityOption | null) {
  emit("update:modelValue", option ? option.id : null);
  search.value = option ? option.name : "";
}
</script>
