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
  <b-select :key="String(showAll)" :model-value="modelValue" @update:model-value="onChange">
    <option v-if="nullLabel" :value="null">{{ nullLabel }}</option>
    <option v-for="item in options" :key="item.id" :value="item.id">{{ item.name }}</option>
    <option v-if="hidden > 0" :value="SHOW_ALL">Afficher tout le référentiel ({{ hidden }} masqué(s))</option>
  </b-select>
</template>

<script setup lang="ts">
import { EntryReferential, SHOW_ALL, hiddenCount, proposedItems } from "@/utils/entryReferentials";
import { computed, ref } from "vue";

interface Props {
  modelValue: string | null;
  referential: EntryReferential;
  /** Libellé d'une option « aucune valeur », absente si non renseigné. */
  nullLabel?: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: "update:modelValue", value: string | null): void;
}>();

// Sert aussi de key au b-select : le remonter réaffiche la valeur retenue au lieu de l'option choisie.
const showAll = ref(false);

const options = computed(() => proposedItems(props.referential, props.modelValue, showAll.value));

const hidden = computed(() => (showAll.value ? 0 : hiddenCount(props.referential, props.modelValue)));

/** « Afficher tout le référentiel » déplie la liste sans changer la valeur retenue. */
function onChange(value: string | null) {
  if (value === SHOW_ALL) {
    showAll.value = true;
  } else {
    emit("update:modelValue", value);
  }
}
</script>
