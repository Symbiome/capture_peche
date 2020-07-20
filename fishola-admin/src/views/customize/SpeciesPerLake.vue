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
              <b-input v-model="speciesPerLakeAliases[l.id][s.id]"
                       placeholder="Nom spécifique"
                       ></b-input>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="buttons">
        <button class="button is-primary" 
            @click="save()">
            Enregistrer
        </button>
    </div>
    <h2>Debug</h2>
    {{speciesPerLakeAliases}}
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
    speciesPerLakeAliases = {};

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
          this.speciesPerLakeAliases[l.id] = {};
        });

        Object.keys(this.speciesPerLake).forEach((lakeId) => {
          let items = this.speciesPerLake[lakeId];
          items.forEach((spl) => {
            if (spl.alias) {
              this.speciesPerLakeAliases[lakeId][spl.id] = spl.alias;
            }
          });
        })

      });
    }

    save() {
      console.log("speciesPerLakeAliases", this.speciesPerLakeAliases);
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
