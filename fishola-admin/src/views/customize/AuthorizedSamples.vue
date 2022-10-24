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
    <h1>Autorisations de prélèvement et taille des maillages</h1>
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
            <div class="field specie-container">
              <b-checkbox
                v-model="authorizedSamplesMap[l.id][s.id]"
                @input="$forceUpdate()"
              >
              </b-checkbox>
              <b-input
                v-if="authorizedSamplesMap[l.id][s.id]"
                class="minsize-input"
                type="number"
                v-model="minSizeMap[l.id][s.id]"
              >
              </b-input>
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
import { Component, Prop, Vue } from "vue-property-decorator";

import BackendService from "@/services/BackendService.ts";

@Component
export default class AuthorizedSamplesVue extends Vue {
  lakes = [];
  species = [];
  speciesPerLake = {};
  authorizedSamplesMap = {};
  minSizeMap = {};

  created() {
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
  }

  save() {
    BackendService.backendPut(
      "/v1/referential/authorized-samples",
      this.authorizedSamplesMap
    ).then(
      res => {
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
}
</script>

<style scoped lang="less">
@import "../../less/main";

.authorized-samples {
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
}
</style>
