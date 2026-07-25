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
    <h1 class="title">Import CSV opérateur</h1>
    <p class="subtitle is-6">
      Fichier au format officiel (28 colonnes, séparateur « ; », UTF-8).
    </p>

    <b-field>
      <b-upload v-model="file" drag-drop expanded accept=".csv,text/csv">
        <section class="ex-drop">
          <p><b-icon icon="upload" size="is-large"></b-icon></p>
          <p>Déposez un fichier CSV ou cliquez pour en choisir un</p>
        </section>
      </b-upload>
    </b-field>

    <div v-if="file" class="mb-3">
      <b-tag type="is-primary" closable @close="reset">{{ file.name }}</b-tag>
    </div>

    <b-button
      type="is-primary"
      icon-left="database-import"
      :disabled="!file || loading"
      :loading="loading"
      @click="doImport"
    >
      Importer
    </b-button>

    <b-notification
      v-if="result"
      :type="notificationType"
      :closable="true"
      class="mt-4"
      @close="result = null"
    >
      <p v-if="result.duplicate">
        Fichier déjà importé (identique à un import précédent) — aucune réécriture.
      </p>
      <p>
        Statut : <strong>{{ result.status }}</strong> —
        {{ result.total }} ligne(s), {{ result.inserted }} sortie(s) créée(s),
        {{ result.rejected }} ligne(s) rejetée(s).
      </p>
    </b-notification>

    <div v-if="result && result.errors && result.errors.length" class="mt-4">
      <h2 class="subtitle is-5">Rapport d'erreurs</h2>
      <b-table :data="result.errors" :paginated="result.errors.length > 15" per-page="15">
        <b-table-column field="line" label="Ligne" width="80" v-slot="props">
          {{ props.row.line }}
        </b-table-column>
        <b-table-column field="column" label="Colonne" v-slot="props">
          {{ props.row.column || "—" }}
        </b-table-column>
        <b-table-column field="stage" label="Étage" v-slot="props">
          {{ props.row.stage }}
        </b-table-column>
        <b-table-column field="code" label="Code" v-slot="props">
          {{ props.row.code }}
        </b-table-column>
        <b-table-column field="message" label="Message" v-slot="props">
          {{ props.row.message }}
        </b-table-column>
      </b-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import { ref, computed } from "vue";

const file = ref<File | null>(null);
const loading = ref(false);
const result = ref<any>(null);

const notificationType = computed(() => {
  if (!result.value) return "is-info";
  if (result.value.duplicate) return "is-warning";
  if (result.value.status === "FAILED") return "is-danger";
  if (result.value.status === "DONE_WITH_ERRORS") return "is-warning";
  return "is-success";
});

function reset() {
  file.value = null;
  result.value = null;
}

async function doImport() {
  if (!file.value) return;
  loading.value = true;
  result.value = null;
  try {
    const buffer = await file.value.arrayBuffer();
    const uri = `/v1/admin/imports?filename=${encodeURIComponent(file.value.name)}&mode=partial`;
    result.value = await BackendService.backendPostBinary(uri, buffer);
  } catch (err) {
    result.value = {
      status: "FAILED",
      total: 0,
      inserted: 0,
      rejected: 0,
      duplicate: false,
      errors: [{ line: 0, column: null, stage: "-", code: "HTTP",
        message: "Échec de l'import (voir la console / réessayer)." }]
    };
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped lang="less">
.ex-drop {
  padding: 2rem;
  text-align: center;
}
</style>
