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
          v-if="loggedAdmin.isNationalAdmin"
          class="button is-primary export-button"
          accept=".csv"
          @input="importCsv"
        >
          Importer un csv
        </b-upload>
        <b-button
          type="is-primary export-button"
          @click="exportCsv"
          v-if="loggedAdmin.isNationalAdmin"
        >
          Exporter en csv
        </b-button>
      </div>
    </h1>
    <div v-if="lakes.length > maxLakeBeforeShowingAutoComplete">
      Veuillez indiquer les plans d'eau à afficher
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
      Tableau des plans d'eau
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
            <div
              class="field"
              style="display: flex"
            >
              <b-checkbox
                v-show="!authorizedSamplesMap[l.id][s.id]"
                v-model="authorizedSamplesMap[l.id][s.id]"
                @input="$forceUpdate()"
              >
              </b-checkbox>
              <div
                v-if="authorizedSamplesMap[l.id][s.id]"
                class="specie-container-with-size"
              >
                Taille maillage : <br />
                <div class="input-holder">
                    <input type="checkbox" class="sample-checkbox"
                      :checked="minSizeMap[l.id][s.id] > 0"
                      @change="$event => {
                        // @ts-expect-error checked property exists as target is a checkox
                        minSizeCheckboxInput(l, s, $event.target?.checked) }"
                    >
                  </input>
                  <b-input
                    grouped
                    placeholder="Taille minimale"
                    type="number"
                    custom-class="minsize-input"
                    v-show="minSizeMap[l.id][s.id] > 0"
                    v-model="minSizeMap[l.id][s.id]"
                    @input="$forceUpdate()"
                  >
                  </b-input>
                  <span
                    v-show="minSizeMap[l.id][s.id] > 0"
                    style="margin-left:10px;float:right"
                  >cm</span>
                  <span v-show="minSizeMap[l.id][s.id] == 0">Non définie</span>
                </div>
                <br />
                Taille maximale : <br />
                <div class="input-holder">
                   <input type="checkbox" class="sample-checkbox"
                      :checked="maxSizeMap[l.id][s.id] != 1000"
                   @change="$event => {
                      // @ts-expect-error checked property exists as target is a checkox
                      maxSizeCheckboxInput(l, s, $event.target?.checked)
                    }"/>
                  >
                  </input>
                  <b-input
                    grouped
                    placeholder="Taille maximale"
                    type="number"
                    custom-class="minsize-input"
                    v-model="maxSizeMap[l.id][s.id]"
                    v-show="maxSizeMap[l.id][s.id] != 1000"
                    @input="$forceUpdate()"
                  >
                  </b-input>
                  <span
                    v-show="maxSizeMap[l.id][s.id] != 1000"
                    style="margin-left:10px;float:right"
                  >cm</span>
                  <span v-show="maxSizeMap[l.id][s.id] == 1000">Non définie</span>
                </div>
                <div
                  v-if="minSizeMap[l.id][s.id] < 0"
                  class="error"
                >
                  Taille de maillage invalide
                </div>
                <div
                  v-if="
                    !maxSizeMap[l.id][s.id] ||
                    maxSizeMap[l.id][s.id] < minSizeMap[l.id][s.id] * 1.5
                  "
                  class="error"
                >
                  Taille maximale invalide
                </div>
              </div>
              <i
                v-else
                class="specie-container-without-size"
              >
                Taille non réglementée
              </i>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="buttons">
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

const lakes: Ref<Lake[]> = ref([]);
const species: Ref<Specie[]> = ref([]);
const speciesPerLake: Ref<any> = ref({});
const authorizedSamplesMap: Ref<any> = ref({});
const minSizeMap: Ref<any> = ref({});
const maxSizeMap: Ref<any> = ref({});
const loggedAdmin: Ref<Admin> = ref({ email: "", isNationalAdmin: false });
const lakeSelectionOptions: Ref<any[]> = ref([]);
const lastLakeSelection = useStorage(
  "lastLakeSelection",
  [],
);
const selectedLakes: Ref<Lake[]> = ref([]);
const maxLakeBeforeShowingAutoComplete = ref(5);

Promise.all([
  BackendService.backendGet("/v1/referential/waterEntities"),
  BackendService.backendGet("/v1/referential/species"),
  BackendService.backendGet("/v1/referential/species-per-waterEntity"),
  BackendService.backendGet("/v1/admin/check")
]).then(data => {
  lakes.value = data[0];
  species.value = data[1];
  speciesPerLake.value = data[2];
  loggedAdmin.value = data[3];

  referentialLoaded();
});

async function reloadData() {
  speciesPerLake.value = await BackendService.backendGet(
    "/v1/referential/species-per-waterEntity"
  );
  referentialLoaded();
}

function referentialLoaded() {
  lakes.value.forEach(l => {
    authorizedSamplesMap.value[l.id] = {};
    minSizeMap.value[l.id] = {};
    maxSizeMap.value[l.id] = {};
  });
  lakeSelectionOptions.value = [];
  lakes.value.forEach(l => {
    lakeSelectionOptions.value.push({ id: l.id, label: l.name });
  })
  if (lakes.value.length < maxLakeBeforeShowingAutoComplete.value) {
    selectedLakes.value = lakes.value;
  }

  Object.keys(speciesPerLake.value).forEach(lakeId => {
    const items = speciesPerLake.value[lakeId];
    items.forEach(spl => {
      authorizedSamplesMap.value[lakeId][spl.id] = spl.authorizedSample;
      minSizeMap.value[lakeId][spl.id] = spl.minSize;
      maxSizeMap.value[lakeId][spl.id] = spl.maxSize;
    });
  });
  const instance = getCurrentInstance();
  instance?.proxy?.$forceUpdate();
}

async function save() {
  try {
    const res = await BackendService.backendPut("/v1/referential/authorized-samples", {
      targetLakes: selectedLakes.value.map(l => l.id),
      authorizations: authorizedSamplesMap.value,
      minSizes: minSizeMap.value,
      maxSizes: maxSizeMap.value
    });
    reloadData();
    console.info(res);
    Toast.open({
      message: "Tailles enregistrées",
      type: "is-success"
    });
  } catch (error: any) {
    Toast.open({
      message:
        "Erreur lors de l'enregistrement des tailles : " +
        error.message,
      type: "is-danger"
    });
  }
}

function importCsv(file) {
  const reader = new FileReader();

  authorizedSamplesMap.value = {};
  minSizeMap.value = {};
  maxSizeMap.value = {};
  lakes.value.forEach(l => {
    authorizedSamplesMap.value[l.id] = {};
    minSizeMap.value[l.id] = {};
    maxSizeMap.value[l.id] = {};
  });

  reader.readAsText(file, "UTF-8");
  reader.onload = function (evt) {
    const csvContent = evt.target.result;
    const csvLines = csvContent.split("\n");
    const csvLakes = csvLines[0].split(";");
    for (let i = 1; i < csvLines.length - 1; i++) {
      const csvColumns = csvLines[i].split(";");
      const specieId = getSpecieWithName(csvColumns[0]);
      if (!specieId) {
        Toast.open({
          message: "Espèce inconnue : " + csvColumns[0],
          type: "is-danger"
        });
        return;
      }

      for (let j = 1; j < csvColumns.length; j++) {
        const lakeId = getLakeWithName(csvLakes[j]);
        if (!lakeId) {
          Toast.open({
            message: "Plan d'eau inconnu : " + csvLakes[j],
            type: "is-danger"
          });
          return;
        }

        if (csvColumns[j] && csvColumns[j].split("-").length == 2) {
          authorizedSamplesMap.value[lakeId][specieId] = true;
          minSizeMap.value[lakeId][specieId] = csvColumns[j].split("-")[0];
          maxSizeMap.value[lakeId][specieId] = csvColumns[j].split("-")[1];
        } else {
          authorizedSamplesMap.value[lakeId][specieId] = false;
          minSizeMap.value[lakeId][specieId] = undefined;
          maxSizeMap.value[lakeId][specieId] = undefined;
        }
      }
    }
    updateFromCsv(authorizedSamplesMap, minSizeMap, maxSizeMap);
  };
}

function updateFromCsv(authorizedSamples, minSize, maxSize) {
  authorizedSamplesMap.value = authorizedSamples;
  minSizeMap.value = minSize;
  maxSizeMap.value = maxSize;

  const instance = getCurrentInstance();
  instance?.proxy?.$forceUpdate();
}

function exportCsv() {
  let csvContent = "data:text/csv;charset=utf-8,";
  csvContent += ";";
  const columns = lakes.value.map(l => l.name);
  for (let i = 0; i < columns.length; i++) {
    if (i > 0) {
      csvContent += ";";
    }
    csvContent += columns[i];
  }
  csvContent += "\n";
  species.value.forEach(specie => {
    let csvRow = "";
    csvRow += specie.name + ";";
    lakes.value
      .map(l => l.id)
      .forEach(lakeId => {
        const maillageSize = minSizeMap.value[lakeId]
          ? minSizeMap.value[lakeId][specie.id]
          : "";
        const maximumSize = maxSizeMap.value[lakeId]
          ? maxSizeMap.value[lakeId][specie.id]
          : "";
        if (maillageSize) {
          csvRow +=
            (maillageSize ?? "") +
            "-" +
            (maximumSize ?? "1000") +
            ";";
        } else {
          csvRow += ";";
        }
      });
    csvContent += csvRow + "\n";
  });
  const encodedUri = encodeURI(csvContent);
  const hiddenElement = document.createElement("a");
  hiddenElement.href = encodedUri;
  hiddenElement.target = "_blank";
  const m = new Date();
  const fileName =
    "Fishola_Export__" +
    m.getUTCFullYear() +
    "-" +
    (m.getUTCMonth() + 1) +
    "-" +
    m.getUTCDate() +
    ".csv";
  hiddenElement.download = fileName;
  hiddenElement.click();
}

function getSpecieWithName(specieName: string) {
  const specie = species.value.filter(s => s.name == specieName);
  if (specie.length == 1) {
    return specie[0].id;
  } else {
    return;
  }
}

function getLakeWithName(lakeName: string) {
  const lake = lakes.value.filter(l => l.name == lakeName);
  if (lake.length == 1) {
    return lake[0].id;
  } else {
    return;
  }
}

function minSizeCheckboxInput(l: Lake, s: Specie, value: boolean) {
  if (value) {
    minSizeMap.value[l.id][s.id] = 45;
  } else {
    minSizeMap.value[l.id][s.id] = 0;
    if (minSizeMap.value[l.id][s.id] == 1000) {
      authorizedSamplesMap.value[l.id][s.id] = false;
    }
  }
  const instance = getCurrentInstance();
  instance?.proxy?.$forceUpdate();
}

function maxSizeCheckboxInput(l: Lake, s: Specie, value: boolean) {
  if (value) {
    maxSizeMap.value[l.id][s.id] = 800;
  } else {
    maxSizeMap.value[l.id][s.id] = 1000;
    if (minSizeMap.value[l.id][s.id] == 0) {
      authorizedSamplesMap.value[l.id][s.id] = false;
    }
  }
  const instance = getCurrentInstance();
  instance?.proxy?.$forceUpdate();
}

function changeLakeSelection(newSelectedLakeIds: string[]) {
  selectedLakes.value = lakes.value.filter(l => newSelectedLakeIds.indexOf(l.id) > -1);
  localStorage.setItem("lastLakeSelection", JSON.stringify(newSelectedLakeIds));
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
  }

  .specie-container {
    display: flex;

    .minsize-input {
      width: 80px !important;
      border: 2px solid green !important;
      background-color: pink !important;
      border-color: cyan !important;
    }

    max-width: 250px;
  }

  .specie-container-with-size {
    max-width: 170px;

    .input-holder {
      display: flex;
    }
  }

  .specie-container-without-size {
    max-width: 250px;
  }
}
</style>
