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
        <b-button type="is-primary export-button" @click="exportCsv"
          v-if="loggedAdmin.isNationalAdmin">
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
    <p id="table-desc" style="display:none">
      Tableau des plans d'eau
    </p>
    <table class="table is-striped" aria-describedby="table-desc" v-if="selectedLakes.length > 0">
      <thead>
        <tr>
          <th id="th-lac-vide"></th>
          <th :id="l.id" v-for="l in selectedLakes" v-bind:key="l.id">{{ l.name }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="s in species" v-bind:key="s.id">
          <th :id="s.name">{{ s.name }}</th>
          <td v-for="l in selectedLakes" v-bind:key="l.id">
            <div class="field" style="display: flex">
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
                  <b-checkbox grouped
                   :value="minSizeMap[l.id][s.id] > 0"
                   @input="value => { minSizeCheckboxInput(l, s, value) }"
                >
                </b-checkbox>
                  <b-input grouped
                    placeholder="Taille minimale"
                    type="number"
                    custom-class="minsize-input"
                    v-show="minSizeMap[l.id][s.id] > 0"
                    v-model="minSizeMap[l.id][s.id]"
                    @input="$forceUpdate()"
                  >
                  </b-input>
                  <span v-show="minSizeMap[l.id][s.id] > 0" style="margin-left:10px;float:right">cm</span>
                  <span v-show="minSizeMap[l.id][s.id] == 0">Non définie</span>
                </div>
                <br />
                Taille maximale : <br />
                <div class="input-holder">
                  <b-checkbox grouped
                   :value="maxSizeMap[l.id][s.id] != 1000"
                   @input="value => { maxSizeCheckboxInput(l, s, value) }"
                >
                </b-checkbox>
                  <b-input grouped
                    placeholder="Taille maximale"
                    type="number"
                    custom-class="minsize-input"
                    v-model="maxSizeMap[l.id][s.id]"
                    v-show="maxSizeMap[l.id][s.id] != 1000"
                    @input="$forceUpdate()"
                  >
                  </b-input>
                  <span v-show="maxSizeMap[l.id][s.id] != 1000" style="margin-left:10px;float:right">cm</span>
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
              <i v-else class="specie-container-without-size">
                Taille non réglementée
              </i>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="buttons">
      <button class="button is-primary" @click="save()">
        Enregistrer
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";

import BackendService from "@/services/BackendService";
import MultipleAutoComplete from "@/components/MultipleAutoComplete.vue";

@Component({
  components: {MultipleAutoComplete}
})
export default class AuthorizedSamplesVue extends Vue {
  lakes = [];
  species = [];
  speciesPerLake = {};
  authorizedSamplesMap = {};
  minSizeMap = {};
  maxSizeMap = {};
  loggedAdmin = { isNationalAdmin: false}
  lakeSelectionOptions = [];
  lastLakeSelection = [];
  selectedLakes = [];
  maxLakeBeforeShowingAutoComplete = 5;

  created() {
    this.lakes = [];
    this.species = [];
    this.speciesPerLake = {};
    this.authorizedSamplesMap = {};
    this.minSizeMap = {};
    this.maxSizeMap = {};
    this.lastLakeSelection = JSON.parse(localStorage.getItem('lastLakeSelection') ?? "[]");

    Promise.all([
      BackendService.backendGet("/v1/referential/lakes"),
      BackendService.backendGet("/v1/referential/species"),
      BackendService.backendGet("/v1/referential/species-per-lake"),
      BackendService.backendGet("/v1/admin/check")
    ]).then(data => {
      this.lakes = data[0];
      this.species = data[1];
      this.speciesPerLake = data[2];
      this.loggedAdmin = data[3];

      this.referentialLoaded();
    });
  }

  async reloadData() {
    this.speciesPerLake = await BackendService.backendGet(
      "/v1/referential/species-per-lake"
    );
    this.referentialLoaded();
  }

  referentialLoaded() {
    this.lakes.forEach(l => {
      this.authorizedSamplesMap[l.id] = {};
      this.minSizeMap[l.id] = {};
      this.maxSizeMap[l.id] = {};
    });
    this.lakeSelectionOptions = [];
    this.lakes.forEach(l => {
      this.lakeSelectionOptions.push({id: l.id, label: l.name});
    })
    if (this.lakes.length < this.maxLakeBeforeShowingAutoComplete) {
      this.selectedLakes = this.lakes;
    }

    Object.keys(this.speciesPerLake).forEach(lakeId => {
      let items = this.speciesPerLake[lakeId];
      items.forEach(spl => {
        this.authorizedSamplesMap[lakeId][spl.id] = spl.authorizedSample;
        this.minSizeMap[lakeId][spl.id] = spl.minSize;
        this.maxSizeMap[lakeId][spl.id] = spl.maxSize;
      });
    });
    this.$forceUpdate();
  }

  save() {
    BackendService.backendPut("/v1/referential/authorized-samples", {
      targetLakes: this.selectedLakes.map(l => l.id),
      authorizations: this.authorizedSamplesMap,
      minSizes: this.minSizeMap,
      maxSizes: this.maxSizeMap
    }).then(
      res => {
        this.reloadData();
        console.info(res);
        this.$buefy.toast.open({
          message: "Tailles enregistrées",
          type: "is-success"
        });
      },
      error => {
        this.$buefy.toast.open({
          message:
            "Erreur lors de l'enregistrement des tailles : " +
            error.message,
          type: "is-danger"
        });
      }
    );
  }

  importCsv(file) {
    const reader = new FileReader();
    let getSpecieWithName = this.getSpecieWithName;
    let getLakeWithName = this.getLakeWithName;
    let buefy = this.$buefy;

    this.authorizedSamplesMap = {};
    this.minSizeMap = {};
    this.maxSizeMap = {};
    this.lakes.forEach(l => {
      this.authorizedSamplesMap[l.id] = {};
      this.minSizeMap[l.id] = {};
      this.maxSizeMap[l.id] = {};
    });
    let authorizedSamplesMap = this.authorizedSamplesMap;
    let minSizeMap = this.minSizeMap;
    let maxSizeMap = this.maxSizeMap;
    let updateFromCsv = this.updateFromCsv;

    reader.readAsText(file, "UTF-8");
    reader.onload = function(evt) {
      let csvContent = evt.target.result;
      const csvLines = csvContent.split("\n");
      const csvLakes = csvLines[0].split(";");
      for (let i = 1; i < csvLines.length - 1; i++) {
        const csvColumns = csvLines[i].split(";");
        const specieId = getSpecieWithName(csvColumns[0]);
        if (!specieId) {
          buefy.toast.open({
            message: "Espèce inconnue : " + csvColumns[0],
            type: "is-danger"
          });
          return;
        }

        for (let j = 1; j < csvColumns.length; j++) {
          const lakeId = getLakeWithName(csvLakes[j]);
          if (!lakeId) {
            buefy.toast.open({
              message: "Plan d'eau inconnu : " + csvLakes[j],
              type: "is-danger"
            });
            return;
          }

          if (csvColumns[j] && csvColumns[j].split("-").length == 2) {
            authorizedSamplesMap[lakeId][specieId] = true;
            minSizeMap[lakeId][specieId] = csvColumns[j].split("-")[0];
            maxSizeMap[lakeId][specieId] = csvColumns[j].split("-")[1];
          } else {
            authorizedSamplesMap[lakeId][specieId] = false;
            minSizeMap[lakeId][specieId] = undefined;
            maxSizeMap[lakeId][specieId] = undefined;
          }
        }
      }
      updateFromCsv(authorizedSamplesMap, minSizeMap, maxSizeMap);
    };
  }

  updateFromCsv(authorizedSamplesMap, minSizeMap, maxSizeMap) {
    this.authorizedSamplesMap = authorizedSamplesMap;
    this.minSizeMap = minSizeMap;
    this.maxSizeMap = maxSizeMap;

    this.$forceUpdate();
  }

  exportCsv() {
    let csvContent = "data:text/csv;charset=utf-8,";
    csvContent += ";";
    const columns = this.lakes.map(l => l.name);
    for (let i = 0; i < columns.length; i++) {
      if (i > 0) {
        csvContent += ";";
      }
      csvContent += columns[i];
    }
    csvContent += "\n";
    this.species.forEach(specie => {
      let csvRow = "";
      csvRow += specie.name + ";";
      this.lakes
        .map(l => l.id)
        .forEach(lakeId => {
          const maillageSize = this.minSizeMap[lakeId]
            ? this.minSizeMap[lakeId][specie.id]
            : "";
          const maximumSize = this.maxSizeMap[lakeId]
            ? this.maxSizeMap[lakeId][specie.id]
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

  getSpecieWithName(specieName) {
    let specie = this.species.filter(s => s.name == specieName);
    if (specie.length == 1) {
      return specie[0].id;
    } else {
      return;
    }
  }

  getLakeWithName(lakeName) {
    let lake = this.lakes.filter(l => l.name == lakeName);
    if (lake.length == 1) {
      return lake[0].id;
    } else {
      return;
    }
  }

  minSizeCheckboxInput(l, s, value) {
    if (value) {
      this.minSizeMap[l.id][s.id] = 45;
    } else {
      this.minSizeMap[l.id][s.id] = 0;
      if (this.minSizeMap[l.id][s.id] == 1000) {
        this.authorizedSamplesMap[l.id][s.id]  = false;
      }
    }
    this.$forceUpdate();
  }

  maxSizeCheckboxInput(l, s, value) {
    if (value) {
      this.maxSizeMap[l.id][s.id] = 800;
    } else {
      this.maxSizeMap[l.id][s.id] = 1000;
      if (this.minSizeMap[l.id][s.id] == 0) {
        this.authorizedSamplesMap[l.id][s.id]  = false;
      }
    }
    this.$forceUpdate();
  }

  changeLakeSelection(newSelectedLakeIds: string[]) {
    this.selectedLakes = this.lakes.filter(l => newSelectedLakeIds.indexOf(l.id) > -1);
    localStorage.setItem('lastLakeSelection', JSON.stringify(newSelectedLakeIds));
  }
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
