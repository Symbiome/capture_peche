<template>
  <div class="species-per-lake">
    <h1>Espèces par lac</h1>
    <table class="table is-striped">
      <thead>
        <tr>
          <th></th>
          <th v-for="l in lakes" v-bind:key="l.id">{{l.name}}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="s in species" v-bind:key="s.id">
          <th>{{s.name}}</th>
          <td v-for="l in lakes" v-bind:key="l.id">
            <div class="field">
              <b-checkbox v-model="speciesPerLakeIndex[l.id][s.id].selected"></b-checkbox>
              <b-input v-model="speciesPerLakeIndex[l.id][s.id].alias" placeholder="Nom spécifique"></b-input>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="buttons">
        <!-- Creation button (only displayed if createElement is defined) -->
        <button class="button is-primary" 
            @click="save()">
            Enregistrer
        </button>
    </div>
    <h2>Debug</h2>
    {{speciesPerLakeIndex}}
  </div>
</template>

<script lans="ts">

import { Component, Prop, Vue } from 'vue-property-decorator'

import BackendService from '@/services/BackendService.ts';

@Component
export default class SpeciesPerLakeVue extends Vue {

    lakes = [];
    species = [];
    speciesPerLake = {};
    speciesPerLakeIndex = {};

    created() {

      Promise.all([
        BackendService.backendGet("/v1/referential/lakes"),
        BackendService.backendGet("/v1/referential/species"),
        BackendService.backendGet("/v1/referential/species-per-lake")
      ]).then(data => {
        this.lakes = data[0];
        this.species = data[1];
        this.speciesPerLake = data[2];

        this.lakes.forEach((l) => {
          this.speciesPerLakeIndex[l.id] = {};
          this.species.forEach((s) => {
            this.speciesPerLakeIndex[l.id][s.id] = {
              selected: false,
              alias:''
            };
          });
        });

        Object.keys(this.speciesPerLake).forEach((lakeId) => {
          let items = this.speciesPerLake[lakeId];
          items.forEach((spl) => {
            this.speciesPerLakeIndex[lakeId][spl.id].selected = true;
            this.speciesPerLakeIndex[lakeId][spl.id].alias = spl.alias;
          });
        })

      });
    }
}
</script>

<style scoped lang="less">

@import "../../less/main";

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

h2 {
  font-size: 24px;
}

</style>
