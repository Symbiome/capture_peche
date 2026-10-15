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
        <h1 class="title">Import — Enquête terrain</h1>
        <p class="subtitle is-6">
          Classeur Excel (.xlsx) multi-feuilles : une feuille par table, un onglet
          « Session souvenir » facultatif. Ou fichier CSV plat (.csv) : une ligne par capture,
          informations de session, de sortie et de pêcheur répétées sur chaque ligne.
        </p>
      </div>
      <!-- Mis en valeur (#205), comme pour l'import carnet volontaire. -->
      <div class="buttons">
        <b-button
          type="is-primary"
          size="is-medium"
          icon-left="file-download-outline"
          :loading="downloadingTemplate === 'xlsx'"
          @click="downloadTemplate('xlsx')"
        >
          Gabarit XLSX
        </b-button>
        <b-button
          type="is-primary"
          size="is-medium"
          outlined
          icon-left="file-download-outline"
          :loading="downloadingTemplate === 'csv'"
          @click="downloadTemplate('csv')"
        >
          Gabarit CSV
        </b-button>
      </div>
    </header>

    <b-collapse class="card format-help" animation="slide" :open="false">
      <template #trigger="props">
        <div class="card-header" role="button">
          <p class="card-header-title">Format attendu — XLSX à 4 feuilles ou CSV plat</p>
          <a class="card-header-icon">
            <b-icon :icon="props.open ? 'menu-up' : 'menu-down'"></b-icon>
          </a>
        </div>
      </template>

      <div class="card-content">
        <p class="help-intro">
          Chaque ligne est contrôlée en trois étapes —
          <b-tag type="is-danger">Format</b-tag> (colonnes, dates, heures, clés de liaison),
          <b-tag type="is-warning">Référentiel</b-tag> (espèces, techniques, secteurs, périmètre) puis
          <b-tag type="is-info">Règle métier</b-tag> (tailles aberrantes, cohérence des lots, bredouille).
          Les lignes valides sont importées, les autres listées dans le rapport.
        </p>

        <div v-for="sheet in SHEETS" :key="sheet.nom" class="col-group">
          <div class="col-group-head">
            <strong>{{ sheet.nom }}</strong>
            <b-tag v-if="!sheet.obligatoire" type="is-light">facultative</b-tag>
            <span class="col-group-hint">{{ sheet.aide }}</span>
          </div>
          <div class="col-tags">
            <b-tag v-for="c in sheet.colonnes" :key="c" type="is-primary">{{ c }}</b-tag>
          </div>
        </div>

        <p class="help-note">
          Les captures d'une même sortie enquêtée partagent le même
          <code>Code sortie</code> / <code>Code pêcheur</code>. Dates au format
          <code>JJ/MM/AAAA</code>, heures <code>HH:mm</code>.
        </p>

        <div class="col-group flat-format">
          <div class="col-group-head">
            <strong>CSV plat (.csv)</strong>
            <span class="col-group-hint">Séparateur « ; », UTF-8, une ligne par capture ou une ligne « bredouille ».</span>
          </div>
          <div class="col-tags">
            <b-tag v-for="c in FLAT_COLUMNS" :key="c" type="is-primary">{{ c }}</b-tag>
          </div>
          <ul class="help-note flat-rules">
            <li><code>type_sortie</code> : <code>en_cours</code> (sortie enquêtée) ou <code>souvenir</code>
              (dernière sortie du pêcheur, sans code ni heures de sortie).</li>
            <li>Les lignes de même <code>code_session</code> forment une session, celles de même
              <code>code_sortie</code> une sortie, celles de même <code>code_sortie</code> +
              <code>code_pecheur</code> la sortie d'un pêcheur. Les souvenirs d'un même
              <code>code_session</code> + <code>code_pecheur</code> forment une seule sortie souvenir ;
              le pêcheur doit figurer sur une ligne <code>en_cours</code> de la session.</li>
            <li>Les valeurs répétées doivent être identiques dans leur groupe (espaces, majuscules et accents
              ignorés), sinon la ligne divergente est rejetée.</li>
            <li><code>espece_recherchee</code> vide = « Toutes espèces ». Sortie bredouille : une seule ligne,
              colonnes <code>capture_*</code> vides.</li>
          </ul>
        </div>
      </div>
    </b-collapse>

    <div class="box">
      <b-field>
        <b-upload v-model="file" drag-drop expanded accept=".xlsx,.csv">
          <section class="ex-drop">
            <p><b-icon icon="upload" size="is-large"></b-icon></p>
            <p class="drop-label">Déposez un classeur XLSX ou un fichier CSV, ou cliquez pour en choisir un</p>
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
import { useToast } from "buefy";
import { ref, computed } from "vue";

const Toast = useToast();
const file = ref<File | null>(null);
const loading = ref(false);
const downloadingTemplate = ref<"xlsx" | "csv" | null>(null);
const result = ref<any>(null);

// Reflète SurveySchema.HEADER_* côté backend (fr.inrae.fishola.rest.imports.survey).
const SHEETS = [
  {
    nom: "Session d'enquête", obligatoire: true,
    aide: "Une ligne par session (enquêteur + site + date).",
    colonnes: ["Code session", "Secteur", "Date", "Heure de début de session", "Heure de fin de session",
      "Nom de l'opérateur", "Nb pêcheurs carnassiers du bord observés non-enquêtés",
      "Nb pêcheurs carnassiers en bateau observés non-enquêtés"],
  },
  {
    nom: "Sortie de pêche", obligatoire: true,
    aide: "Une ligne par sortie, rattachée à une session (Code session).",
    colonnes: ["Code session", "Code sortie", "Heure du contrôle",
      "Heure de début de pêche", "Heure de fin de pêche prévue", "Date de fin de pêche prévue"],
  },
  {
    nom: "Capture pêcheur", obligatoire: true,
    aide: "Une ligne par capture (ou bredouille), rattachée à une sortie (Code sortie) et un pêcheur (Code pêcheur).",
    colonnes: ["Code sortie", "Code pêcheur", "Département origine", "Espèce recherchée",
      "Mode de pêche", "Technique", "Nombre de lignes", "Appât / type de leurre", "Bredouille",
      "Espèce (capture)", "Taille", "Nombre", "Poisson conservé", "Taille min", "Taille max"],
  },
  {
    nom: "Session souvenir", obligatoire: false,
    aide: "Dernière sortie passée d'un pêcheur déjà rencontré dans « Capture pêcheur ».",
    colonnes: ["Code pêcheur", "Site pêché", "Date", "Période de la journée", "Espèce recherchée",
      "Mode de pêche", "Technique", "Nombre de lignes", "Appât / type de leurre", "Bredouille",
      "Espèce (capture)", "Taille", "Nombre", "Poisson conservé", "Taille min", "Taille max"],
  },
];

// Reflète SurveySchema.FLAT_HEADER côté backend (#242).
const FLAT_COLUMNS = [
  "type_sortie", "code_session", "secteur", "date_session", "heure_debut_session", "heure_fin_session",
  "nom_operateur", "nb_bord_non_enquetes", "nb_bateau_non_enquetes", "code_sortie", "heure_controle",
  "heure_debut", "heure_fin_prevue", "date_fin_prevue", "code_pecheur", "departement_origine",
  "site_peche", "date_souvenir", "periode_journee", "espece_recherchee", "mode_peche", "technique",
  "nombre_lignes", "appat_leurre", "bredouille", "capture_espece", "capture_taille", "capture_nombre",
  "capture_conservee", "capture_taille_min", "capture_taille_max"
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

/** Gabarit XLSX (#207) ou CSV plat (#242), généré par le backend depuis le schéma d'import. */
async function downloadTemplate(format: "xlsx" | "csv") {
  downloadingTemplate.value = format;
  try {
    const blob = await BackendService.backendGetBlob("/v1/admin/imports/survey/template?format=" + format);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "gabarit-enquete-terrain." + format;
    link.click();
    URL.revokeObjectURL(url);
  } catch (err) {
    Toast.open({ message: "Impossible de télécharger le gabarit, réessayez.", type: "is-danger" });
  } finally {
    downloadingTemplate.value = null;
  }
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
    const uri = `/v1/admin/imports/survey?filename=${encodeURIComponent(file.value.name)}&mode=partial`;
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
    margin: 0.75rem 0 0;
  }
  .flat-format {
    margin-top: 1.5rem;
  }
  .flat-rules {
    list-style: disc;
    padding-left: 1.25rem;
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
    background: var(--bulma-scheme-main);
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
