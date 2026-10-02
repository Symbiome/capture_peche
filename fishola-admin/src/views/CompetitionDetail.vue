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
  <div class="competition-detail">
    <button class="button" type="button" @click="goBack">← Retour aux concours</button>
    <div v-if="competition">
      <h1>{{ competition.name }}</h1>
      <p class="subtitle">
        {{ formatDate(competition.date) }} — {{ competition.waterEntityName }} — {{ competition.federationName }}
      </p>

      <div class="box">
        <h2>Associer un pêcheur</h2>
        <b-field label="Rechercher un pêcheur (pseudo, nom, email)">
          <b-autocomplete
            v-model="search"
            :data="suggestions"
            :loading="searching"
            field="pseudo"
            icon="magnify"
            clearable
            placeholder="Au moins 2 caractères..."
            @typing="onTyping"
            @select="onSelect"
          >
            <template #empty>Aucun résultat</template>
            <template #default="props">
              {{ props.option.pseudo }} — {{ props.option.firstName }} {{ props.option.lastName }} ({{ props.option.email }})
            </template>
          </b-autocomplete>
        </b-field>
        <b-button type="is-primary" :disabled="!selectedUser" :loading="attributing" @click="attribute">
          Attribuer le badge concours
        </b-button>
      </div>

      <h2>Participants ({{ participants.length }})</h2>
      <b-table :data="participants" :striped="true" :loading="loadingParticipants">
        <b-table-column field="pseudo" label="Pseudo" sortable v-slot="props">
          {{ props.row.pseudo }}
        </b-table-column>
        <b-table-column field="firstName" label="Prénom" sortable v-slot="props">
          {{ props.row.firstName }}
        </b-table-column>
        <b-table-column field="lastName" label="Nom" sortable v-slot="props">
          {{ props.row.lastName }}
        </b-table-column>
        <b-table-column field="email" label="E-mail" sortable v-slot="props">
          {{ props.row.email }}
        </b-table-column>
      </b-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import router from "@/router";

import { BAutocomplete, BButton, BField, BTable, BTableColumn, useToast } from "buefy";
import { onMounted, ref } from "vue";

const Toast = useToast();
const { id: competitionId } = defineProps<{ id: string }>();

function goBack() {
  router.push("/competitions");
}

interface CompetitionBean {
  id: string;
  name: string;
  date: [number, number, number];
  waterEntityId: string;
  waterEntityName: string;
  federationName: string;
}

interface ParticipantRow {
  userId: string;
  pseudo: string;
  firstName: string;
  lastName: string;
  email: string;
}

interface UserSearchResult {
  id: string;
  pseudo: string;
  firstName: string;
  lastName: string;
  email: string;
}

const competition = ref<CompetitionBean | null>(null);
const participants = ref<ParticipantRow[]>([]);
const loadingParticipants = ref(true);

const search = ref("");
const suggestions = ref<UserSearchResult[]>([]);
const searching = ref(false);
const selectedUser = ref<UserSearchResult | null>(null);
const attributing = ref(false);

let searchTimer: ReturnType<typeof setTimeout> | null = null;
let searchSeq = 0;

onMounted(async () => {
  competition.value = await BackendService.backendGet("/v1/admin/competitions/" + competitionId);
  await loadParticipants();
});

async function loadParticipants() {
  loadingParticipants.value = true;
  participants.value = await BackendService.backendGet("/v1/admin/competitions/" + competitionId + "/participants");
  loadingParticipants.value = false;
}

function formatDate(dateArray: [number, number, number]): string {
  if (!dateArray) {
    return "";
  }
  const [year, month, day] = dateArray;
  return new Date(year, month - 1, day).toLocaleDateString("fr-FR");
}

function onTyping(term: string) {
  selectedUser.value = null;
  if (searchTimer) {
    clearTimeout(searchTimer);
  }
  const trimmed = (term || "").trim();
  if (trimmed.length < 2) {
    suggestions.value = [];
    searching.value = false;
    return;
  }
  searching.value = true;
  searchTimer = setTimeout(async () => {
    const seq = ++searchSeq;
    try {
      const results = await BackendService.backendGet(
        "/v1/admin/competitions/search-users?q=" + encodeURIComponent(trimmed)
      );
      if (seq === searchSeq) {
        suggestions.value = results;
      }
    } catch {
      if (seq === searchSeq) {
        suggestions.value = [];
      }
    } finally {
      if (seq === searchSeq) {
        searching.value = false;
      }
    }
  }, 250);
}

function onSelect(option: UserSearchResult | null) {
  selectedUser.value = option;
}

async function attribute() {
  if (!selectedUser.value) {
    return;
  }
  attributing.value = true;
  try {
    await BackendService.backendPost("/v1/admin/competitions/" + competitionId + "/attribute", {
      userId: selectedUser.value.id,
    });
    Toast.open({ message: "Badge attribué à " + selectedUser.value.pseudo, type: "is-success" });
    search.value = "";
    suggestions.value = [];
    selectedUser.value = null;
    await loadParticipants();
  } catch (error: any) {
    Toast.open({ message: "Impossible d'attribuer le badge : " + error.message, type: "is-danger" });
  } finally {
    attributing.value = false;
  }
}
</script>

<style scoped lang="less">
.competition-detail {
  .subtitle {
    color: #7a7a7a;
    margin-bottom: 20px;
  }

  .box {
    max-width: 480px;
    margin-bottom: 30px;
  }

  h2 {
    margin-bottom: 15px;
  }
}
</style>
