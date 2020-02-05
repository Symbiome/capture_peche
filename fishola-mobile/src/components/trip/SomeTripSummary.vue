<template>
  <div class="form" v-if="ready">
    <FormInput name="name"
                label="Nom de la sortie"
                placeholder="Nommez votre sortie"
                v-model="trip.name"
                v-bind:error="nameError"
                v-bind:readonly="readonly"/>
    <FormSelect name="lake"
                label="Lac"
                v-bind:options="allLakes"
                v-model="trip.lakeId"
                v-bind:error="lakeIdError"
                v-bind:readonly="readonly"/>
    <FormInput name="date"
                label="Date"
                type="date"
                v-model="date"
                v-bind:error="dateError"
                v-bind:readonly="readonly"/>
    <FormInput name="startAt"
                label="Heure de début"
                type="time"
                v-model="startedAt"
                v-bind:error="startedAtError"
                v-bind:readonly="readonly"/>
    <FormInput name="finishedat"
                label="Heure de fin"
                type="time"
                v-model="finishedAt"
                v-bind:error="finishedAtError"
                v-bind:readonly="readonly"/>
    <FormSelect name="weather"
                label="Météo"
                v-bind:options="allWeathers"
                v-model="trip.weatherId"
                v-bind:error="weatherIdError"
                v-bind:readonly="readonly"/>
    <FormMultiValues name="species"
                      v-bind:label="speciesLabel"
                      v-bind:values="species"
                      v-bind:readonly="readonly"/>
    <FormMultiValues name="technics"
                      v-bind:label="technicsLabel"
                      v-bind:values="technics"
                      v-bind:readonly="readonly"/>
    <FormMultiValues name="type"
                      label="Type de pêche"
                      v-bind:values="types"
                      v-bind:readonly="readonly"/>
  </div>
</template>

<script lang="ts">
import TripSummary from '@/pojos/TripSummary';
import {Lake, Weather, SpeciesWithAlias} from '@/pojos/BackendPojos';

import Constants from '@/services/Constants';
import Helpers from '@/pojos/Helpers';
import ReferentialService from '@/services/ReferentialService';

import FormInput from '@/components/common/FormInput.vue'
import FormSelect from '@/components/common/FormSelect.vue'
import FormMultiValues from '@/components/common/FormMultiValues.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

@Component({
  components: {
    FormInput,
    FormSelect,
    FormMultiValues
  }
})
export default class SomeTripSummary extends Vue {

  @Prop() trip!:TripSummary;
  @Prop() readonly!:boolean;

  // On est obligés de gérer un flag de ce genre, sinon les FormSelect
  // sont créés à vide et ne sélectionnent pas les bonnes valeurs
  ready:boolean = false;

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
  allSpecies:Map<string, SpeciesWithAlias[]> = new Map();
  allWeathers:Weather[] = [];
  allTripTypes:any[] = [];

  created() {
    ReferentialService.getLakesWeathersTripTypesAndSpecies(this.referentialsLoaded);
  }

  mounted() {
  }

  referentialsLoaded(lakes:Lake[], weathers:Weather[], tripTypes:any[], species:Map<string, SpeciesWithAlias[]>) {
    lakes.forEach((lake) => this.allLakes.push(lake));
    weathers.forEach((weather) => this.allWeathers.push(weather));
    tripTypes.forEach((type) => this.allTripTypes.push(type));
    this.allSpecies = species;
    this.tripLoaded(this.trip);
  }

  tripLoaded(someTrip:TripSummary) {

    if (someTrip.date) {
      if (this.readonly) {
        this.date = Helpers.formatToLongDate(someTrip.date);
      } else {
        this.date = Helpers.formatToDate(someTrip.date);
      }
    }
    if (someTrip.startedAt) {
      this.startedAt = Helpers.formatToTime(someTrip.startedAt);
    }
    if (someTrip.finishedAt) {
      this.finishedAt = Helpers.formatToTime(someTrip.finishedAt);
    }

    let speciesPerLake = this.allSpecies.get(someTrip.lakeId);
    someTrip.speciesIds.forEach((speciesId:string) => {
      speciesPerLake!.forEach((s) => {
        if (s.id == speciesId) {
          let speciesDisplayValue = s.alias ? `${s.alias} (${s.name})` : s.name;
          this.species.push(speciesDisplayValue);
        }
      });
    });
    if (this.species.length > 1) {
      this.speciesLabel = 'Espèces recherchées';
    }

    this.technics.push('Work in progress');

    this.allTripTypes.forEach((tt) => {
      if (tt.id == someTrip.type) {
        this.types.push(tt.name);
      }
    });

    this.ready = true;
  }

  emitUpdatedTrip() {
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

        let startedAt = Helpers.parseDateTime(newDate, this.startedAt);
        this.trip!.startedAt = startedAt;
      } else {
        this.startedAtError = "Vous devez renseigner l'heure de début";
        hasError = true;
      }

      if (this.finishedAt) {
        this.finishedAtError = '';

        let finishedAt = Helpers.parseDateTime(newDate, this.finishedAt);
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
      // On émet au parent le modèle mis à jour
      this.$emit('trip-modified', this.trip!);
    }
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

</style>
