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
    <template v-if="openSessions.length">
      <h2 class="subtitle is-5">Sessions ouvertes</h2>
      <b-table :data="openSessions" class="mb-5">
        <b-table-column v-slot="{ row }" label="Date">{{ frenchDate(row.day) }}</b-table-column>
        <b-table-column v-slot="{ row }" label="Début">{{ row.startTime }}</b-table-column>
        <b-table-column v-slot="{ row }" label="Secteur">{{ row.waterEntityName }}</b-table-column>
        <b-table-column v-slot="{ row }" label="Opérateur">{{ row.operatorName }}</b-table-column>
        <b-table-column v-slot="{ row }" label="Sorties">{{ row.sortiesCount }}</b-table-column>
        <b-table-column v-slot="{ row }">
          <b-button type="is-primary" size="is-small" outlined icon-left="play" @click="emit('selected', row)">
            Reprendre
          </b-button>
        </b-table-column>
      </b-table>
    </template>

    <h2 class="subtitle is-5">Nouvelle session d'enquête</h2>
    <div class="columns is-multiline">
      <b-field label="Date" class="column is-2">
        <input type="date" class="input" v-model="session.day" :max="todayIso" />
      </b-field>
      <TimeField v-model="session.startTime" label="Heure de début de session" class="column is-2" />
      <b-field label="Secteur" class="column is-4">
        <WaterEntitySearchSelect v-model="session.waterEntityId" with-favorites />
      </b-field>
      <b-field label="Nom de l'opérateur" class="column is-4">
        <b-input v-model="session.operatorName"></b-input>
      </b-field>
    </div>
    <b-button type="is-primary" icon-left="play" :loading="loading" :disabled="!canOpen" @click="open">
      Ouvrir la session
    </b-button>

    <b-notification v-if="errors.length" type="is-danger" class="mt-4" :closable="true" @close="errors = []">
      <p><strong>Session non ouverte :</strong></p>
      <ul>
        <li v-for="(e, i) in errors" :key="i"><span v-if="e.field">[{{ e.field }}] </span>{{ e.message }}</li>
      </ul>
    </b-notification>
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import TimeField from "@/components/TimeField.vue";
import WaterEntitySearchSelect from "@/components/WaterEntitySearchSelect.vue";
import { SURVEY_SESSIONS_URI, SurveyError, SurveySession, errorsOf, frenchDate } from "@/components/survey/surveyForm";
import { isValidTimeString, localDateIso, localTimeHHmm } from "@/utils/utils";
import { computed, onMounted, ref } from "vue";

const emit = defineEmits<{
  (e: "selected", session: SurveySession): void;
}>();

const todayIso = computed(() => localDateIso());

// Date et heure de début = celles de l'appareil (#234), opérateur = compte connecté (#236),
// tous modifiables (saisie a posteriori d'une enquête papier, enquêteur ≠ saisisseur).
const session = ref({
  day: localDateIso(),
  startTime: localTimeHHmm(new Date()),
  waterEntityId: null as string | null,
  operatorName: ""
});

const openSessions = ref<SurveySession[]>([]);
const loading = ref(false);
const errors = ref<SurveyError[]>([]);

const canOpen = computed(() => !!session.value.day && isValidTimeString(session.value.startTime)
  && !!session.value.waterEntityId && !!session.value.operatorName.trim());

onMounted(async () => {
  openSessions.value = await BackendService.backendGet(SURVEY_SESSIONS_URI);
  const admin = await BackendService.backendGet("/v1/admin/check");
  if (!session.value.operatorName) {
    session.value.operatorName = admin.email;
  }
});

/** Ouvre la session puis la sélectionne pour y saisir des sorties. */
async function open() {
  loading.value = true;
  errors.value = [];
  try {
    const result = await BackendService.backendPost(SURVEY_SESSIONS_URI, session.value);
    emit("selected", result.session);
  } catch (rejected: any) {
    errors.value = errorsOf(rejected);
  } finally {
    loading.value = false;
  }
}
</script>
