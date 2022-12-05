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
      Autorisations de prélèvement et taille des maillages
      <div class="align-right">
        <b-upload
          class="button is-primary export-button"
          accept=".csv"
          @input="importCsv"
        >
          Importer un csv
        </b-upload>
        <b-button type="is-primary export-button" @click="exportCsv">
          Exporter en csv
        </b-button>
      </div>
    </h1>
    <table class="table is-striped">
      <thead>
        <tr>
          <th></th>
          <th v-for="l in lakes" v-bind:key="l.id">{{ l.name }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="s in species" v-bind:key="s.id">
          <th>{{ s.name }}</th>
          <td v-for="l in lakes" v-bind:key="l.id">
            <div class="field" style="display: flex">
              <b-checkbox
                v-model="authorizedSamplesMap[l.id][s.id]"
                @input="$forceUpdate()"
              >
              </b-checkbox>
              <div
                v-if="authorizedSamplesMap[l.id][s.id]"
                class="specie-container-with-size"
              >
                <div class="input-holder">
                  <b-input
                    placeholder="Taille minimale"
                    type="number"
                    custom-class="minsize-input"
                    v-model="minSizeMap[l.id][s.id]"
                    @input="$forceUpdate()"
                  >
                  </b-input>
                  <span style="margin-left:10px;float:right">cm</span>
                </div>
                <div
                  v-if="!minSizeMap[l.id][s.id] || minSizeMap[l.id][s.id] <= 0"
                  class="error"
                >
                  Taille invalide
                </div>
              </div>
              <i v-else class="specie-container-without-size">
                Prélèvement non autorisé
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

<script lans="ts">
import { Component, Vue } from "vue-property-decorator";

import BackendService from "@/services/BackendService";

@Component
export default class AuthorizedSamplesVue extends Vue {
  lakes = [];
  species = [];
  speciesPerLake = {};
  authorizedSamplesMap = {};
  minSizeMap = {};

  created() {
    this.lakes = [];
    this.species = [];
    this.speciesPerLake = {};
    this.authorizedSamplesMap = {};
    this.minSizeMap = {};

    Promise.all([
      BackendService.backendGet("/v1/referential/lakes"),
      BackendService.backendGet("/v1/referential/species"),
      BackendService.backendGet("/v1/referential/species-per-lake")
    ]).then(data => {
      this.lakes = data[0];
      this.species = data[1];
      this.speciesPerLake = data[2];

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
    });

    Object.keys(this.speciesPerLake).forEach(lakeId => {
      let items = this.speciesPerLake[lakeId];
      items.forEach(spl => {
        this.authorizedSamplesMap[lakeId][spl.id] = spl.authorizedSample;
        this.minSizeMap[lakeId][spl.id] = spl.minSize;
      });
    });
    this.$forceUpdate();
  }

  save() {
    BackendService.backendPut("/v1/referential/authorized-samples", [
      this.authorizedSamplesMap,
      this.minSizeMap
    ]).then(
      res => {
        this.reloadData();
        console.info(res);
        this.$buefy.toast.open({
          message: "Autorisations de prélèvement enregistrées",
          type: "is-success"
        });
      },
      error => {
        this.$buefy.toast.open({
          message:
            "Erreur lors de l'enregistrement des autorisations de prélèvement : " +
            error.message,
          type: "is-danger"
        });
      }
    );
  }

  importCsv(file) {
    var reader = new FileReader();
    let getSpecieWithName = this.getSpecieWithName;
    let getLakeWithName = this.getLakeWithName;
    let buefy = this.$buefy;

    this.authorizedSamplesMap = {};
    this.minSizeMap = {};
    this.lakes.forEach(l => {
      this.authorizedSamplesMap[l.id] = {};
      this.minSizeMap[l.id] = {};
    });
    let authorizedSamplesMap = this.authorizedSamplesMap;
    let minSizeMap = this.minSizeMap;
    let updateFromCsv = this.updateFromCsv;

    reader.readAsText(file, "UTF-8");
    reader.onload = function(evt) {
      let csvContent = evt.target.result;
      const csvLines = csvContent.split("\n");
      const csvLakes = csvLines[0].split(";");
      for (var i = 1; i < csvLines.length - 1; i++) {
        const csvColumns = csvLines[i].split(";");
        const specieId = getSpecieWithName(csvColumns[0]);
        if (!specieId) {
          buefy.toast.open({
            message: "Espèce inconnue : " + csvColumns[0],
            type: "is-danger"
          });
          return;
        }

        for (var j = 1; j < csvColumns.length; j++) {
          const lakeId = getLakeWithName(csvLakes[j]);
          if (!lakeId) {
            buefy.toast.open({
              message: "Lac inconnu : " + csvLakes[j],
              type: "is-danger"
            });
            return;
          }

          if (csvColumns[j]) {
            authorizedSamplesMap[lakeId][specieId] = true;
            minSizeMap[lakeId][specieId] = csvColumns[j];
          } else {
            authorizedSamplesMap[lakeId][specieId] = false;
            minSizeMap[lakeId][specieId] = undefined;
          }
        }
      }
      updateFromCsv(authorizedSamplesMap, minSizeMap);
    };
  }

  updateFromCsv(authorizedSamplesMap, minSizeMap) {
    console.error(authorizedSamplesMap, minSizeMap);
    this.authorizedSamplesMap = authorizedSamplesMap;
    this.minSizeMap = minSizeMap;

    this.$forceUpdate();
  }

  exportCsv() {
    let csvContent = "data:text/csv;charset=utf-8,";
    csvContent += ";";
    const columns = this.lakes.map(l => l.name);
    for (var i = 0; i < columns.length; i++) {
      if (i > 0) {
        csvContent += ";";
      }
      csvContent += columns[i];
    }
    csvContent += "\n";

    this.species.forEach(specie => {
      var csvRow = "";
      csvRow += specie.name + ";";
      this.lakes
        .map(l => l.id)
        .forEach(lakeId => {
          const maillageSize = this.minSizeMap[lakeId]
            ? this.minSizeMap[lakeId][specie.id]
            : "";
          csvRow += (maillageSize ? maillageSize : "") + ";";
        });
      csvContent += csvRow + "\n";
    });
    var encodedUri = encodeURI(csvContent);
    window.open(encodedUri);
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
}
</script>

<style scoped lang="less">
@import "../../less/main";

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

  .specie-container {
    display: flex;
  }
  .error {
    color: red;
    font-weight: bold;
  }

  .specie-container {
    .minsize-input {
      width: 80px !important;
      border: 2px solid green !important;
      background-color: pink !important;
      border-color: cyan !important;
    }
    max-width: 250px;
  }

  .specie-container-with-size {
    max-width: 130px;
    .input-holder {
      display: flex;
    }
  }
  .specie-container-without-size {
    max-width: 250px;
  }
}
</style>
