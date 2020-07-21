<template>
  <div class="authorized-samples">
    <h1>Autorisations de prélèvement</h1>
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
              <b-checkbox v-model="authorizedSamplesMap[l.id][s.id]"></b-checkbox>
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
  </div>
</template>

<script lans="ts">

import { Component, Prop, Vue } from 'vue-property-decorator'

import BackendService from '@/services/BackendService.ts';

@Component
export default class AuthorizedSamplesVue extends Vue {

    lakes = [];
    species = [];
    speciesPerLake = {};
    authorizedSamplesMap = {};

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

      this.lakes.forEach((l) => {
        this.authorizedSamplesMap[l.id] = {};
      });

      Object.keys(this.speciesPerLake).forEach((lakeId) => {
        let items = this.speciesPerLake[lakeId];
        items.forEach((spl) => {
          this.authorizedSamplesMap[lakeId][spl.id] = spl.authorizedSample;
        });
      });
    }

    save() {
      BackendService.backendPut("/v1/referential/authorized-samples", this.authorizedSamplesMap)
        .then(
          (res) => {
              this.$buefy.toast.open({
                  message:'Autorisations de prélèvement enregistrées',
                  type: 'is-success'
              });
          },
          (error) => {
              this.$buefy.toast.open({
                  message: 'Erreur lors de l\'enregistrement des autorisations de prélèvement : ' + error.message,
                  type: 'is-danger'
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

}

</style>
