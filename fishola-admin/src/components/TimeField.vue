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
  <b-field :label="label" :type="message ? 'is-danger' : ''" :message="message">
    <input type="text" inputmode="numeric" maxlength="5" placeholder="HH:mm" class="input" :value="modelValue"
      @input="onInput" @blur="onBlur" />
  </b-field>
</template>

<script setup lang="ts">
import { maskTimeInput, isValidTimeString } from "@/utils/utils";
import { computed, ref } from "vue";

const TIME_FORMAT_ERROR = "Heure invalide (format 24h HH:mm, ex. 13:45)";

interface Props {
  modelValue: string;
  label: string;
  /** Erreur métier calculée par le parent (ex. ordre des heures), affichée si le format est valide. */
  error?: string;
}

const props = withDefaults(defineProps<Props>(), { error: "" });

const emit = defineEmits<{
  (e: "update:modelValue", value: string): void;
}>();

// Masque de saisie HH:mm indépendant de la locale : les <input type="time"> natifs
// affichent AM/PM selon la locale de l'appareil.
const formatError = ref("");

const message = computed(() => formatError.value || props.error);

/** Masque la saisie et signale un format invalide une fois les 5 caractères tapés. */
function onInput(event: Event) {
  const masked = maskTimeInput((event.target as HTMLInputElement).value);
  emit("update:modelValue", masked);
  if (!masked || isValidTimeString(masked)) {
    formatError.value = "";
  } else if (masked.length === 5) {
    formatError.value = TIME_FORMAT_ERROR;
  }
}

/** Signale un format invalide en quittant le champ. */
function onBlur() {
  const value = props.modelValue;
  formatError.value = !value || isValidTimeString(value) ? "" : TIME_FORMAT_ERROR;
}
</script>
