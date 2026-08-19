<!--
  #%L
  Fishola :: Admin
  %%
  Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
  <section class="section">
    <h1 class="title">Nouvelle saisie opérateur</h1>

    <div class="columns is-multiline">
      <b-field label="Méthode de recueil" class="column is-4">
        <b-select v-model="trip.collectionMethod" expanded>
          <option v-for="m in collectionMethods" :key="m.value" :value="m.value">{{ m.label }}</option>
        </b-select>
      </b-field>

      <b-field label="Date" class="column is-2">
        <input type="date" class="input" v-model="trip.day" :max="todayIso" />
      </b-field>
      <b-field label="Heure de début" class="column is-2" :type="startTimeError ? 'is-danger' : ''"
        :message="startTimeError">
        <input type="text" inputmode="numeric" maxlength="5" placeholder="HH:mm" class="input" :value="trip.startTime"
          @input="onTimeInput($event, 'startTime')" @blur="onTimeBlur('startTime')" />
      </b-field>
      <b-field label="Heure de fin" class="column is-2" :type="endTimeError ? 'is-danger' : ''" :message="endTimeError">
        <input type="text" inputmode="numeric" maxlength="5" placeholder="HH:mm" class="input" :value="trip.endTime"
          @input="onTimeInput($event, 'endTime')" @blur="onTimeBlur('endTime')" />
      </b-field>

      <b-field label="Entité hydrographique" class="column is-6">
        <b-select v-model="trip.waterEntityId" expanded>
          <option v-for="w in waterEntities" :key="w.id" :value="w.id">{{ w.name }}</option>
        </b-select>
      </b-field>
      <b-field label="Technique" class="column is-6">
        <b-select v-model="trip.techniqueId" expanded>
          <option v-for="t in techniques" :key="t.id" :value="t.id">{{ t.name }}</option>
        </b-select>
      </b-field>

      <b-field class="column is-12">
        <b-switch v-model="trip.bredouille">Bredouille (aucune capture)</b-switch>
      </b-field>
    </div>

    <div v-if="!trip.bredouille">
      <h2 class="subtitle is-5">Captures</h2>
      <div v-for="(c, i) in captures" :key="i" class="box">
        <div class="columns is-multiline is-vcentered">
          <b-field label="Espèce" class="column is-3">
            <b-select v-model="c.speciesId" expanded>
              <option v-for="s in species" :key="s.id" :value="s.id">{{ s.name }}</option>
            </b-select>
          </b-field>
          <b-field label="Nombre" class="column is-1">
            <b-input type="number" min="1" v-model.number="c.quantity"></b-input>
          </b-field>
          <b-field label="Taille (cm)" class="column is-2">
            <b-input type="number" min="0" v-model.number="c.size"></b-input>
          </b-field>
          <b-field label="Poids (g)" class="column is-2">
            <b-input type="number" min="0" v-model.number="c.weight"></b-input>
          </b-field>
          <b-field label="Classe de taille" class="column is-2">
            <b-input v-model="c.sizeClass"></b-input>
          </b-field>
          <b-field label="Conservée" class="column is-1">
            <b-switch v-model="c.kept"></b-switch>
          </b-field>
          <div class="column is-1">
            <b-button type="is-danger" icon-left="delete" @click="removeCapture(i)"></b-button>
          </div>
          <b-field label="Pathologies / description" class="column is-12">
            <b-input type="textarea" rows="2" v-model="c.description"></b-input>
          </b-field>
        </div>
      </div>
      <b-button type="is-light" icon-left="plus" @click="addCapture">Ajouter une capture</b-button>
    </div>

    <div class="mt-5">
      <b-button type="is-primary" icon-left="content-save" :loading="loading" @click="submit">
        Enregistrer la sortie
      </b-button>
    </div>

    <b-notification v-if="success" type="is-success" class="mt-4" @close="success = null">
      Sortie enregistrée ({{ success.captures }} capture(s)).
    </b-notification>

    <b-notification v-if="errors.length" type="is-danger" class="mt-4" :closable="true" @close="errors = []">
      <p><strong>Saisie non enregistrée :</strong></p>
      <ul>
        <li v-for="(e, i) in errors" :key="i">
          <span v-if="e.index != null">Capture {{ e.index + 1 }} — </span>
          <span v-if="e.field">[{{ e.field }}] </span>{{ e.message }}
        </li>
      </ul>
    </b-notification>
  </section>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import { maskTimeInput, isValidTimeString } from "@/utils/utils";
import { ref, computed } from "vue";

const TIME_FORMAT_ERROR = "Heure invalide (format 24h HH:mm, ex. 13:45)";

const collectionMethods = [
  { value: "enquete", label: "Enquête" },
  { value: "carnet_volontaire", label: "Carnet volontaire" },
  { value: "carnet_obligatoire", label: "Carnet obligatoire" }
];

const waterEntities = ref<any[]>([]);
const techniques = ref<any[]>([]);
const species = ref<any[]>([]);

const trip = ref<any>({
  collectionMethod: "enquete",
  day: "",
  startTime: "",
  endTime: "",
  waterEntityId: null,
  techniqueId: null,
  bredouille: false
});
const captures = ref<any[]>([newCapture()]);

const loading = ref(false);
const success = ref<any>(null);
const errors = ref<any[]>([]);

const startTimeError = ref("");
const endTimeError = ref("");

const todayIso = computed(() => new Date().toISOString().slice(0, 10));

function onTimeInput(event: Event, field: "startTime" | "endTime") {
  const raw = (event.target as HTMLInputElement).value;
  const masked = maskTimeInput(raw);
  trip.value[field] = masked;

  const errorRef = field === "startTime" ? startTimeError : endTimeError;
  if (!masked || isValidTimeString(masked)) {
    errorRef.value = "";
  } else if (masked.length === 5) {
    errorRef.value = TIME_FORMAT_ERROR;
  }
}

function onTimeBlur(field: "startTime" | "endTime") {
  const value = trip.value[field];
  const errorRef = field === "startTime" ? startTimeError : endTimeError;
  errorRef.value = !value || isValidTimeString(value) ? "" : TIME_FORMAT_ERROR;
}

loadReferentials();

async function loadReferentials() {
  waterEntities.value = await BackendService.backendGet("/v1/referential/waterEntities");
  techniques.value = await BackendService.backendGet("/v1/referential/techniques");
  species.value = await BackendService.backendGet("/v1/referential/species");
}

function newCapture() {
  return {
    speciesId: null,
    quantity: 1,
    size: null,
    weight: null,
    sizeClass: "",
    kept: false,
    description: ""
  };
}

function addCapture() {
  captures.value.push(newCapture());
}

function removeCapture(i: number) {
  captures.value.splice(i, 1);
}

function toIntOrNull(v: any): number | null {
  if (v === "" || v === null || v === undefined) return null;
  const n = Number(v);
  return Number.isFinite(n) ? Math.trunc(n) : null;
}

function cleanCapture(c: any) {
  return {
    speciesId: c.speciesId,
    quantity: toIntOrNull(c.quantity),
    size: toIntOrNull(c.size),
    weight: toIntOrNull(c.weight),
    sizeClass: c.sizeClass || null,
    kept: !!c.kept,
    description: c.description || null
  };
}

async function submit() {
  loading.value = true;
  success.value = null;
  errors.value = [];
  const payload = {
    ...trip.value,
    captures: trip.value.bredouille ? [] : captures.value.map(cleanCapture)
  };
  try {
    const result = await BackendService.backendPost("/v1/admin/manual-entries", payload);
    success.value = result;
    resetForm();
  } catch (rejected: any) {
    const content = rejected && rejected.content;
    if (content && content.errors && content.errors.length) {
      errors.value = content.errors;
    } else {
      errors.value = [{ index: null, field: null, message: "Échec de l'enregistrement (réessayer)." }];
    }
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  trip.value = {
    collectionMethod: "enquete",
    day: "",
    startTime: "",
    endTime: "",
    waterEntityId: null,
    techniqueId: null,
    bredouille: false
  };
  captures.value = [newCapture()];
  startTimeError.value = "";
  endTimeError.value = "";
}
</script>
