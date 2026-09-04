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
  <section class="section">
    <h1 class="title">Nouvelle saisie — Carnet volontaire</h1>

    <div class="columns is-multiline">
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
      <b-field label="Mode de pêche" class="column is-3">
        <b-select v-model="trip.fishingMode" expanded>
          <option v-for="m in FISHING_MODES" :key="m" :value="m">{{ m }}</option>
        </b-select>
      </b-field>
      <b-field label="Nombre de lignes" class="column is-3">
        <b-input type="number" min="1" v-model.number="trip.rodCount"></b-input>
      </b-field>

      <b-field label="Entité hydrographique" class="column is-6">
        <b-select v-model="trip.waterEntityId" expanded>
          <option v-for="w in waterEntities" :key="w.id" :value="w.id">{{ w.name }}</option>
        </b-select>
      </b-field>
      <b-field label="Technique principale" class="column is-6">
        <b-select v-model="trip.techniqueId" expanded>
          <option v-for="t in techniques" :key="t.id" :value="t.id">{{ t.name }}</option>
        </b-select>
      </b-field>

      <b-field label="Espèce recherchée" class="column is-6">
        <b-select v-model="trip.expectedSpeciesId" expanded :disabled="trip.noExpectedSpecies">
          <option v-for="s in species" :key="s.id" :value="s.id">{{ s.name }}</option>
        </b-select>
      </b-field>
      <b-field class="column is-6 no-species-field">
        <b-switch v-model="trip.noExpectedSpecies">Aucune espèce recherchée</b-switch>
      </b-field>

      <b-field class="column is-12">
        <b-switch v-model="trip.bredouille">Bredouille (aucune capture)</b-switch>
      </b-field>
    </div>

    <b-collapse class="card advanced-fields" animation="slide" :open="false">
      <template #trigger="props">
        <div class="card-header" role="button">
          <p class="card-header-title">Options avancées (technique secondaire, appât/leurre, observations)</p>
          <a class="card-header-icon">
            <b-icon :icon="props.open ? 'menu-up' : 'menu-down'"></b-icon>
          </a>
        </div>
      </template>
      <div class="card-content">
        <div class="columns is-multiline">
          <b-field label="Technique secondaire" class="column is-6">
            <b-select v-model="trip.secondaryTechniqueId" expanded>
              <option :value="null">—</option>
              <option v-for="t in techniques" :key="t.id" :value="t.id">{{ t.name }}</option>
            </b-select>
          </b-field>
          <b-field label="Appât / type de leurre" class="column is-6">
            <b-input v-model="trip.baitOrLure"></b-input>
          </b-field>
          <b-field label="Observations diverses" class="column is-12">
            <b-input type="textarea" rows="2" v-model="trip.observations"
              placeholder="Cormorans, harle bièvre, pollution…"></b-input>
          </b-field>
        </div>
      </div>
    </b-collapse>

    <div v-if="!trip.bredouille">
      <h2 class="subtitle is-5">Captures</h2>
      <div v-for="(c, i) in captures" :key="i" class="box">
        <div class="columns is-multiline is-vcentered">
          <b-field label="Espèce" class="column is-3">
            <b-select v-model="c.speciesId" expanded>
              <option v-for="s in species" :key="s.id" :value="s.id">{{ s.name }}</option>
            </b-select>
          </b-field>
          <b-field label="Nombre" class="column is-2">
            <b-input type="number" min="1" v-model.number="c.quantity"></b-input>
          </b-field>

          <template v-if="isLot(c)">
            <b-field label="Taille min (cm)" class="column is-2">
              <b-input type="number" min="0" v-model.number="c.lotMinSize"></b-input>
            </b-field>
            <b-field label="Taille max (cm)" class="column is-2">
              <b-input type="number" min="0" v-model.number="c.lotMaxSize"></b-input>
            </b-field>
          </template>
          <b-field v-else label="Taille (cm)" class="column is-2">
            <b-input type="number" min="0" v-model.number="c.size"></b-input>
          </b-field>

          <b-field label="Poids (g)" class="column is-2">
            <b-input type="number" min="0" v-model.number="c.weight"></b-input>
          </b-field>
          <b-field label="Conservée" class="column is-1">
            <b-switch v-model="c.kept"></b-switch>
          </b-field>
          <div class="column is-1">
            <b-button type="is-danger" icon-left="delete" @click="removeCapture(i)"></b-button>
          </div>

          <b-field label="Marquée / baguée" class="column is-3">
            <b-switch v-model="c.tagged"></b-switch>
          </b-field>
          <b-field v-if="c.tagged" label="N° de marquage" class="column is-4">
            <b-input v-model="c.tagReference"></b-input>
          </b-field>
          <b-field label="Origine (si truite fario)" class="column is-4">
            <b-select v-model="c.troutOrigin" expanded>
              <option :value="null">—</option>
              <option v-for="o in TROUT_ORIGINS" :key="o" :value="o">{{ o }}</option>
            </b-select>
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

// Référentiel fermé propre à ce format (spec client), distinct de la liste générique
// des modes de pêche (CarnetVolontaireSchema.MODES_PECHE côté backend).
const FISHING_MODES = ["bateau", "float tube/canoë", "bord itinérant", "bord statique"];
const TROUT_ORIGINS = ["naturelle", "déversement", "inconnue"];

const waterEntities = ref<any[]>([]);
const techniques = ref<any[]>([]);
const species = ref<any[]>([]);

function newTrip() {
  return {
    day: "",
    startTime: "",
    endTime: "",
    waterEntityId: null,
    fishingMode: FISHING_MODES[0],
    techniqueId: null,
    secondaryTechniqueId: null,
    rodCount: 1,
    baitOrLure: "",
    expectedSpeciesId: null,
    noExpectedSpecies: false,
    observations: "",
    bredouille: false
  };
}

const trip = ref<any>(newTrip());
const captures = ref<any[]>([newCapture()]);

const loading = ref(false);
const success = ref<any>(null);
const errors = ref<any[]>([]);

const startTimeError = ref("");
const endTimeError = ref("");

const todayIso = computed(() => new Date().toISOString().slice(0, 10));

function isLot(c: any): boolean {
  return Number(c.quantity) > 1;
}

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
    lotMinSize: null,
    lotMaxSize: null,
    weight: null,
    kept: false,
    techniqueId: null,
    baitOrLure: "",
    captureTime: null,
    troutOrigin: null,
    tagged: false,
    tagReference: ""
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
  const lot = isLot(c);
  return {
    speciesId: c.speciesId,
    quantity: toIntOrNull(c.quantity),
    size: lot ? null : toIntOrNull(c.size),
    lotMinSize: lot ? toIntOrNull(c.lotMinSize) : null,
    lotMaxSize: lot ? toIntOrNull(c.lotMaxSize) : null,
    weight: toIntOrNull(c.weight),
    kept: !!c.kept,
    techniqueId: c.techniqueId || null,
    baitOrLure: c.baitOrLure || null,
    captureTime: c.captureTime || null,
    troutOrigin: c.troutOrigin || null,
    tagged: !!c.tagged,
    tagReference: c.tagged ? (c.tagReference || null) : null
  };
}

async function submit() {
  loading.value = true;
  success.value = null;
  errors.value = [];
  const payload = {
    ...trip.value,
    rodCount: toIntOrNull(trip.value.rodCount),
    captures: trip.value.bredouille ? [] : captures.value.map(cleanCapture)
  };
  try {
    const result = await BackendService.backendPost("/v1/admin/manual-entries/carnet-volontaire", payload);
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
  trip.value = newTrip();
  captures.value = [newCapture()];
  startTimeError.value = "";
  endTimeError.value = "";
}
</script>

<style scoped lang="less">
.no-species-field {
  display: flex;
  align-items: center;
}
.advanced-fields {
  margin-bottom: 1.5rem;
  box-shadow: none;
  border: 1px solid #e2e8ec;

  .card-header {
    box-shadow: none;
    cursor: pointer;
  }
}
</style>
