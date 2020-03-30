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
    <FormSelect name="type"
                label="Type de pêche"
                v-bind:options="allTripTypes"
                v-model="trip.type"
                v-bind:readonly="readonly"/>
    <FormMultiValues name="species"
                      v-bind:label="speciesLabel"
                      v-bind:values="species"
                      v-bind:readonly="readonly"
                      v-on:clicked="$emit('goEditSpecies')"/>
    <FormMultiValues name="techniques"
                      v-bind:label="techniquesLabel"
                      v-bind:values="techniques"
                      v-bind:readonly="readonly"
                      v-on:clicked="$emit('goEditTechniques')"/>

  </div>
</template>

<script lang="ts">
import TripSummary from '@/pojos/TripSummary';
import {Lake, Weather, SpeciesWithAlias, Technique} from '@/pojos/BackendPojos';

import Constants from '@/services/Constants';
import Helpers from '@/pojos/Helpers';
import {LakesWeathersTripTypesSpeciesAndTechniques} from '@/services/ReferentialService';
import ReferentialService from '@/services/ReferentialService';

import FormInput from '@/components/common/FormInput.vue'
import FormSelect from '@/components/common/FormSelect.vue'
import FormMultiValues from '@/components/common/FormMultiValues.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';
import moment from 'moment';

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
  techniques:string[] = [];
  techniquesLabel:string = 'Technique utilisée';
  types:string[] = [];

  allLakes:Lake[] = [];
  allSpecies:Map<string, SpeciesWithAlias[]> = new Map();
  allWeathers:Weather[] = [];
  allTripTypes:any[] = [];
  allTechniques:Technique[] = [];

  created() {
    ReferentialService.getLakesWeathersTripTypesSpeciesAndTechniques()
      .then(this.referentialsLoaded);
  }

  mounted() {
  }

  referentialsLoaded(data:LakesWeathersTripTypesSpeciesAndTechniques) {
    data.lakes.forEach((lake) => this.allLakes.push(lake));
    data.weathers.forEach((weather) => this.allWeathers.push(weather));
    data.tripTypes.forEach((type) => this.allTripTypes.push(type));
    data.techniques.forEach((technique) => this.allTechniques.push(technique));
    this.allSpecies = data.species;
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
      this.startedAt = Helpers.truncateTimeToMinutes(someTrip.startedAt);
    }
    if (someTrip.finishedAt) {
      this.finishedAt = Helpers.truncateTimeToMinutes(someTrip.finishedAt);
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
    if (someTrip.otherSpecies) {
      this.species.push(someTrip.otherSpecies);
    }
    if (this.species.length > 1) {
      this.speciesLabel = 'Espèces recherchées';
    }

    if (someTrip.techniqueIds) {
      this.allTechniques.forEach((technique:Technique) => {
        someTrip.techniqueIds.forEach((techniqueId) => {
          if (techniqueId == technique.id) {
            this.techniques.push(technique.name);
          }
        });
      });
    }
    if (this.techniques.length > 1) {
      this.techniquesLabel = 'Techniques utilisées';
    }

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

      let newDateSOD = moment(newDate).startOf('day');
      let nowSOD = moment().startOf('day');
      if (newDateSOD.isAfter(nowSOD)) {
        this.dateError = "La date ne peut être dans le futur";
        hasError = true;
      }

      if (this.startedAt) {
        this.startedAtError = '';
        this.trip!.startedAt = this.startedAt;
      } else {
        this.startedAtError = "Vous devez renseigner l'heure de début";
        hasError = true;
      }

      if (this.finishedAt) {

        let startedAtMoment = moment(this.startedAt, moment.HTML5_FMT.TIME_SECONDS);
        let finishedAtMoment = moment(this.finishedAt, moment.HTML5_FMT.TIME_SECONDS);

        if (finishedAtMoment.isAfter(startedAtMoment)) {
          this.finishedAtError = '';
          this.trip!.finishedAt = this.finishedAt;
        } else {
          this.finishedAtError = "Doit être après l'heure de début";
          hasError = true;
        }

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
