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
  <div>
    <h2 class="subtitle is-5">Nouvelle sortie</h2>
    <div class="columns is-multiline">
      <b-field label="Date" class="column is-2">
        <input type="date" class="input" v-model="sortie.day" :max="todayIso" />
      </b-field>
      <TimeField v-model="sortie.controlTime" label="Heure du contrôle" class="column is-2"
        :error="controlTimeError" />
      <TimeField v-model="sortie.startTime" label="Heure de début" class="column is-2" />
      <b-field label="Date de fin prévue" class="column is-2">
        <input type="date" class="input" v-model="sortie.endDay" :min="sortie.day" />
      </b-field>
      <TimeField v-model="sortie.endTime" label="Heure de fin prévue" class="column is-2" :error="endTimeError" />
      <b-field label="Secteur" class="column is-4">
        <WaterEntitySearchSelect v-model="sortie.waterEntityId" v-model:position="sortie.position"
          :initial-label="session.waterEntityName" with-map with-favorites />
      </b-field>
    </div>

    <h2 class="subtitle is-5">Pêcheurs interrogés</h2>
    <SurveyAnglerForm v-for="(angler, i) in anglers" :key="i" :angler="angler" :index="i"
      :removable="anglers.length > 1" :species="species" :techniques="techniques" :today-iso="todayIso"
      @remove="anglers.splice(i, 1)" />
    <b-button type="is-light" icon-left="plus" @click="anglers.push(newAngler())">Ajouter un pêcheur interrogé</b-button>

    <div class="mt-5">
      <b-button type="is-primary" icon-left="content-save" :loading="loading" :disabled="!timesValid"
        @click="submit">
        Enregistrer la sortie
      </b-button>
    </div>

    <b-notification v-if="errors.length" type="is-danger" class="mt-4" :closable="true" @close="errors = []">
      <p><strong>Sortie non enregistrée :</strong></p>
      <ul>
        <li v-for="(e, i) in errors" :key="i">
          <span v-if="e.index != null">Pêcheur {{ e.index + 1 }} — </span>
          <span v-if="e.field">[{{ e.field }}] </span>{{ e.message }}
        </li>
      </ul>
    </b-notification>
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import SurveyAnglerForm from "@/components/survey/SurveyAnglerForm.vue";
import TimeField from "@/components/TimeField.vue";
import WaterEntitySearchSelect from "@/components/WaterEntitySearchSelect.vue";
import type { MapPosition } from "@/components/WaterEntityMapPicker.vue";
import {
  SURVEY_SESSIONS_URI, SurveyError, SurveySession, cleanAngler, errorsOf, newAngler
} from "@/components/survey/surveyForm";
import type { EntryReferential } from "@/utils/entryReferentials";
import { isEndAfterStart, isValidTimeString, localDateIso, localTimeHHmm } from "@/utils/utils";
import { computed, ref, watch } from "vue";

const CONTROL_BEFORE_START_ERROR = "L'heure du contrôle ne peut pas être antérieure à l'heure de début";
const CONTROL_AFTER_END_ERROR = "L'heure du contrôle ne peut pas être postérieure à la fin prévue";
const END_NOT_AFTER_START_ERROR = "La fin doit être postérieure au début";

interface Props {
  session: SurveySession;
  species: EntryReferential;
  techniques: EntryReferential;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: "saved", result: { tripIds: string[]; captures: number }): void;
}>();

const todayIso = computed(() => localDateIso());

/** Sortie pré-renseignée depuis la session (#236) : date, secteur, heure du contrôle = heure de l'appareil. */
function newSortie() {
  return {
    day: props.session.day,
    controlTime: localTimeHHmm(new Date()),
    startTime: "",
    // Date de fin prévue (#237) : carpiste contrôlé pendant une session de plusieurs jours.
    endDay: props.session.day,
    endTime: "",
    waterEntityId: props.session.waterEntityId as string | null,
    position: null as MapPosition | null
  };
}

const sortie = ref(newSortie());
const anglers = ref<any[]>([newAngler()]);
const loading = ref(false);
const errors = ref<SurveyError[]>([]);

function areValidTimes(...values: string[]): boolean {
  return values.every((value) => !!value && isValidTimeString(value));
}

// Heure du contrôle comprise entre le début et la fin prévue, date de fin comprise (#236, #237),
// même règle que le backend.
const controlTimeError = computed(() => {
  const { day, controlTime, startTime, endDay, endTime } = sortie.value;
  if (areValidTimes(controlTime, startTime) && controlTime < startTime) {
    return CONTROL_BEFORE_START_ERROR;
  }
  if (areValidTimes(controlTime) && isEndAfterStart(day, startTime, endDay, endTime)
      && `${day}T${controlTime}` > `${endDay || day}T${endTime}`) {
    return CONTROL_AFTER_END_ERROR;
  }
  return "";
});

const endTimeError = computed(() => {
  const { day, startTime, endDay, endTime } = sortie.value;
  return isEndAfterStart(day, startTime, endDay, endTime) === false ? END_NOT_AFTER_START_ERROR : "";
});

// Date de fin prévue pré-remplie avec la date (#237), qu'elle suit tant qu'elle lui est égale.
watch(() => sortie.value.day, (newDay: string, oldDay: string) => {
  if (!sortie.value.endDay || sortie.value.endDay === oldDay) {
    sortie.value.endDay = newDay;
  }
});

const timesValid = computed(() => {
  const { controlTime, startTime, endTime } = sortie.value;
  return areValidTimes(controlTime, startTime, endTime) && !controlTimeError.value && !endTimeError.value;
});

/** Enregistre la sortie dans la session, puis repart d'un formulaire pré-renseigné. */
async function submit() {
  loading.value = true;
  errors.value = [];
  const payload = {
    waterEntityId: sortie.value.waterEntityId,
    latitude: sortie.value.position ? sortie.value.position.lat : null,
    longitude: sortie.value.position ? sortie.value.position.lng : null,
    day: sortie.value.day || null,
    controlTime: sortie.value.controlTime,
    startTime: sortie.value.startTime,
    endDay: sortie.value.endDay || null,
    endTime: sortie.value.endTime,
    anglers: anglers.value.map(cleanAngler)
  };
  try {
    const result = await BackendService.backendPost(SURVEY_SESSIONS_URI + "/" + props.session.id + "/sorties",
      payload);
    sortie.value = newSortie();
    anglers.value = [newAngler()];
    emit("saved", result);
  } catch (rejected: any) {
    errors.value = errorsOf(rejected);
  } finally {
    loading.value = false;
  }
}
</script>
