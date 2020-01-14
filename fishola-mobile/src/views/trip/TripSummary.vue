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
                      v-model="trip.name"
                      v-bind:error="nameError" />
          <FormSelect name="lake"
                      label="Lac"
                      v-bind:options="allLakes"
                      v-model="trip.lakeId"
                      v-bind:error="lakeIdError"/>
          <FormInput name="date"
                      label="Date"
                      type="date"
                      v-model="date"
                      v-bind:error="dateError"/>
          <FormInput name="startAt"
                      label="Heure de début"
                      type="time"
                      v-model="startedAt"
                      v-bind:error="startedAtError"/>
          <FormInput name="finishedat"
                      label="Heure de fin"
                      type="time"
                      v-model="finishedAt"
                      v-bind:error="finishedAtError"/>
          <FormSelect name="weather"
                      label="Météo"
                      v-bind:options="allWeathers"
                      v-model="trip.weatherId"
                      v-bind:error="weatherIdError"/>
          <FormMultiValues name="species"
                           v-bind:label="speciesLabel"
                           v-bind:values="species"/>
          <FormMultiValues name="technics"
                           v-bind:label="technicsLabel"
                           v-bind:values="technics"/>
          <FormMultiValues name="type"
                           label="Type de pêche"
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
import TripSummary from '@/pojos/TripSummary';
import Lake from '@/pojos/Lake';
import Weather from '@/pojos/Weather';
import Species from '@/pojos/Species';
import Constants from '@/services/Constants';
import Helpers from '@/pojos/Helpers';
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
export default class TripSummaryVue extends Vue {

  @Prop() id!:string;

  trip?:TripSummary = { id:'', name:'',  mode:'Live', startedAt: new Date(), lakeId:'', date: new Date(), type:'Craft', speciesIds:[] };

  date:string = '';
  startedAt:string = '';
  finishedAt:string = '';

  dateError:string = '';
  startedAtError:string = '';
  finishedAtError:string = '';
  nameError:string = '';
  lakeIdError:string = '';
  weatherIdError:string = '';

  species:string[] = [];
  speciesLabel:string = 'Espèce recherchée';
  technics:string[] = [];
  technicsLabel:string = 'Technique utilisée';
  types:string[] = [];

  allLakes:Lake[] = [];
  allSpecies:Map<string, Species[]> = new Map();
  allWeathers:Weather[] = [];
  allTripTypes:any[] = [];

  created() {
    ReferentialService.getLakesWeathersTripTypesAndSpecies(this.referentialsLoaded);
  }

  mounted() {
  }

  tripLoaded(someTrip:TripSummary) {
    console.log("Trip chargé", someTrip);
    this.trip = someTrip;
    if (someTrip.date) {
      this.date = Helpers.formateToDate(someTrip.date);
    }
    if (someTrip.startedAt) {
      this.startedAt = Helpers.formateToTime(someTrip.startedAt);
    }
    if (someTrip.finishedAt) {
      this.finishedAt = Helpers.formateToTime(someTrip.finishedAt);
    }

    let speciesPerLake = this.allSpecies.get(someTrip.lakeId);
    someTrip.speciesIds.forEach((speciesId:string) => {
      speciesPerLake!.forEach((s) => {
        if (s.id == speciesId) {
          this.species.push(s.name);
        }
      });
    });
    if (this.species.length > 1) {
      this.speciesLabel = 'Espèces recherchées';
    }

    this.allTripTypes.forEach((tt) => {
      if (tt.id == someTrip.type) {
        this.types.push(tt.name);
      }
    });
  }

  referentialsLoaded(lakes:Lake[], weathers:Weather[], tripTypes:any[], species:Map<string, Species[]>) {
    lakes.forEach((lake) => this.allLakes.push(lake));
    weathers.forEach((weather) => this.allWeathers.push(weather));
    tripTypes.forEach((type) => this.allTripTypes.push(type));
    this.allSpecies = species;
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  send() {
    let hasError = false;

    if (this.trip!.name) {
      this.nameError = '';
    } else {
      hasError = true;
      this.nameError = 'Vous devez nommer la sortie';
    }

    if (this.date) {
      this.dateError = '';
      let newDate = new Date(this.date);
      this.trip!.date = newDate;

      if (this.startedAt) {
        this.startedAtError = '';

        let startedAt = Helpers.parseDateTime(this.date, this.startedAt);
        this.trip!.startedAt = startedAt;
      } else {
        this.startedAtError = "Vous devez renseigner l'heure de début";
        hasError = true;
      }

      if (this.finishedAt) {
        this.finishedAtError = '';

        let finishedAt = Helpers.parseDateTime(this.date, this.finishedAt);
        this.trip!.finishedAt = finishedAt;
      } else {
        this.finishedAtError = "Vous devez renseigner l'heure de fin";
        hasError = true;
      }

    } else {
      this.dateError = "Vous devez renseigner la date";
      hasError = true;
    }

    if (this.trip!.weatherId) {
      this.weatherIdError = '';
    } else {
      hasError = true;
      this.weatherIdError = 'Vous devez préciser la météo';
    }

    if (hasError) {
      this.$root.$emit('toaster-error', 'Vous devez renseigner les champs obligatoires');
    } else {
      TripsService.sendTrip(this.trip!, this.tripSaved);
    }
  }

  tripSaved() {
    router.push('/trips');
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
