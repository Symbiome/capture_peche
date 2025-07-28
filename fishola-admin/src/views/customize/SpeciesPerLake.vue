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
  <div class="species-per-lake">
    <h1 id="table-title">Espèces par lac</h1>

    <div v-if="lakes.length > maxLakeBeforeShowingAutoComplete">
      Veuillez indiquer les lacs à afficher
      <MultipleAutoComplete
        :defaultSelection="lastLakeSelection"
        :data="lakeSelectionOptions"
        @updated="(value) => changeLakeSelection(value)"
      />
  </div>
  <table class="table is-striped" aria-describedby="table-title" v-if="selectedLakes.length > 0">
      <thead>
        <tr>
          <th id="empty-lake-cell"></th>
          <th :id="l.name" v-for="l in selectedLakes" v-bind:key="l.id">
            {{ l.name }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="s in species" v-bind:key="s.id">
          <th :id="s.name">{{ s.name }}</th>
          <td v-for="l in selectedLakes" v-bind:key="l.id">
            <div class="field">
              <b-input
                v-model="speciesPerLakeAliases[l.id][s.id]"
                placeholder="Nom spécifique"
              ></b-input>
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

@Component(
  {components: {MultipleAutoComplete}}
)
export default class SpeciesPerLakeVue extends Vue {
  lakes = [];
  species = [];
  speciesPerLake = {};
  speciesPerLakeAliases = {};
  lakeSelectionOptions = [];
  lastLakeSelection = [];
  selectedLakes = [];
  maxLakeBeforeShowingAutoComplete = 5;

  created() {
    this.lastLakeSelection = JSON.parse(localStorage.getItem('lastLakeSelection') ?? "[]");
    Promise.all([
      BackendService.backendGet("/v1/referential/lakes"),
      BackendService.backendGet("/v1/referential/species"),
      BackendService.backendGet("/v1/referential/species-per-lake")
    ]).then(data => {
      this.lakes = data[0];
      this.species = data[1];
      this.speciesPerLake = data[2];
      this.lakeSelectionOptions = [];
      this.lakes.forEach(l => {
        this.lakeSelectionOptions.push({id: l.id, label: l.name});
      })
      if (this.lakes.length < this.maxLakeBeforeShowingAutoComplete) {
        this.selectedLakes = this.lakes;
      }
      this.referentialLoaded();
    });
  }

  referentialLoaded() {
    this.lakes.forEach(l => {
      this.speciesPerLakeAliases[l.id] = {};
    });

    Object.keys(this.speciesPerLake).forEach(lakeId => {
      let items = this.speciesPerLake[lakeId];
      items.forEach(spl => {
        if (spl.alias) {
          this.speciesPerLakeAliases[lakeId][spl.id] = spl.alias;
        }
      });
    });
  }

  save() {
    BackendService.backendPut(
      "/v1/referential/species-aliases-per-lake",
      {
        targetLakes: this.selectedLakes.map(l => l.id),
        speciesPerLakeAliases: this.speciesPerLakeAliases
      }).then(
      res => {
        this.$buefy.toast.open({
          message: "Espèces par lac enregistrées",
          type: "is-success"
        });
      },
      error => {
        this.$buefy.toast.open({
          message:
            "Erreur lors de l'enregistrement des espèces par lac : " +
            error.message,
          type: "is-danger"
        });
      }
    );
  }

  changeLakeSelection(newSelectedLakeIds: string[]) {
    this.selectedLakes = this.lakes.filter(l => newSelectedLakeIds.indexOf(l.id) > -1);
    localStorage.setItem('lastLakeSelection', JSON.stringify(newSelectedLakeIds));
  }
}
</script>

<style scoped lang="less">

.species-per-lake {
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
}
</style>
