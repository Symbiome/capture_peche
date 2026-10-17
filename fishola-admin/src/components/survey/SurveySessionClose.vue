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
  <div class="box close-box">
    <h2 class="subtitle is-5">Clôture de la session</h2>
    <div class="columns is-multiline">
      <TimeField v-model="close.endTime" label="Heure de fin de session" class="column is-3" :error="endTimeError" />
      <b-field label="Pêcheurs du bord non-enquêtés" class="column is-4">
        <b-input type="number" min="0" v-model.number="close.unsurveyedShoreAnglers"></b-input>
      </b-field>
      <b-field label="Pêcheurs en bateau non-enquêtés" class="column is-4">
        <b-input type="number" min="0" v-model.number="close.unsurveyedBoatAnglers"></b-input>
      </b-field>
    </div>
    <div class="buttons">
      <b-button type="is-primary" icon-left="lock" :loading="loading" :disabled="!canClose" @click="submit">
        Clôturer la session
      </b-button>
      <b-button @click="emit('cancel')">Annuler</b-button>
    </div>

    <b-notification v-if="errors.length" type="is-danger" class="mt-4" :closable="true" @close="errors = []">
      <p><strong>Session non clôturée :</strong></p>
      <ul>
        <li v-for="(e, i) in errors" :key="i"><span v-if="e.field">[{{ e.field }}] </span>{{ e.message }}</li>
      </ul>
    </b-notification>
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import TimeField from "@/components/TimeField.vue";
import { SURVEY_SESSIONS_URI, SurveyError, SurveySession, errorsOf, toIntOrNull } from "@/components/survey/surveyForm";
import { isValidTimeString, localTimeHHmm } from "@/utils/utils";
import { computed, ref } from "vue";

const END_NOT_AFTER_START_ERROR = "L'heure de fin doit être postérieure à l'heure de début de session";

interface Props {
  session: SurveySession;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: "closed", session: SurveySession): void;
  (e: "cancel"): void;
}>();

const close = ref({
  endTime: localTimeHHmm(new Date()),
  unsurveyedShoreAnglers: null as number | null,
  unsurveyedBoatAnglers: null as number | null
});
const loading = ref(false);
const errors = ref<SurveyError[]>([]);

const endTimeError = computed(() => {
  const { startTime } = props.session;
  const { endTime } = close.value;
  return startTime && isValidTimeString(endTime) && endTime <= startTime ? END_NOT_AFTER_START_ERROR : "";
});

const canClose = computed(() => isValidTimeString(close.value.endTime) && !endTimeError.value);

/** Clôture la session avec l'heure de fin et les pêcheurs observés non enquêtés. */
async function submit() {
  loading.value = true;
  errors.value = [];
  try {
    const result = await BackendService.backendPost(SURVEY_SESSIONS_URI + "/" + props.session.id + "/close", {
      endTime: close.value.endTime,
      unsurveyedShoreAnglers: toIntOrNull(close.value.unsurveyedShoreAnglers),
      unsurveyedBoatAnglers: toIntOrNull(close.value.unsurveyedBoatAnglers)
    });
    emit("closed", result.session);
  } catch (rejected: any) {
    errors.value = errorsOf(rejected);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped lang="less">
.close-box {
  border: 1px solid #e2e8ec;
}
</style>
