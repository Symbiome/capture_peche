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
      Session d'enquête → sortie(s) → pêcheur(s) interrogé(s) → captures, avec bloc « session souvenir »
      facultatif par pêcheur. Les codes session / sortie / pêcheur sont générés automatiquement.
    </p>

    <b-notification v-if="closedSession" type="is-success" @close="closedSession = null">
      Session du {{ frenchDate(closedSession.day) }} clôturée : {{ closedSession.sortiesCount }} sortie(s),
      {{ closedSession.anglersCount }} pêcheur(s) interrogé(s).
    </b-notification>

    <SurveySessionPicker v-if="!session" @selected="selectSession" />

    <template v-else>
      <div class="box session-recap">
        <div>
          <strong>Session en cours</strong> — {{ frenchDate(session.day) }}, début {{ session.startTime }} ·
          {{ session.waterEntityName }} · opérateur : {{ session.operatorName }}
          <br />
          <small>{{ session.sortiesCount }} sortie(s), {{ session.anglersCount }} pêcheur(s) interrogé(s)</small>
        </div>
        <div class="buttons">
          <b-button size="is-small" icon-left="swap-horizontal" @click="session = null">Changer de session</b-button>
          <b-button size="is-small" type="is-warning" icon-left="lock" @click="closing = true">
            Clôturer la session
          </b-button>
        </div>
      </div>

      <SurveySessionClose v-if="closing" :session="session" @closed="onClosed" @cancel="closing = false" />

      <b-notification v-if="lastSaved" type="is-success" class="mt-4" @close="lastSaved = null">
        Sortie enregistrée : {{ lastSaved.tripIds.length }} sortie(s) créée(s), {{ lastSaved.captures }} capture(s).
      </b-notification>

      <SurveySortieForm v-if="!closing" :key="session.id" :session="session" :species="species"
        :techniques="techniques" @saved="onSaved" />
    </template>
  </section>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import SurveySessionClose from "@/components/survey/SurveySessionClose.vue";
import SurveySessionPicker from "@/components/survey/SurveySessionPicker.vue";
import SurveySortieForm from "@/components/survey/SurveySortieForm.vue";
import { SURVEY_SESSIONS_URI, SurveySession, frenchDate } from "@/components/survey/surveyForm";
import { ref } from "vue";

const techniques = ref<any[]>([]);
const species = ref<any[]>([]);

const session = ref<SurveySession | null>(null);
const closing = ref(false);
const lastSaved = ref<{ tripIds: string[]; captures: number } | null>(null);
const closedSession = ref<SurveySession | null>(null);

loadReferentials();

/** Techniques et espèces proposées à la saisie, hors éléments archivés (#202). */
async function loadReferentials() {
  const allTechniques = await BackendService.backendGet("/v1/referential/techniques");
  const allSpecies = await BackendService.backendGet("/v1/referential/species");
  techniques.value = allTechniques.filter((t: any) => !t.archived);
  species.value = allSpecies.filter((s: any) => !s.archived);
}

/** Session ouverte ou reprise : on y saisit les sorties. */
function selectSession(selected: SurveySession) {
  session.value = selected;
  closing.value = false;
  lastSaved.value = null;
  closedSession.value = null;
}

/** Sortie enregistrée : rafraîchit les compteurs de la session. */
async function onSaved(result: { tripIds: string[]; captures: number }) {
  lastSaved.value = result;
  if (session.value) {
    session.value = await BackendService.backendGet(SURVEY_SESSIONS_URI + "/" + session.value.id);
  }
}

/** Session clôturée : retour à la liste des sessions. */
function onClosed(closed: SurveySession) {
  closedSession.value = closed;
  session.value = null;
  closing.value = false;
  lastSaved.value = null;
}
</script>

<style scoped lang="less">
.session-recap {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  border: 1px solid #e2e8ec;
}
</style>
