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
  <section class="operator-page">
    <header class="page-header">
      <div>
        <h1 class="title">Import CSV opérateur</h1>
        <p class="subtitle is-6">
          Fichier au format officiel : {{ EXPECTED_HEADER.length }} colonnes,
          séparateur « ; ». Les fichiers Excel français (Windows-1252) sont acceptés.
        </p>
      </div>
      <b-button icon-left="file-download-outline" @click="downloadTemplate">
        Télécharger le gabarit
      </b-button>
    </header>

    <div class="box">
      <b-field>
        <b-upload v-model="file" drag-drop expanded accept=".csv,text/csv">
          <section class="ex-drop">
            <p><b-icon icon="upload" size="is-large"></b-icon></p>
            <p class="drop-label">Déposez un fichier CSV ou cliquez pour en choisir un</p>
            <p class="drop-hint">Un même fichier ne peut être importé qu'une fois.</p>
          </section>
        </b-upload>
      </b-field>

      <div class="actions">
        <b-tag v-if="file" type="is-primary" closable @close="reset">{{ file.name }}</b-tag>
        <b-button
          type="is-primary"
          icon-left="database-import"
          :disabled="!file || loading"
          :loading="loading"
          @click="doImport"
        >
          Importer
        </b-button>
      </div>
    </div>

    <template v-if="result">
      <b-notification
        :type="notificationType"
        :closable="true"
        class="mt-4"
        @close="result = null"
      >
        <strong>{{ statusLabel }}</strong>
        <span v-if="result.duplicate">
          — ce fichier est identique à un import précédent, rien n'a été réécrit.
        </span>
      </b-notification>

      <div class="stats">
        <div class="stat">
          <span class="stat-value">{{ result.total }}</span>
          <span class="stat-label">ligne(s) lue(s)</span>
        </div>
        <div class="stat is-ok">
          <span class="stat-value">{{ result.inserted }}</span>
          <span class="stat-label">sortie(s) créée(s)</span>
        </div>
        <div class="stat" :class="{ 'is-ko': result.rejected > 0 }">
          <span class="stat-value">{{ result.rejected }}</span>
          <span class="stat-label">ligne(s) rejetée(s)</span>
        </div>
      </div>
    </template>

    <div v-if="result && result.errors && result.errors.length" class="box mt-4">
      <h2 class="subtitle is-5">Rapport d'erreurs</h2>
      <b-table :data="result.errors" :paginated="result.errors.length > 15" per-page="15" narrowed>
        <b-table-column field="line" label="Ligne" width="80" v-slot="props">
          {{ props.row.line }}
        </b-table-column>
        <b-table-column field="stage" label="Étape" v-slot="props">
          <b-tag :type="stageTagType(props.row.stage)">{{ stageLabel(props.row.stage) }}</b-tag>
        </b-table-column>
        <b-table-column field="column" label="Colonne" v-slot="props">
          <code v-if="props.row.column">{{ props.row.column }}</code>
          <span v-else class="muted">—</span>
        </b-table-column>
        <b-table-column field="message" label="Message" v-slot="props">
          {{ props.row.message }}
        </b-table-column>
        <b-table-column field="code" label="Code" v-slot="props">
          <span class="muted">{{ props.row.code }}</span>
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

// En-tête officiel attendu par le backend (ImportSchema.EXPECTED_HEADER), servant
// aussi à produire le gabarit téléchargeable : les colonnes sont trop nombreuses
// pour être ressaisies de mémoire.
const EXPECTED_HEADER = [
  "session_ref", "collection_method", "date", "heure_debut", "heure_fin",
  "eau_nom", "commune", "mode_peche", "technique", "nb_lignes",
  "espece_ciblee", "bredouille",
  "enquete_age", "enquete_sexe", "enquete_commune", "enquete_experience_annees",
  "enquete_importance", "enquete_membre_club", "enquete_sorties_par_an",
  "capture_espece", "capture_longueur_cm", "capture_poids_g", "capture_conservation",
  "capture_nombre", "capture_classe_taille", "capture_prelevement",
  "capture_marque", "capture_pathologies",
];

const notificationType = computed(() => {
  if (!result.value) return "is-info";
  if (result.value.duplicate) return "is-warning";
  if (result.value.status === "FAILED") return "is-danger";
  if (result.value.status === "DONE_WITH_ERRORS") return "is-warning";
  return "is-success";
});

const statusLabel = computed(() => {
  if (!result.value) return "";
  switch (result.value.status) {
    case "DONE": return "Import réussi";
    case "DONE_WITH_ERRORS": return "Import partiel : certaines lignes ont été rejetées";
    case "FAILED": return "Import échoué";
    default: return result.value.status;
  }
});

function stageLabel(stage: string): string {
  switch (stage) {
    case "structurel": return "Format";
    case "referentiel": return "Référentiel";
    case "metier": return "Règle métier";
    default: return stage;
  }
}

function stageTagType(stage: string): string {
  switch (stage) {
    case "structurel": return "is-danger";
    case "referentiel": return "is-warning";
    case "metier": return "is-info";
    default: return "is-light";
  }
}

/** Gabarit CSV : en-tête officiel + une ligne d'exemple commentée. */
function downloadTemplate() {
  const example = [
    "S001", "carnet_volontaire", "2026-07-01", "08:00", "10:00",
    "Lac du Bourget", "", "du bord", "Pêche au coup", "1",
    "", "false",
    "", "", "", "", "", "", "",
    "Perche", "25", "200", "true",
    "1", "", "false",
    "", "",
  ];
  // BOM UTF-8 : sans lui, Excel (FR) ouvre le fichier en Windows-1252 et casse les accents.
  const csv = "﻿" + EXPECTED_HEADER.join(";") + "\n" + example.join(";") + "\n";
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = "gabarit-import-sessions.csv";
  link.click();
  URL.revokeObjectURL(url);
}

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
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1.5rem;

  .title {
    margin-bottom: 0.25rem;
  }
  .subtitle {
    color: #6b7780;
    max-width: 60ch;
  }
}

.ex-drop {
  padding: 2.5rem 2rem;
  text-align: center;

  .drop-label {
    font-weight: 600;
  }
  .drop-hint {
    color: #8a949c;
    font-size: 0.85rem;
    margin-top: 0.25rem;
  }
}

.actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1rem;
}

.stats {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 1rem;

  .stat {
    flex: 1 1 160px;
    background: #fff;
    border: 1px solid #e2e8ec;
    border-left: 4px solid #c9d3da;
    border-radius: 8px;
    padding: 0.9rem 1.1rem;
    display: flex;
    flex-direction: column;

    &.is-ok {
      border-left-color: #24b47e;
    }
    &.is-ko {
      border-left-color: #e0603a;
    }
  }
  .stat-value {
    font-size: 1.6rem;
    font-weight: 700;
    line-height: 1.1;
  }
  .stat-label {
    color: #6b7780;
    font-size: 0.85rem;
  }
}

.muted {
  color: #a9b2b9;
}

code {
  background: #f2f5f7;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}
</style>
