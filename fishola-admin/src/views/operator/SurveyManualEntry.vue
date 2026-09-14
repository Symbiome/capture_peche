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
    <h1 class="title">Nouvelle saisie — Enquête terrain</h1>
    <p class="subtitle is-6">
      Session d'enquête → sortie → pêcheur(s) interrogé(s) → captures, avec bloc « session
      souvenir » facultatif par pêcheur. Les codes session / sortie / pêcheur sont générés
      automatiquement.
    </p>

    <h2 class="subtitle is-5">Sortie enquêtée</h2>
    <div class="columns is-multiline">
      <b-field label="Date" class="column is-2">
        <input type="date" class="input" v-model="sortie.day" :max="todayIso" />
      </b-field>
      <b-field label="Heure du contrôle" class="column is-2" :type="timeErrors.controlTime ? 'is-danger' : ''"
        :message="timeErrors.controlTime">
        <input type="text" inputmode="numeric" maxlength="5" placeholder="HH:mm" class="input"
          :value="sortie.controlTime" @input="onTimeInput($event, 'controlTime')"
          @blur="onTimeBlur('controlTime')" />
      </b-field>
      <b-field label="Heure de début" class="column is-2" :type="timeErrors.startTime ? 'is-danger' : ''"
        :message="timeErrors.startTime">
        <input type="text" inputmode="numeric" maxlength="5" placeholder="HH:mm" class="input"
          :value="sortie.startTime" @input="onTimeInput($event, 'startTime')" @blur="onTimeBlur('startTime')" />
      </b-field>
      <b-field label="Heure de fin prévue" class="column is-2" :type="timeErrors.endTime ? 'is-danger' : ''"
        :message="timeErrors.endTime">
        <input type="text" inputmode="numeric" maxlength="5" placeholder="HH:mm" class="input"
          :value="sortie.endTime" @input="onTimeInput($event, 'endTime')" @blur="onTimeBlur('endTime')" />
      </b-field>
      <b-field label="Secteur" class="column is-4">
        <b-select v-model="sortie.waterEntityId" expanded>
          <option v-for="w in waterEntities" :key="w.id" :value="w.id">{{ w.name }}</option>
        </b-select>
      </b-field>

      <b-field label="Pêcheurs carnassiers du bord non-enquêtés" class="column is-4">
        <b-input type="number" min="0" v-model.number="sortie.unsurveyedShoreAnglers"></b-input>
      </b-field>
      <b-field label="Pêcheurs carnassiers en bateau non-enquêtés" class="column is-4">
        <b-input type="number" min="0" v-model.number="sortie.unsurveyedBoatAnglers"></b-input>
      </b-field>
    </div>

    <h2 class="subtitle is-5">Pêcheurs interrogés</h2>
    <div v-for="(angler, i) in anglers" :key="i" class="box angler-box">
      <div class="angler-header">
        <strong>Pêcheur {{ i + 1 }}</strong>
        <b-button type="is-danger" size="is-small" icon-left="delete" :disabled="anglers.length <= 1"
          @click="removeAngler(i)">Retirer</b-button>
      </div>

      <div class="columns is-multiline">
        <b-field label="Département / pays d'origine" class="column is-4">
          <b-input v-model="angler.origin" placeholder="ex. 69, Suisse…"></b-input>
        </b-field>
        <b-field label="Mode de pêche" class="column is-4">
          <b-select v-model="angler.fishingMode" expanded>
            <option v-for="m in FISHING_MODES" :key="m" :value="m">{{ m }}</option>
          </b-select>
        </b-field>
        <b-field label="Technique" class="column is-4">
          <b-select v-model="angler.techniqueId" expanded>
            <option v-for="t in techniques" :key="t.id" :value="t.id">{{ t.name }}</option>
          </b-select>
        </b-field>

        <b-field label="Nombre de lignes" class="column is-3">
          <b-input type="number" min="1" v-model.number="angler.rodCount"></b-input>
        </b-field>
        <b-field label="Appât / type de leurre" class="column is-4">
          <b-input v-model="angler.baitOrLure"></b-input>
        </b-field>
        <b-field label="Espèce recherchée" class="column is-5">
          <b-select v-model="angler.expectedSpeciesId" expanded :disabled="angler.noExpectedSpecies">
            <option v-for="s in species" :key="s.id" :value="s.id">{{ s.name }}</option>
          </b-select>
        </b-field>
        <b-field class="column is-12 no-species-field">
          <b-switch v-model="angler.noExpectedSpecies">Aucune espèce recherchée / toutes espèces</b-switch>
        </b-field>

        <b-field class="column is-12">
          <b-switch v-model="angler.bredouille">Bredouille (aucune capture)</b-switch>
        </b-field>
      </div>

      <div v-if="!angler.bredouille">
        <h3 class="capture-title">Captures</h3>
        <div v-for="(c, j) in angler.captures" :key="j" class="box capture-box">
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

            <b-field label="Conservé" class="column is-2">
              <b-switch v-model="c.kept"></b-switch>
            </b-field>
            <div class="column is-1">
              <b-button type="is-danger" icon-left="delete" :disabled="angler.captures.length <= 1"
                @click="removeCapture(angler, j)"></b-button>
            </div>
          </div>
        </div>
        <b-button type="is-light" icon-left="plus" @click="addCapture(angler)">Ajouter une capture</b-button>
      </div>

      <div class="souvenir-toggle">
        <b-switch v-model="angler.hasSouvenir">Session souvenir (dernière sortie passée du pêcheur)</b-switch>
      </div>

      <div v-if="angler.hasSouvenir" class="box souvenir-box">
        <div class="columns is-multiline">
          <b-field label="Date de la sortie" class="column is-3">
            <input type="date" class="input" v-model="angler.souvenir.day" :max="todayIso" />
          </b-field>
          <b-field label="Période de la journée" class="column is-3">
            <b-select v-model="angler.souvenir.dayPeriod" expanded>
              <option v-for="p in DAY_PERIODS" :key="p" :value="p">{{ p }}</option>
            </b-select>
          </b-field>
          <b-field label="Site pêché" class="column is-6">
            <b-select v-model="angler.souvenir.waterEntityId" expanded>
              <option v-for="w in waterEntities" :key="w.id" :value="w.id">{{ w.name }}</option>
            </b-select>
          </b-field>

          <b-field label="Mode de pêche" class="column is-4">
            <b-select v-model="angler.souvenir.fishingMode" expanded>
              <option v-for="m in FISHING_MODES" :key="m" :value="m">{{ m }}</option>
            </b-select>
          </b-field>
          <b-field label="Technique" class="column is-4">
            <b-select v-model="angler.souvenir.techniqueId" expanded>
              <option v-for="t in techniques" :key="t.id" :value="t.id">{{ t.name }}</option>
            </b-select>
          </b-field>
          <b-field label="Nombre de lignes" class="column is-4">
            <b-input type="number" min="1" v-model.number="angler.souvenir.rodCount"></b-input>
          </b-field>

          <b-field label="Appât / type de leurre" class="column is-6">
            <b-input v-model="angler.souvenir.baitOrLure"></b-input>
          </b-field>
          <b-field label="Espèce recherchée" class="column is-6">
            <b-select v-model="angler.souvenir.expectedSpeciesId" expanded
              :disabled="angler.souvenir.noExpectedSpecies">
              <option v-for="s in species" :key="s.id" :value="s.id">{{ s.name }}</option>
            </b-select>
          </b-field>
          <b-field class="column is-12 no-species-field">
            <b-switch v-model="angler.souvenir.noExpectedSpecies">Aucune espèce recherchée / toutes espèces</b-switch>
          </b-field>

          <b-field class="column is-12">
            <b-switch v-model="angler.souvenir.bredouille">Bredouille (aucune capture)</b-switch>
          </b-field>
        </div>

        <div v-if="!angler.souvenir.bredouille" class="box capture-box">
          <div class="columns is-multiline is-vcentered">
            <b-field label="Espèce" class="column is-3">
              <b-select v-model="angler.souvenir.capture.speciesId" expanded>
                <option v-for="s in species" :key="s.id" :value="s.id">{{ s.name }}</option>
              </b-select>
            </b-field>
            <b-field label="Nombre" class="column is-2">
              <b-input type="number" min="1" v-model.number="angler.souvenir.capture.quantity"></b-input>
            </b-field>

            <template v-if="isLot(angler.souvenir.capture)">
              <b-field label="Taille min (cm)" class="column is-2">
                <b-input type="number" min="0" v-model.number="angler.souvenir.capture.lotMinSize"></b-input>
              </b-field>
              <b-field label="Taille max (cm)" class="column is-2">
                <b-input type="number" min="0" v-model.number="angler.souvenir.capture.lotMaxSize"></b-input>
              </b-field>
            </template>
            <b-field v-else label="Taille (cm)" class="column is-2">
              <b-input type="number" min="0" v-model.number="angler.souvenir.capture.size"></b-input>
            </b-field>

            <b-field label="Conservé" class="column is-2">
              <b-switch v-model="angler.souvenir.capture.kept"></b-switch>
            </b-field>
          </div>
        </div>
      </div>
    </div>
    <b-button type="is-light" icon-left="plus" @click="addAngler">Ajouter un pêcheur interrogé</b-button>

    <div class="mt-5">
      <b-button type="is-primary" icon-left="content-save" :loading="loading" @click="submit">
        Enregistrer la sortie enquêtée
      </b-button>
    </div>

    <b-notification v-if="success" type="is-success" class="mt-4" @close="success = null">
      Sortie enregistrée : {{ success.tripIds.length }} sortie(s) créée(s), {{ success.captures }} capture(s).
    </b-notification>

    <b-notification v-if="errors.length" type="is-danger" class="mt-4" :closable="true" @close="errors = []">
      <p><strong>Saisie non enregistrée :</strong></p>
      <ul>
        <li v-for="(e, i) in errors" :key="i">
          <span v-if="e.index != null">Pêcheur {{ e.index + 1 }} — </span>
          <span v-if="e.field">[{{ e.field }}] </span>{{ e.message }}
        </li>
      </ul>
    </b-notification>
  </section>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import { maskTimeInput, isValidTimeString } from "@/utils/utils";
import { reactive, ref, computed } from "vue";

const TIME_FORMAT_ERROR = "Heure invalide (format 24h HH:mm, ex. 13:45)";

// Référentiel fermé partagé avec le carnet volontaire (SurveySchema.MODES_PECHE, #145).
const FISHING_MODES = ["bateau", "float tube/canoë", "bord itinérant", "bord statique"];
const DAY_PERIODS = ["matin", "après-midi", "journée entière", "soirée"];

const waterEntities = ref<any[]>([]);
const techniques = ref<any[]>([]);
const species = ref<any[]>([]);

function newSortie() {
  return {
    day: "",
    controlTime: "",
    startTime: "",
    endTime: "",
    waterEntityId: null,
    unsurveyedShoreAnglers: null,
    unsurveyedBoatAnglers: null
  };
}

function newCapture() {
  return { speciesId: null, quantity: 1, size: null, lotMinSize: null, lotMaxSize: null, kept: false };
}

function newSouvenir() {
  return {
    day: "",
    dayPeriod: DAY_PERIODS[0],
    waterEntityId: null,
    fishingMode: FISHING_MODES[0],
    techniqueId: null,
    rodCount: 1,
    baitOrLure: "",
    expectedSpeciesId: null,
    noExpectedSpecies: false,
    bredouille: false,
    capture: newCapture()
  };
}

function newAngler() {
  return {
    origin: "",
    fishingMode: FISHING_MODES[0],
    techniqueId: null,
    rodCount: 1,
    baitOrLure: "",
    expectedSpeciesId: null,
    noExpectedSpecies: false,
    bredouille: false,
    captures: [newCapture()],
    hasSouvenir: false,
    souvenir: newSouvenir()
  };
}

const sortie = ref<any>(newSortie());
const anglers = ref<any[]>([newAngler()]);

const loading = ref(false);
const success = ref<any>(null);
const errors = ref<any[]>([]);

const timeErrors = reactive({ controlTime: "", startTime: "", endTime: "" });

const todayIso = computed(() => new Date().toISOString().slice(0, 10));

function isLot(c: any): boolean {
  return Number(c.quantity) > 1;
}

function onTimeInput(event: Event, field: "controlTime" | "startTime" | "endTime") {
  const raw = (event.target as HTMLInputElement).value;
  const masked = maskTimeInput(raw);
  sortie.value[field] = masked;

  if (!masked || isValidTimeString(masked)) {
    timeErrors[field] = "";
  } else if (masked.length === 5) {
    timeErrors[field] = TIME_FORMAT_ERROR;
  }
}

function onTimeBlur(field: "controlTime" | "startTime" | "endTime") {
  const value = sortie.value[field];
  timeErrors[field] = !value || isValidTimeString(value) ? "" : TIME_FORMAT_ERROR;
}

loadReferentials();

async function loadReferentials() {
  waterEntities.value = await BackendService.backendGet("/v1/referential/waterEntities");
  techniques.value = await BackendService.backendGet("/v1/referential/techniques");
  species.value = await BackendService.backendGet("/v1/referential/species");
}

function addAngler() {
  anglers.value.push(newAngler());
}

function removeAngler(i: number) {
  anglers.value.splice(i, 1);
}

function addCapture(angler: any) {
  angler.captures.push(newCapture());
}

function removeCapture(angler: any, j: number) {
  angler.captures.splice(j, 1);
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
    kept: !!c.kept
  };
}

function cleanSouvenir(angler: any) {
  if (!angler.hasSouvenir) return null;
  const s = angler.souvenir;
  return {
    day: s.day || null,
    dayPeriod: s.dayPeriod,
    waterEntityId: s.waterEntityId,
    fishingMode: s.fishingMode,
    techniqueId: s.techniqueId,
    rodCount: toIntOrNull(s.rodCount),
    baitOrLure: s.baitOrLure || null,
    expectedSpeciesId: s.noExpectedSpecies ? null : s.expectedSpeciesId,
    noExpectedSpecies: !!s.noExpectedSpecies,
    bredouille: !!s.bredouille,
    capture: s.bredouille ? null : cleanCapture(s.capture)
  };
}

function cleanAngler(angler: any) {
  return {
    origin: angler.origin || null,
    fishingMode: angler.fishingMode,
    techniqueId: angler.techniqueId,
    rodCount: toIntOrNull(angler.rodCount),
    baitOrLure: angler.baitOrLure || null,
    expectedSpeciesId: angler.noExpectedSpecies ? null : angler.expectedSpeciesId,
    noExpectedSpecies: !!angler.noExpectedSpecies,
    bredouille: !!angler.bredouille,
    captures: angler.bredouille ? [] : angler.captures.map(cleanCapture),
    souvenir: cleanSouvenir(angler)
  };
}

async function submit() {
  loading.value = true;
  success.value = null;
  errors.value = [];
  const payload = {
    waterEntityId: sortie.value.waterEntityId,
    secteur: null,
    day: sortie.value.day || null,
    controlTime: sortie.value.controlTime || null,
    startTime: sortie.value.startTime || null,
    endTime: sortie.value.endTime || null,
    unsurveyedShoreAnglers: toIntOrNull(sortie.value.unsurveyedShoreAnglers),
    unsurveyedBoatAnglers: toIntOrNull(sortie.value.unsurveyedBoatAnglers),
    anglers: anglers.value.map(cleanAngler)
  };
  try {
    const result = await BackendService.backendPost("/v1/admin/manual-entries/survey", payload);
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
  sortie.value = newSortie();
  anglers.value = [newAngler()];
  timeErrors.controlTime = "";
  timeErrors.startTime = "";
  timeErrors.endTime = "";
}
</script>

<style scoped lang="less">
.no-species-field {
  display: flex;
  align-items: center;
}
.angler-box {
  border: 1px solid #e2e8ec;
}
.angler-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.capture-title {
  font-weight: 600;
  margin-bottom: 0.5rem;
}
.capture-box {
  box-shadow: none;
  border: 1px solid #eef1f4;
}
.souvenir-toggle {
  margin-top: 1rem;
  margin-bottom: 0.5rem;
}
.souvenir-box {
  box-shadow: none;
  border: 1px dashed #cbd5db;
}
</style>
