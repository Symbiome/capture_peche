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
  <section class="operator-page">
    <header class="page-header">
      <div>
        <h1 class="title">Import CSV — Carnet volontaire</h1>
        <p class="subtitle is-6">
          Format dédié : {{ EXPECTED_HEADER.length }} colonnes, séparateur « ; ».
          Les fichiers Excel français (Windows-1252) sont acceptés.
        </p>
      </div>
      <b-button icon-left="file-download-outline" @click="downloadTemplate">
        Télécharger le gabarit
      </b-button>
    </header>

    <b-collapse class="card format-help" animation="slide" :open="false">
      <template #trigger="props">
        <div class="card-header" role="button">
          <p class="card-header-title">Format attendu — {{ EXPECTED_HEADER.length }} colonnes</p>
          <a class="card-header-icon">
            <b-icon :icon="props.open ? 'menu-up' : 'menu-down'"></b-icon>
          </a>
        </div>
      </template>

      <div class="card-content">
        <p class="help-intro">
          Chaque ligne est contrôlée en trois étapes —
          <b-tag type="is-danger">Format</b-tag> (colonnes, dates, heures),
          <b-tag type="is-warning">Référentiel</b-tag> (espèces, techniques, plans d'eau, périmètre) puis
          <b-tag type="is-info">Règle métier</b-tag> (tailles aberrantes, cohérence des lots, marquage).
          Les lignes valides sont importées, les autres listées dans le rapport.
        </p>

        <div v-for="g in COLUMN_GROUPS" :key="g.titre" class="col-group">
          <div class="col-group-head">
            <strong>{{ g.titre }}</strong>
            <span class="col-group-hint">{{ g.aide }}</span>
          </div>
          <div class="col-tags">
            <b-tag
              v-for="c in g.colonnes"
              :key="c.nom"
              :type="c.requis ? 'is-primary' : 'is-light'"
              :title="c.aide"
            >
              {{ c.nom }}<span v-if="c.requis"> *</span>
            </b-tag>
          </div>
        </div>

        <p class="help-note">
          <span class="req">*</span> colonne obligatoire.
          Les captures d'une même sortie partagent le même <code>session_ref</code>.
          Dates au format <code>JJ/MM/AAAA</code>, heures <code>HH:MM</code>.
        </p>

        <div class="coded-values">
          <strong>Valeurs acceptées</strong>
          <div v-for="v in CODED_VALUES" :key="v.colonne" class="coded-line">
            <span class="coded-col">{{ v.colonne }}</span>
            <b-tag v-for="val in v.valeurs" :key="val" type="is-light">{{ val }}</b-tag>
          </div>
        </div>
      </div>
    </b-collapse>

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

// En-tête officiel attendu par le backend (CarnetVolontaireSchema.EXPECTED_HEADER),
// dédié à ce format — distinct du format générique 28 colonnes.
const EXPECTED_HEADER = [
  "session_ref", "eau_nom", "commune", "date", "mode_peche",
  "heure_debut", "heure_fin", "technique_principale", "technique_secondaire",
  "nombre_lignes", "appat_leurre", "espece_recherchee", "observations_diverses", "bredouille",
  "capture_espece", "capture_origine_trf", "capture_technique", "capture_appat_leurre",
  "capture_heure", "capture_taille", "capture_poids", "capture_nombre", "capture_conservee",
  "capture_taille_min", "capture_taille_max", "capture_marque", "capture_marque_numero",
];

const COLUMN_GROUPS = [
  {
    titre: "Sortie",
    aide: "Une ligne par capture ; les captures d'une même sortie partagent le même « session_ref ».",
    colonnes: [
      { nom: "session_ref", aide: "Identifiant de la sortie (regroupe ses captures)", requis: true },
      { nom: "eau_nom", aide: "Secteur pêché — nom du plan ou cours d'eau (référentiel)", requis: true },
      { nom: "commune", aide: "Commune — lève l'ambiguïté entre homonymes", requis: false },
      { nom: "date", aide: "JJ/MM/AAAA", requis: true },
      { nom: "mode_peche", aide: "Bateau, float tube/canoë, bord itinérant, bord statique", requis: true },
      { nom: "heure_debut", aide: "HH:MM", requis: true },
      { nom: "heure_fin", aide: "HH:MM, postérieure à l'heure de début", requis: true },
      { nom: "technique_principale", aide: "Technique de pêche (référentiel)", requis: true },
      { nom: "technique_secondaire", aide: "Technique de pêche (référentiel)", requis: false },
      { nom: "nombre_lignes", aide: "Nombre de lignes en action", requis: true },
      { nom: "appat_leurre", aide: "Pré-rempli sur les captures si non renseigné", requis: false },
      { nom: "espece_recherchee", aide: "Référentiel, ou « Aucune »", requis: true },
      { nom: "observations_diverses", aide: "Cormorans, harle bièvre, pollution…", requis: false },
      { nom: "bredouille", aide: "oui / non — si oui, aucune capture attendue", requis: true },
    ],
  },
  {
    titre: "Capture",
    aide: "Une ligne par poisson (ou par lot). À laisser vide si la sortie est bredouille.",
    colonnes: [
      { nom: "capture_espece", aide: "Espèce (référentiel)", requis: true },
      { nom: "capture_origine_trf", aide: "Pertinent seulement si espèce = truite fario", requis: false },
      { nom: "capture_technique", aide: "Hérite de technique_principale si vide", requis: false },
      { nom: "capture_appat_leurre", aide: "Hérite de appat_leurre si vide", requis: false },
      { nom: "capture_heure", aide: "HH:MM", requis: false },
      { nom: "capture_taille", aide: "cm — capture individuelle uniquement (nombre = 1)", requis: false },
      { nom: "capture_poids", aide: "Grammes", requis: false },
      { nom: "capture_nombre", aide: "≥ 1 ; > 1 déclenche un lot (taille min/max)", requis: true },
      { nom: "capture_conservee", aide: "oui / non", requis: true },
      { nom: "capture_taille_min", aide: "cm — obligatoire si lot (nombre > 1)", requis: false },
      { nom: "capture_taille_max", aide: "cm — obligatoire si lot (nombre > 1)", requis: false },
      { nom: "capture_marque", aide: "oui / non — poisson marqué ou bagué", requis: false },
      { nom: "capture_marque_numero", aide: "Obligatoire si capture_marque = oui", requis: false },
    ],
  },
];

const CODED_VALUES = [
  { colonne: "mode_peche", valeurs: ["bateau", "float tube/canoë", "bord itinérant", "bord statique"] },
  { colonne: "capture_origine_trf", valeurs: ["naturelle", "déversement", "inconnue"] },
  { colonne: "espece_recherchee", valeurs: ["Aucune", "(nom d'espèce)"] },
  { colonne: "colonnes oui/non", valeurs: ["oui", "non"] },
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
    "S001", "Lac du Bourget", "", "01/07/2026", "bord statique", "08:00", "10:00",
    "Pêche au coup", "", "1", "", "Aucune", "", "non",
    "Perche", "", "", "", "", "25", "200", "1", "oui", "", "", "non", "",
  ];
  // BOM UTF-8 : sans lui, Excel (FR) ouvre le fichier en Windows-1252 et casse les accents.
  const csv = "﻿" + EXPECTED_HEADER.join(";") + "\n" + example.join(";") + "\n";
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = "gabarit-carnet-volontaire.csv";
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
    const uri = `/v1/admin/imports/carnet-volontaire?filename=${encodeURIComponent(file.value.name)}&mode=partial`;
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

.format-help {
  margin-bottom: 1.5rem;
  box-shadow: none;
  border: 1px solid #e2e8ec;

  .card-header {
    box-shadow: none;
    cursor: pointer;
  }
  .help-intro {
    margin-bottom: 1.2rem;
    line-height: 1.7;
  }
  .col-group {
    margin-bottom: 1rem;
  }
  .col-group-head {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 0.5rem;
    margin-bottom: 0.4rem;
  }
  .col-group-hint {
    color: #8a949c;
    font-size: 0.85rem;
  }
  .col-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem;
  }
  .help-note {
    color: #6b7780;
    font-size: 0.85rem;
    margin: 0.75rem 0 1.25rem;

    .req {
      color: #1fb6a8;
      font-weight: 700;
    }
  }
  .coded-values {
    border-top: 1px solid #e2e8ec;
    padding-top: 0.9rem;
  }
  .coded-line {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.35rem;
    margin-top: 0.5rem;
  }
  .coded-col {
    color: #6b7780;
    font-size: 0.85rem;
    min-width: 11rem;
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
