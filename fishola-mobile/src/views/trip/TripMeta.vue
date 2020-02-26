<template>
  <div class="edit-trip-meta page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-meta-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="pane">
        <h1>Information de pêche</h1>
        <div class="pane-content">
          {{hereIAm}}
          {{hereIAmError}}
          <FormInput name="name"
                      label="Nom de la sortie"
                      placeholder="Nommez votre sortie"
                      v-model="trip.name"
                      v-bind:error="nameError" />
          <FormSelect name="lake"
                      label="Lac"
                      v-bind:options="lakes"
                      v-model="trip.lakeId"
                      v-bind:error="lakeIdError"/>
          <FormSelect name="type"
                      label="Type de pêche"
                      v-bind:options="types"
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
import Helpers from '@/pojos/Helpers';
import TripsService from '@/services/TripsService';
import {LakesAndTripTypes} from '@/services/ReferentialService';
import ReferentialService from '@/services/ReferentialService';

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
export default class TripMetaVue extends Vue {
  
  @Prop() id!:string;

  trip:TripMeta = { id:'', mode:'Live', date: new Date(), startedAt: new Date() };

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

    // if (navigator) {
    //   console.log("navigator", navigator);
    //   if (navigator.geolocation) {
    //     console.log("navigator.geolocation", navigator.geolocation);
    //     if (navigator.geolocation.getCurrentPosition) {
    //       navigator.geolocation.getCurrentPosition(
    //         (position, someError) => {
    //           console.log("someError", someError);
    //           this.hereIAmJson = JSON.strigify(position);
    //           this.hereIAm = position;
    //         },
    //         (error) => {
    //           console.log("error", error);
    //         });
    //     } else {
    //       this.hereIAmJson = "navigator.geolocation.getCurrentPosition undefined";
    //     }

    //     // if (navigator.geolocation.watchPosition) {
    //     //   navigator.geolocation.watchPosition((position) => {
    //     //     this.hereIAmJson = JSON.strigify(position);
    //     //     this.hereIAm = position;
    //     //   });
    //     // } else {
    //     //   this.hereIAmJson = "navigator.geolocation.getCurrentPosition undefined";
    //     // }
    //   } else {
    //     this.hereIAmJson = "navigator.geolocation undefined";
    //     console.log("navigator", navigator);
    //   }
    // } else {
    //   this.hereIAmJson = "navigator undefined";
    // }

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(this.displayLocationInfo, this.handleLocationError);
    }

  }

  displayLocationInfo(position:any) {
    const lng = position.coords.longitude;
    const lat = position.coords.latitude;

    this.hereIAm  = `longitude: ${ lng } | latitude: ${ lat }`;
    console.log(this.hereIAm);
  }

  handleLocationError(error:any) {
    window.alert(error);
    window.alert(error.code);
    switch (error.code) {
      case 3:
        // ...deal with timeout
        this.hereIAmError = "3: deal with timeout";
        break;
      case 2:
        // ...device can't get data
        this.hereIAmError = "2: device can't get data";
        break;
      case 1:
        // ...user said no ☹️
        this.hereIAmError = "1: user said no";
    }
  }

  tripLoaded(someTrip:TripMeta) {
    console.log("Trip chargé", someTrip);
    this.trip = someTrip;
    if (someTrip.mode == 'Afterwards') {
      if (someTrip.date) {
        this.date = Helpers.formatToDate(someTrip.date);
      }
      if (someTrip.startedAt) {
        this.startedAt = Helpers.formatToTime(someTrip.startedAt);
      }
      if (someTrip.finishedAt) {
        this.finishedAt = Helpers.formatToTime(someTrip.finishedAt);
      }
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

.edit-trip-meta-page {

}

</style>
