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
  <section class="accounts-page">
    <header class="page-header">
      <h1 class="title">Inscriptions et comptes</h1>
      <p class="subtitle is-6">Mode bêta-testeurs, réservé aux administrateurs nationaux.</p>
    </header>

    <b-notification v-if="betaTestersMode === false" type="is-info" :closable="false">
      Le mode bêta-testeurs est inactif sur cet environnement : l'inscription libre reste ouverte
      et l'import de comptes est désactivé. Il n'est actif qu'en production.
    </b-notification>

    <template v-if="betaTestersMode">
      <div class="box">
        <h2 class="subtitle is-5">Inscription libre dans l'app pêcheur</h2>
        <b-field>
          <b-switch
            v-model="registrationEnabled"
            :disabled="registrationEnabled === null || savingRegistration"
            @update:modelValue="saveRegistration"
          >
            {{ registrationEnabled ? "Ouverte : chacun peut créer son compte" : "Fermée : seuls les comptes importés peuvent se connecter" }}
          </b-switch>
        </b-field>
        <p class="help-note">
          Fermée, l'app masque « Créer un compte » et l'API refuse toute inscription,
          y compris la validation d'un lien reçu avant la fermeture.
        </p>
      </div>

      <div class="box">
        <header class="section-header">
          <h2 class="subtitle is-5">Import de comptes (CSV)</h2>
          <b-button icon-left="file-download-outline" @click="downloadTemplate">Télécharger le gabarit</b-button>
        </header>
        <p class="help-note">
          Une ligne par compte, séparateur « ; » : <code>{{ EXPECTED_HEADER.join(";") }}</code>.
          Un même e-mail peut avoir un compte <code>pecheur</code> et un compte staff : mettez deux lignes.
          Un compte déjà existant est ignoré, jamais modifié : l'import peut être rejoué.
        </p>
        <ul class="help-list">
          <li><code>profil</code> : {{ PROFILES.join(", ") }}.</li>
          <li>
            <code>perimetre</code> : codes département séparés par des virgules (ex. <code>74,73</code>),
            obligatoire pour <code>operateur</code> et <code>admin_regional</code>, vide sinon.
          </li>
          <li><code>prenom</code> : obligatoire pour un pêcheur ; son pseudo est créé à partir du prénom et du nom.</li>
          <li>
            <code>mot_de_passe</code> : 6 caractères minimum, stocké haché. Le fichier contient des mots de passe en
            clair : ne le versez pas dans le dépôt et supprimez-le après l'import.
          </li>
        </ul>

        <b-field>
          <b-upload v-model="file" drag-drop expanded accept=".csv,text/csv">
            <section class="ex-drop">
              <p><b-icon icon="upload" size="is-large"></b-icon></p>
              <p class="drop-label">Déposez un fichier CSV ou cliquez pour en choisir un</p>
            </section>
          </b-upload>
        </b-field>
        <div class="actions">
          <b-tag v-if="file" type="is-primary" closable @close="reset">{{ file.name }}</b-tag>
          <b-button type="is-primary" icon-left="account-multiple-plus" :disabled="!file || importing"
                    :loading="importing" @click="doImport">
            Importer
          </b-button>
        </div>
      </div>

      <b-notification v-if="fileError" type="is-danger" :closable="false">{{ fileError }}</b-notification>

      <template v-if="report">
        <div class="stats">
          <div class="stat"><span class="stat-value">{{ report.total }}</span><span class="stat-label">ligne(s) lue(s)</span></div>
          <div class="stat is-ok"><span class="stat-value">{{ report.created }}</span><span class="stat-label">compte(s) créé(s)</span></div>
          <div class="stat"><span class="stat-value">{{ report.ignored }}</span><span class="stat-label">compte(s) déjà existant(s)</span></div>
          <div class="stat" :class="{ 'is-ko': report.rejected > 0 }">
            <span class="stat-value">{{ report.rejected }}</span><span class="stat-label">ligne(s) en erreur</span>
          </div>
        </div>
        <div class="box mt-4">
          <b-table :data="report.lines" :paginated="report.lines.length > 20" per-page="20" narrowed>
            <b-table-column field="line" label="Ligne" width="80" v-slot="props">{{ props.row.line }}</b-table-column>
            <b-table-column field="email" label="E-mail" v-slot="props">{{ props.row.email }}</b-table-column>
            <b-table-column field="profile" label="Profil" v-slot="props"><code>{{ props.row.profile }}</code></b-table-column>
            <b-table-column field="status" label="Résultat" v-slot="props">
              <b-tag :type="STATUS_TAGS[props.row.status]">{{ STATUS_LABELS[props.row.status] }}</b-tag>
            </b-table-column>
            <b-table-column field="message" label="Détail" v-slot="props">{{ props.row.message }}</b-table-column>
          </b-table>
        </div>
      </template>
    </template>
  </section>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import { useToast } from "buefy";
import { onMounted, ref } from "vue";

interface AccountImportLine {
  line: number;
  email: string;
  profile: string;
  status: "CREATED" | "IGNORED" | "REJECTED";
  message: string;
}

interface AccountImportReport {
  total: number;
  created: number;
  ignored: number;
  rejected: number;
  fileError: string | null;
  lines: AccountImportLine[];
}

// Reflète AccountImportService.EXPECTED_COLUMNS et AccountProfile côté backend.
const EXPECTED_HEADER = ["email", "prenom", "nom", "profil", "perimetre", "mot_de_passe"];
const PROFILES = ["pecheur", "operateur", "admin_regional", "admin_national"];
const STATUS_LABELS: Record<string, string> = { CREATED: "Créé", IGNORED: "Déjà existant", REJECTED: "Erreur" };
const STATUS_TAGS: Record<string, string> = { CREATED: "is-success", IGNORED: "is-light", REJECTED: "is-danger" };

const Toast = useToast();
const betaTestersMode = ref<boolean | null>(null);
const registrationEnabled = ref<boolean | null>(null);
const savingRegistration = ref(false);
const file = ref<File | null>(null);
const importing = ref(false);
const report = ref<AccountImportReport | null>(null);
const fileError = ref<string | null>(null);

onMounted(async () => {
  const admin = await BackendService.backendGet("/v1/admin/check");
  betaTestersMode.value = !!admin.betaTestersMode;
  const status = await BackendService.backendGet("/v1/security/registration");
  registrationEnabled.value = status.enabled;
});

/** Enregistre l'ouverture ou la fermeture de l'inscription libre. */
async function saveRegistration(enabled: boolean) {
  savingRegistration.value = true;
  try {
    const saved = await BackendService.backendPut("/v1/admin/registration", { enabled });
    registrationEnabled.value = saved.enabled;
    Toast.open({ message: saved.enabled ? "Inscription ouverte" : "Inscription fermée", type: "is-success" });
  } catch (err) {
    registrationEnabled.value = !enabled;
    Toast.open({ message: "Le réglage n'a pas pu être enregistré", type: "is-danger" });
  } finally {
    savingRegistration.value = false;
  }
}

/** Gabarit CSV : en-tête et une ligne d'exemple par profil, sans mot de passe réel. */
function downloadTemplate() {
  const examples = [
    "pecheur@exemple.fr;Camille;Martin;pecheur;;A_REMPLACER",
    "pecheur@exemple.fr;Camille;Martin;admin_regional;74,73;A_REMPLACER",
    "operateur@exemple.fr;Louis;Bernard;operateur;74;A_REMPLACER",
    "national@exemple.fr;Inès;Petit;admin_national;;A_REMPLACER",
  ];
  // BOM UTF-8 : sans lui, Excel (FR) ouvre le fichier en Windows-1252 et casse les accents.
  const csv = "﻿" + [EXPECTED_HEADER.join(";"), ...examples].join("\n") + "\n";
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = "gabarit-comptes.csv";
  link.click();
  URL.revokeObjectURL(url);
}

function reset() {
  file.value = null;
  report.value = null;
  fileError.value = null;
}

/** Envoie le fichier et affiche le rapport, ou l'erreur de fichier. */
async function doImport() {
  if (!file.value) return;
  importing.value = true;
  report.value = null;
  fileError.value = null;
  try {
    report.value = await BackendService.backendPostBinary("/v1/admin/accounts/import", await file.value.arrayBuffer());
  } catch (err: any) {
    fileError.value = err?.content?.fileError ?? "Échec de l'import (voir la console / réessayer).";
  } finally {
    importing.value = false;
  }
}
</script>

<style scoped lang="less">
.page-header,
.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.help-note {
  color: #6b7780;
  font-size: 0.9rem;
  margin-bottom: 0.75rem;
}

.help-list {
  list-style: disc;
  margin: 0 0 1rem 1.25rem;
  font-size: 0.9rem;
}

.ex-drop {
  padding: 2rem;
  text-align: center;

  .drop-label {
    font-weight: 600;
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
</style>
