<template>
  <div class="edit-trip-summary page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-summary-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="edit-trip-summary-content">
        <h1>Fin de pêche</h1>
        <div class="form">
          <FormInput name="name"
                      label="Nom de la sortie"
                      placeholder="Nommez votre sortie"
                      v-model="name"
                      v-bind:error="nameError" />
          <FormSelect name="lake"
                      label="Lac"
                      v-bind:options="lakes"
                      v-model="lakeId"
                      v-bind:error="lakeIdError"/>
          <FormInput name="date"
                      label="Date"
                      v-model="date"/>
          <FormInput name="startAt"
                      label="Heure de début"
                      v-model="startedAt"/>
          <FormInput name="finishedat"
                      label="Heure de fin"
                      v-model="finishedAt"/>
          <FormSelect name="weather"
                      label="Météo"
                      v-bind:options="weathers"
                      v-model="weatherId"
                      v-bind:error="weatherIdError"/>
          <FormMultiValues name="species"
                           label="Espèce recherchée"
                           v-bind:values="species"/>
          <FormMultiValues name="technics"
                           label="Technique utilisée"
                           v-bind:values="technics"/>
          <FormMultiValues name="type"
                           label="Situation"
                           v-bind:values="types"/>
        </div>
        <div class="bottom-page-spacer"></div>
      </div>
    </div>
    <FisholaFooter button-text="Envoyer"
                    button-icon="icon-send"
                    v-on:buttonClicked="send"
                    shortcuts="back,step-4-4,giveup"/>
  </div>
</template>

<script lang="ts">
import Trip from '@/pojos/Trip';
import Lake from '@/pojos/Lake';
import Weather from '@/pojos/Weather';
import Constants from '@/services/Constants';
import TripsService from '@/services/TripsService';
import ReferentialService from '@/services/ReferentialService';

import FormInput from '@/components/common/FormInput.vue'
import FormSelect from '@/components/common/FormSelect.vue'
import FormMultiValues from '@/components/common/FormMultiValues.vue'

import FisholaHeader from '@/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    FormInput,
    FormSelect,
    FormMultiValues,
    FisholaFooter
  }
})
export default class EditTripSummary extends Vue {

  @Prop() id!:string;

  trip?:Trip = new Trip();

  name:string = '';
  nameError:string = '';
  lakeId:string = '';
  lakeIdError:string = '';
  date:string = '';
  startedAt:string = '';
  finishedAt:string = '';
  weatherId:string = '';
  weatherIdError:string = '';

  species:string[] = [];
  technics:string[] = [];
  types:string[] = [];

  lakes:Lake[] = [];
  weathers:Weather[] = [];

  created() {
    TripsService.getTrip(this.id, this.tripLoaded);
    ReferentialService.getLakes(this.lakesLoaded);
    ReferentialService.getWeathers(this.weathersLoaded);
  }

  mounted() {
  }

  tripLoaded(someTrip:any) {
    console.log("Trip chargé", someTrip);
    this.trip = someTrip;
    this.name = someTrip.name;
    this.lakeId = someTrip.lakeId;
    var dayOptions = {weekday: "long", month: "long", day: "numeric", year: "numeric"};
    this.date = someTrip.date.toLocaleDateString('fr-FR', dayOptions);
    var hourOptions = {hour: "numeric", minute:"numeric"};
    this.startedAt = someTrip.startedAt.toLocaleTimeString('fr-FR', hourOptions);
    this.finishedAt = someTrip.finishedAt.toLocaleTimeString('fr-FR', hourOptions);

    this.species = someTrip.speciesIds;
    this.types.push(someTrip.type);
  }

  lakesLoaded(result:Lake[]) {
    result.forEach((lake) => this.lakes.push(lake));
  }

  weathersLoaded(result:Weather[]) {
    result.forEach((wearther) => this.weathers.push(wearther));
  }

  send() {
    this.$root.$emit('toaster-warning', 'Work in progress');
  }

  tripSaved() {
    router.push({name:'edit-trip-species', params: {id: this.id}});
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

.edit-trip-summary-page {

  display: flex;
  flex-direction: column;
  justify-content: space-between;

  text-align:center;

  overflow: auto;


  .edit-trip-summary-content {

    flex:auto;

    display: flex;
    flex-direction: column;
    justify-content: flex-start;

    text-align:center;

    background-color: @white-smoke;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    padding-left: 30px;
    padding-right: 30px;
    padding-top: 30px;

    color: @gunmetal;

    h1 {
      margin-top: 0px;
      margin-bottom: 30px;
      height: 30px;
      font-style: normal;
      font-weight: normal;
      font-size: 22px;
      line-height: 30px;
      color: @pelorous;
      text-align: center;
    }

  }

}

</style>
