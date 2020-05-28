<template>
  <div class="edit-trip-meta page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-meta-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="pane">
        <div class="pane-content rounded">
          <h1>Information de pêche</h1>
          <FormInput name="name"
                      label="Nom de la sortie"
                      placeholder="Nommez votre sortie"
                      v-model="trip.name"
                      v-bind:error="nameError" />
          <FormSelect name="lake"
                      label="Lac"
                      v-bind:options="lakes"
                      orderBy="name"
                      v-model="trip.lakeId"
                      v-bind:error="lakeIdError"/>
          <FormSelect name="type"
                      label="Type de pêche"
                      v-bind:options="types"
                      orderBy="name"
                      v-model="trip.type"
                      v-bind:error="typeError" />
          <div v-if="trip.mode == 'Afterwards'">
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
          </div>
          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter button-text="Suivant"
                    v-on:buttonClicked="next"
                    shortcuts="back,step-1-4,giveup"/>
  </div>
</template>

<script lang="ts">

import TripMeta from '@/pojos/TripMeta';

import {Lake} from '@/pojos/BackendPojos';
import Constants from '@/services/Constants';
import Helpers from '@/services/Helpers';
import TripsService from '@/services/TripsService';
import {LakesAndTripTypes} from '@/services/ReferentialService';
import ReferentialService from '@/services/ReferentialService';
import {CoordsAndLake} from '@/services/GeolocationService';
import GeolocationService from '@/services/GeolocationService';

import FormInput from '@/components/common/FormInput.vue'
import FormSelect from '@/components/common/FormSelect.vue'

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';
  
@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    FormInput,
    FormSelect,
    FisholaFooter
  }
})
export default class TripMetaView extends Vue {
  
  @Prop() id!:string;

  trip:TripMeta = { id:'', mode:'Live', date: new Date(), startedAt: '' };

  date:string = '';
  startedAt:string = '';
  finishedAt:string = '';
  
  hereIAm:string = '';
  hereIAmError:string = '';

  dateError:string = '';
  startedAtError:string = '';
  finishedAtError:string = '';
  nameError:string = '';
  lakeIdError:string = '';
  typeError:string = '';

  lakes:Lake[] = [];
  types:any[] = [];

  created() {
    ReferentialService.getLakesAndTripTypes().then(this.referentialsLoaded);
  }

  tripLoaded(someTrip:TripMeta) {
    console.debug("Trip chargé", someTrip);
    this.trip = someTrip;
    if (someTrip.mode == 'Afterwards') {
      if (someTrip.date) {
        this.date = Helpers.formatToDate(someTrip.date);
      }
      if (someTrip.startedAt) {
        this.startedAt = someTrip.startedAt;
      }
      if (someTrip.finishedAt) {
        this.finishedAt = someTrip.finishedAt;
      }
    }

    if (this.id == Constants.NEW_TRIP_ID) {
      GeolocationService.getClosestLake()
        .then(
          (coordsAndLake:CoordsAndLake) => {
            let lake = coordsAndLake.lake;
            console.debug("Le lac le plus proche est ", lake);
            this.trip.lakeId = lake.id;
            // Les lignes suivantes sont une bidouille pour que le Select s'affiche .......
            this.lakeIdError = lake.id;
            this.lakeIdError = '';

            if (this.trip.mode == 'Live') {
              this.trip.beginLatitude = coordsAndLake.latitude;
              this.trip.beginLongitude = coordsAndLake.longitude;
              console.info(`Coordonnées de début de sortie : ${this.trip.beginLatitude},${this.trip.beginLongitude}`);
            }
          },
          (e) => {
            console.error("Impossible de récupérer les coordonnées", e);
          }
        );
    }

  }

  referentialsLoaded(data:LakesAndTripTypes) {
    data.lakes.forEach((lake:Lake) => this.lakes.push(lake));
    data.tripTypes.forEach((type:any) => this.types.push(type));
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  next() {
    let hasError = false;
    if (this.trip.lakeId) {
      this.lakeIdError = '';
    } else {
      hasError = true;
      this.lakeIdError = 'Vous devez sélectionner le lac';
    }
    if (this.trip.name) {
      this.nameError = '';
    } else {
      hasError = true;
      this.nameError = 'Vous devez nommer la sortie';
    }
    if (this.trip.type) {
      this.typeError = '';
    } else {
      hasError = true;
      this.typeError = 'Vous devez spécifier le type de pêche';
    }

    if (this.trip!.mode == 'Afterwards') {
      if (this.date) {
        this.dateError = '';
        let newDate = new Date(this.date);
        this.trip!.date = newDate;

        if (this.startedAt) {
          this.startedAtError = '';

          // let startedAt = Helpers.parseDateTime(newDate, this.startedAt);
          this.trip!.startedAt = this.startedAt;
        } else {
          this.startedAtError = "Vous devez renseigner l'heure de début";
          hasError = true;
        }

        if (this.finishedAt) {
          this.finishedAtError = '';

          // let finishedAt = Helpers.parseDateTime(newDate, this.finishedAt);
          this.trip!.finishedAt = this.finishedAt;
        } else {
          this.finishedAtError = "Vous devez renseigner l'heure de fin";
          hasError = true;
        }

      } else {
        this.dateError = "Vous devez renseigner la date";
        hasError = true;
      }
    }

    if (hasError) {
      this.$root.$emit('toaster-error', 'Vous devez renseigner les champs obligatoires');
    } else {
      // this.trip!.name = this.name;
      // this.trip!.lakeId = this.lakeId;
      // this.trip!.type = this.type;

      TripsService.saveTripMeta(this.trip!, this.tripSaved);
    }
  }

  tripSaved() {
    router.push({name:'trip-species', params: {id: this.id}});
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

</style>
