<!--
  #%L
  Fishola :: Admin
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
  <div class="authorized-samples">
    <h1>
      Maillages et tailles maximales
      <div class="align-right">
        <b-upload
          v-if="loggedAdmin.isNationalAdmin && selectedLakes.length > 0"
          class="button is-primary export-button"
          accept=".csv"
          @input="importCsv"
        >
          Importer un csv
        </b-upload>
        <b-button
          type="is-primary export-button"
          @click="exportCsv"
          v-if="loggedAdmin.isNationalAdmin && selectedLakes.length > 0"
        >
          Exporter en csv
        </b-button>
      </div>
    </h1>

    <!--
      #154 : le référentiel hydrographique compte ~181 000 entités (extension
      multi-milieux RM&C). On borne obligatoirement le périmètre par département
      AVANT de charger la matrice espèces × entités, sinon le navigateur et le
      backend tombent en mémoire insuffisante.
    -->
    <div class="field perimeter">
      <label class="label">Département</label>
      <b-select
        v-model="selectedDepartment"
        placeholder="Choisir un département"
        @input="changeDepartment"
      >
        <option
          v-for="d in departments"
          :key="d"
          :value="d"
        >
          Département {{ d }}
        </option>
      </b-select>
    </div>

    <p v-if="!selectedDepartment">
      Choisissez un département pour afficher les espèces et milieux à configurer.
    </p>
    <p v-else-if="departmentEntities.length === 0">
      Aucune entité hydrographique chargée pour ce département.
    </p>

    <div v-if="selectedDepartment && departmentEntities.length > maxLakeBeforeShowingAutoComplete">
      Ce département compte {{ departmentEntities.length }} milieux : indiquez ceux à afficher.
      <MultipleAutoComplete
        :defaultSelection="lastLakeSelection"
        :data="lakeSelectionOptions"
        @updated="(value) => changeLakeSelection(value)"
      />
    </div>

    <p
      id="table-desc"
      style="display:none"
    >
      Tableau des tailles réglementaires par espèce et par milieu
    </p>
    <table
      class="table is-striped"
      aria-describedby="table-desc"
      v-if="selectedLakes.length > 0"
    >
      <thead>
        <tr>
          <th id="th-lac-vide"></th>
          <th
            :id="l.id"
            v-for="l in selectedLakes"
            v-bind:key="l.id"
          >{{ l.name }}</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="s in species"
          v-bind:key="s.id"
        >
          <th :id="s.name">{{ s.name }}</th>
          <td
            v-for="l in selectedLakes"
            v-bind:key="l.id"
          >
            <div v-if="!regulatedMap[l.id][s.id]" class="unregulated">
              <i>Taille non réglementée</i>
              <br />
              <b-button
                size="is-small"
                type="is-text"
                @click="startRegulation(l, s)"
              >
                Spécifier une taille réglementaire
              </b-button>
            </div>
            <div v-else class="regulated">
              <b-field label="Taille minimale (cm)" custom-class="is-small">
                <b-input
                  type="number"
                  min="1"
                  size="is-small"
                  v-model="minSizeMap[l.id][s.id]"
                  @input="forceUpdate()"
                />
              </b-field>
              <b-field label="Taille maximale (cm)" custom-class="is-small">
                <b-input
                  type="number"
                  size="is-small"
                  placeholder="Non définie"
                  v-model="maxSizeMap[l.id][s.id]"
                  @input="forceUpdate()"
                />
              </b-field>
              <b-field label="Maillage (cm)" custom-class="is-small">
                <b-input
                  type="number"
                  size="is-small"
                  placeholder="Non défini"
                  v-model="meshSizeMap[l.id][s.id]"
                  @input="forceUpdate()"
                />
              </b-field>
              <div v-if="cellError(l, s)" class="error">{{ cellError(l, s) }}</div>
              <b-button
                size="is-small"
                type="is-text"
                @click="stopRegulation(l, s)"
              >
                Retirer la réglementation
              </b-button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="buttons" v-if="selectedLakes.length > 0">
      <button
        class="button is-primary"
        @click="save()"
      >
        Enregistrer
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, ref, Ref } from "vue";

import BackendService from "@/services/BackendService";
import MultipleAutoComplete from "@/components/MultipleAutoComplete.vue";
import { useToast } from "buefy";
import { useStorage } from "@vueuse/core";

const Toast = useToast();

// Valeurs par défaut proposées quand l'admin rend une espèce réglementée (#154).
const DEFAULT_MIN_SIZE = 30;
const DEFAULT_MAX_SIZE = 60;
const DEFAULT_MESH_SIZE = 10;
// Sentinelles « non défini » du backend et de l'export CSV historiques.
const MAX_UNSET = 1000;
const MESH_UNSET = 0;

// Dans les maps de l'UI, une chaîne vide représente « non défini ».
type SizeValue = number | string;

const species: Ref<Specie[]> = ref([]);
const departments: Ref<string[]> = ref([]);
const selectedDepartment: Ref<string | null> = ref(null);
const departmentEntities: Ref<Lake[]> = ref([]);

const regulatedMap: Ref<any> = ref({});
const minSizeMap: Ref<any> = ref({});
const maxSizeMap: Ref<any> = ref({});
const meshSizeMap: Ref<any> = ref({});

const loggedAdmin: Ref<Admin> = ref({ email: "", isNationalAdmin: false });
const lakeSelectionOptions: Ref<any[]> = ref([]);
const lastLakeSelection = useStorage("lastLakeSelection", []);
const selectedLakes: Ref<Lake[]> = ref([]);
const maxLakeBeforeShowingAutoComplete = 5;

Promise.all([
  BackendService.backendGet("/v1/referential/species"),
  BackendService.backendGet("/v1/referential/departments"),
  BackendService.backendGet("/v1/admin/check")
]).then(data => {
  species.value = data[0];
  departments.value = data[1];
  loggedAdmin.value = data[2];
});

async function changeDepartment() {
  selectedLakes.value = [];
  lakeSelectionOptions.value = [];
  departmentEntities.value = [];
  if (!selectedDepartment.value) {
    return;
  }
  departmentEntities.value = await BackendService.backendGet(
    "/v1/referential/waterEntities/by-department/" + encodeURIComponent(selectedDepartment.value)
  );
  lakeSelectionOptions.value = departmentEntities.value.map(l => ({ id: l.id, label: l.name }));
  if (departmentEntities.value.length <= maxLakeBeforeShowingAutoComplete) {
    selectedLakes.value = departmentEntities.value;
    await loadMatrix();
  }
}

function changeLakeSelection(newSelectedLakeIds: string[]) {
  selectedLakes.value = departmentEntities.value.filter(l => newSelectedLakeIds.indexOf(l.id) > -1);
  localStorage.setItem("lastLakeSelection", JSON.stringify(newSelectedLakeIds));
  if (selectedLakes.value.length > 0) {
    loadMatrix();
  }
}

async function loadMatrix() {
  const query = selectedLakes.value
    .map(l => "waterEntityId=" + encodeURIComponent(l.id))
    .join("&");
  const speciesPerLake = await BackendService.backendGet(
    "/v1/referential/species-per-waterEntity?" + query
  );
  buildMaps(speciesPerLake);
}

function buildMaps(speciesPerLake: any) {
  regulatedMap.value = {};
  minSizeMap.value = {};
  maxSizeMap.value = {};
  meshSizeMap.value = {};
  selectedLakes.value.forEach(l => {
    regulatedMap.value[l.id] = {};
    minSizeMap.value[l.id] = {};
    maxSizeMap.value[l.id] = {};
    meshSizeMap.value[l.id] = {};
    species.value.forEach(s => {
      regulatedMap.value[l.id][s.id] = false;
      minSizeMap.value[l.id][s.id] = "";
      maxSizeMap.value[l.id][s.id] = "";
      meshSizeMap.value[l.id][s.id] = "";
    });
  });

  Object.keys(speciesPerLake).forEach(lakeId => {
    if (!regulatedMap.value[lakeId]) {
      return;
    }
    speciesPerLake[lakeId].forEach(spl => {
      regulatedMap.value[lakeId][spl.id] = spl.authorizedSample;
      minSizeMap.value[lakeId][spl.id] = spl.minSize > 0 ? spl.minSize : "";
      maxSizeMap.value[lakeId][spl.id] =
        spl.maxSize && spl.maxSize !== MAX_UNSET ? spl.maxSize : "";
      meshSizeMap.value[lakeId][spl.id] = spl.meshSize ? spl.meshSize : "";
    });
  });
  forceUpdate();
}

function startRegulation(l: Lake, s: Specie) {
  regulatedMap.value[l.id][s.id] = true;
  minSizeMap.value[l.id][s.id] = DEFAULT_MIN_SIZE;
  maxSizeMap.value[l.id][s.id] = DEFAULT_MAX_SIZE;
  meshSizeMap.value[l.id][s.id] = DEFAULT_MESH_SIZE;
  forceUpdate();
}

function stopRegulation(l: Lake, s: Specie) {
  regulatedMap.value[l.id][s.id] = false;
  minSizeMap.value[l.id][s.id] = "";
  maxSizeMap.value[l.id][s.id] = "";
  meshSizeMap.value[l.id][s.id] = "";
  forceUpdate();
}

function cellError(l: Lake, s: Specie): string | null {
  if (!regulatedMap.value[l.id][s.id]) {
    return null;
  }
  const rawMin = minSizeMap.value[l.id][s.id];
  const rawMax = maxSizeMap.value[l.id][s.id];
  const rawMesh = meshSizeMap.value[l.id][s.id];
  const min = Number(rawMin);
  if (rawMin === "" || !min || min <= 0) {
    return "La taille minimale est obligatoire pour une espèce réglementée.";
  }
  if (rawMax !== "" && Number(rawMax) <= min) {
    return "La taille maximale doit être supérieure à la taille minimale.";
  }
  if (rawMesh !== "" && Number(rawMesh) < 0) {
    return "Le maillage ne peut pas être négatif.";
  }
  return null;
}

function hasErrors(): boolean {
  return selectedLakes.value.some(l =>
    species.value.some(s => cellError(l, s) !== null)
  );
}

function forceUpdate() {
  const instance = getCurrentInstance();
  instance?.proxy?.$forceUpdate();
}

// Convertit une map de l'UI (chaîne vide = non défini) vers les sentinelles
// attendues par le backend.
function toPayloadMap(source: any, unset: number): any {
  const result: any = {};
  selectedLakes.value.forEach(l => {
    result[l.id] = {};
    species.value.forEach(s => {
      const raw: SizeValue = source[l.id][s.id];
      result[l.id][s.id] = raw === "" || raw === null || raw === undefined ? unset : Number(raw);
    });
  });
  return result;
}

async function save() {
  if (hasErrors()) {
    Toast.open({
      message: "Corrigez les tailles en erreur avant d'enregistrer.",
      type: "is-danger"
    });
    return;
  }
  try {
    const res = await BackendService.backendPut("/v1/referential/authorized-samples", {
      targetLakes: selectedLakes.value.map(l => l.id),
      authorizations: regulatedMap.value,
      minSizes: toPayloadMap(minSizeMap.value, 0),
      maxSizes: toPayloadMap(maxSizeMap.value, MAX_UNSET),
      meshSizes: toPayloadMap(meshSizeMap.value, MESH_UNSET)
    });
    console.info(res);
    Toast.open({ message: "Tailles enregistrées", type: "is-success" });
    await loadMatrix();
  } catch (error: any) {
    Toast.open({
      message: "Erreur lors de l'enregistrement des tailles : " + error.message,
      type: "is-danger"
    });
  }
}

function getSpecieWithName(specieName: string) {
  const specie = species.value.filter(s => s.name == specieName);
  return specie.length == 1 ? specie[0].id : undefined;
}

function getLakeWithName(lakeName: string) {
  const lake = selectedLakes.value.filter(l => l.name == lakeName);
  return lake.length == 1 ? lake[0].id : undefined;
}

function importCsv(file) {
  const reader = new FileReader();
  reader.readAsText(file, "UTF-8");
  reader.onload = function (evt) {
    const csvContent = evt.target.result as string;
    const csvLines = csvContent.split("\n");
    const csvLakes = csvLines[0].split(";");
    for (let i = 1; i < csvLines.length - 1; i++) {
      const csvColumns = csvLines[i].split(";");
      const specieId = getSpecieWithName(csvColumns[0]);
      if (!specieId) {
        Toast.open({ message: "Espèce inconnue : " + csvColumns[0], type: "is-danger" });
        return;
      }
      for (let j = 1; j < csvColumns.length; j++) {
        const lakeId = getLakeWithName(csvLakes[j]);
        if (!lakeId) {
          Toast.open({ message: "Milieu hors périmètre : " + csvLakes[j], type: "is-danger" });
          return;
        }
        applyCsvCell(lakeId, specieId, csvColumns[j]);
      }
    }
    forceUpdate();
  };
}

// Format d'une cellule CSV : « min-max » ou « min-max-maillage » (rétro-compatible
// avec l'export à deux valeurs). Cellule vide => espèce non réglementée.
function applyCsvCell(lakeId: string, specieId: string, raw: string) {
  const parts = (raw ?? "").trim().split("-").filter(p => p !== "");
  if (parts.length >= 2) {
    regulatedMap.value[lakeId][specieId] = true;
    minSizeMap.value[lakeId][specieId] = Number(parts[0]);
    maxSizeMap.value[lakeId][specieId] =
      parts[1] && Number(parts[1]) !== MAX_UNSET ? Number(parts[1]) : "";
    meshSizeMap.value[lakeId][specieId] =
      parts[2] && Number(parts[2]) !== MESH_UNSET ? Number(parts[2]) : "";
  } else {
    regulatedMap.value[lakeId][specieId] = false;
    minSizeMap.value[lakeId][specieId] = "";
    maxSizeMap.value[lakeId][specieId] = "";
    meshSizeMap.value[lakeId][specieId] = "";
  }
}

function exportCsv() {
  let csvContent = "data:text/csv;charset=utf-8,;";
  csvContent += selectedLakes.value.map(l => l.name).join(";") + "\n";
  species.value.forEach(specie => {
    let csvRow = specie.name + ";";
    selectedLakes.value.forEach(l => {
      if (regulatedMap.value[l.id] && regulatedMap.value[l.id][specie.id]) {
        const min = minSizeMap.value[l.id][specie.id] || "";
        const max = maxSizeMap.value[l.id][specie.id] || MAX_UNSET;
        const mesh = meshSizeMap.value[l.id][specie.id] || MESH_UNSET;
        csvRow += min + "-" + max + "-" + mesh + ";";
      } else {
        csvRow += ";";
      }
    });
    csvContent += csvRow + "\n";
  });
  const hiddenElement = document.createElement("a");
  hiddenElement.href = encodeURI(csvContent);
  hiddenElement.target = "_blank";
  const m = new Date();
  hiddenElement.download =
    "Fishola_Export__" + m.getUTCFullYear() + "-" + (m.getUTCMonth() + 1) + "-" + m.getUTCDate() + ".csv";
  hiddenElement.click();
}
</script>

<style scoped lang="less">
.authorized-samples {
  h1 {
    display: flex;
    justify-content: space-between;

    .export-button {
      margin-left: 10px;
    }
  }

  .perimeter {
    max-width: 320px;
    margin-bottom: 20px;
  }

  .table {
    width: 100%;
  }

  .buttons {
    width: 100%;
    display: flex;
    flex-direction: row-reverse;
    padding-right: 30px;
    padding-top: 10px;
  }

  .error {
    color: red;
    font-weight: bold;
    margin: 5px 0;
  }

  .regulated {
    max-width: 190px;
  }

  .unregulated {
    max-width: 220px;
  }
}
</style>
