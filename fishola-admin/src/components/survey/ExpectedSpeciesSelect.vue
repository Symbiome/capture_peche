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
  <b-autocomplete v-model="search" :data="suggestions" field="label" open-on-focus clearable keep-first
    placeholder="Rechercher une espèce…" @select="onSelect" @blur="restoreLabel">
    <template #empty>Aucune espèce</template>
  </b-autocomplete>
</template>

<script setup lang="ts">
import { ALL_SPECIES, NO_SPECIES } from "@/components/survey/surveyForm";
import { normalizeSearchText } from "@/utils/utils";
import { computed, ref, watch } from "vue";

interface SpeciesOption {
  id: string;
  label: string;
}

interface Props {
  /** Identifiant d'espèce, ou ALL_SPECIES / NO_SPECIES. */
  modelValue: string;
  species: { id: string; name: string }[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: "update:modelValue", value: string): void;
}>();

// « Toutes espèces » (défaut) et « Aucune » en tête, puis les espèces (#236).
const options = computed<SpeciesOption[]>(() => [
  { id: ALL_SPECIES, label: "Toutes espèces" },
  { id: NO_SPECIES, label: "Aucune" },
  ...props.species.map((s) => ({ id: s.id, label: s.name }))
]);

const search = ref("");

const suggestions = computed(() => {
  const term = normalizeSearchText(search.value);
  const selected = labelOf(props.modelValue);
  if (!term || search.value === selected) {
    return options.value;
  }
  return options.value.filter((o) => normalizeSearchText(o.label).includes(term));
});

/** Libellé de la valeur courante. */
function labelOf(id: string): string {
  const option = options.value.find((o) => o.id === id);
  return option ? option.label : "";
}

watch(
  () => [props.modelValue, props.species.length],
  () => {
    search.value = labelOf(props.modelValue);
  },
  { immediate: true }
);

/** Émet l'espèce choisie ; un champ vidé revient à « Toutes espèces ». */
function onSelect(option: SpeciesOption | null) {
  if (option) {
    emit("update:modelValue", option.id);
  }
}

/** Réaffiche le libellé de la valeur retenue si la saisie n'a rien sélectionné. */
function restoreLabel() {
  setTimeout(() => {
    if (!search.value) {
      emit("update:modelValue", ALL_SPECIES);
    }
    search.value = labelOf(props.modelValue);
  }, 200);
}
</script>
