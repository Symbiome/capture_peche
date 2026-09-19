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
  <div class="competitions">
    <h1>Concours de pêche</h1>
    <b-message type="is-info">
      Créez un concours puis ouvrez-le pour y associer les pêcheurs participants : ils
      débloqueront le badge « Participation à un concours », avec le nom et la date du
      concours affichés sur leur profil (#90).
    </b-message>

    <div class="box">
      <h2>Nouveau concours</h2>
      <b-field label="Nom du concours" :type="errors.name ? 'is-danger' : ''" :message="errors.name">
        <b-input v-model="form.name" placeholder="Ex. Concours de la Truite du Léman" />
      </b-field>
      <b-field label="Date" :type="errors.date ? 'is-danger' : ''" :message="errors.date">
        <b-datepicker v-model="form.date" locale="fr-FR" icon="calendar-today" editable placeholder="Sélectionnez une date" />
      </b-field>
      <b-field label="Plan d'eau ou cours d'eau" :type="errors.waterEntityId ? 'is-danger' : ''" :message="errors.waterEntityId">
        <WaterEntitySearchSelect @update:modelValue="onWaterEntitySelected" />
      </b-field>
      <b-field label="Fédération organisatrice" :type="errors.federationName ? 'is-danger' : ''" :message="errors.federationName">
        <b-input v-model="form.federationName" placeholder="Ex. Fédération de pêche de Haute-Savoie" />
      </b-field>
      <b-button type="is-primary" :loading="creating" @click="createCompetition">Créer le concours</b-button>
    </div>

    <b-table
      :data="competitions"
      :striped="true"
      :loading="loading"
      class="clickable"
      @click="openCompetition"
    >
      <b-table-column field="name" label="Nom" sortable v-slot="props">
        {{ props.row.name }}
      </b-table-column>
      <!-- Non triable : `date` est un array [y,m,d] (cf. CompetitionRow), pas une valeur
           directement comparable par le tri par défaut de Buefy. -->
      <b-table-column field="date" label="Date" v-slot="props">
        {{ formatDate(props.row.date) }}
      </b-table-column>
      <b-table-column field="waterEntityName" label="Plan d'eau / cours d'eau" sortable v-slot="props">
        {{ props.row.waterEntityName }}
      </b-table-column>
      <b-table-column field="federationName" label="Fédération organisatrice" sortable v-slot="props">
        {{ props.row.federationName }}
      </b-table-column>
    </b-table>
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import WaterEntitySearchSelect from "@/components/WaterEntitySearchSelect.vue";
import router from "@/router";

import { BButton, BDatepicker, BField, BInput, BMessage, useToast } from "buefy";
import { onMounted, reactive, ref } from "vue";

const Toast = useToast();

interface CompetitionRow {
  id: string;
  name: string;
  // LocalDate sérialisé par Jackson (quarkus.jackson.write-dates-as-timestamps=true) comme
  // [année, mois, jour] -- PAS une chaîne ISO, et pas directement compatible avec
  // UtilityServices.formatDate (qui suppose au moins [y,m,d,h,mi], cf. formatDate() ci-dessous).
  date: [number, number, number];
  waterEntityId: string;
  waterEntityName: string;
  federationName: string;
}

const competitions = ref<CompetitionRow[]>([]);
const loading = ref(true);
const creating = ref(false);

const form = reactive<{ name: string; date: Date | null; waterEntityId: string | null; federationName: string }>({
  name: "",
  date: null,
  waterEntityId: null,
  federationName: "",
});
const errors = reactive<{ [key: string]: string }>({});

onMounted(loadCompetitions);

async function loadCompetitions() {
  loading.value = true;
  competitions.value = await BackendService.backendGet("/v1/admin/competitions");
  loading.value = false;
}

function onWaterEntitySelected(id: string | null) {
  form.waterEntityId = id;
}

function formatDate(dateArray: [number, number, number]): string {
  if (!dateArray) {
    return "";
  }
  const [year, month, day] = dateArray;
  return new Date(year, month - 1, day).toLocaleDateString("fr-FR");
}

function validate(): boolean {
  Object.keys(errors).forEach((key) => delete errors[key]);
  if (!form.name || !form.name.trim()) {
    errors.name = "Le nom du concours est obligatoire";
  }
  if (!form.date) {
    errors.date = "La date est obligatoire";
  }
  if (!form.waterEntityId) {
    errors.waterEntityId = "Sélectionnez un plan d'eau ou un cours d'eau";
  }
  if (!form.federationName || !form.federationName.trim()) {
    errors.federationName = "La fédération organisatrice est obligatoire";
  }
  return Object.keys(errors).length === 0;
}

async function createCompetition() {
  if (!validate()) {
    return;
  }
  creating.value = true;
  try {
    await BackendService.backendPost("/v1/admin/competitions", {
      name: form.name,
      date: (form.date as Date).toISOString().substring(0, 10),
      waterEntityId: form.waterEntityId,
      federationName: form.federationName,
    });
    Toast.open({ message: "Concours créé", type: "is-success" });
    form.name = "";
    form.date = null;
    form.waterEntityId = null;
    form.federationName = "";
    await loadCompetitions();
  } catch (error: any) {
    Toast.open({ message: "Impossible de créer le concours : " + error.message, type: "is-danger" });
  } finally {
    creating.value = false;
  }
}

function openCompetition(row: CompetitionRow) {
  router.push("/competitions/" + row.id);
}
</script>

<style scoped lang="less">
.competitions {
  .box {
    max-width: 480px;
    margin-bottom: 30px;
  }

  h2 {
    margin-bottom: 15px;
  }

  .clickable :deep(tbody tr) {
    cursor: pointer;
  }
}
</style>
