<template>
  <div class="edit-trip-meta page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-meta-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="edit-trip-meta-content">
        <h1>Information de pêche</h1>
        <div class="form">
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
        </div>
        <div class="bottom-page-spacer"></div>
      </div>
    </div>
    <FisholaFooter button-text="Suivant"
                    v-on:buttonClicked="next"
                    shortcuts="back,step-1-4,giveup"/>
  </div>
</template>

<script lang="ts">

import TripMeta from '@/pojos/TripMeta';

import Lake from '@/pojos/Lake';
import Constants from '@/services/Constants';
import Helpers from '@/pojos/Helpers';
import TripsService from '@/services/TripsService';
import ReferentialService from '@/services/ReferentialService';

import FormInput from '@/components/common/FormInput.vue'
import FormSelect from '@/components/common/FormSelect.vue'

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
    FisholaFooter
  }
})
export default class TripMetaVue extends Vue {
  
  @Prop() id!:string;

  trip:TripMeta = { id:'', mode:'Live', date: new Date(), startedAt: new Date() };

  date:string = '';
  startedAt:string = '';
  finishedAt:string = '';

  dateError:string = '';
  startedAtError:string = '';
  finishedAtError:string = '';
  nameError:string = '';
  lakeIdError:string = '';
  typeError:string = '';

  lakes:Lake[] = [];
  types:any[] = [];

  created() {
    ReferentialService.getLakesAndTripTypes(this.referentialsLoaded);
  }

  tripLoaded(someTrip:TripMeta) {
    console.log("Trip chargé", someTrip);
    this.trip = someTrip;
    if (someTrip.mode == 'Afterwards') {
      if (someTrip.date) {
        this.date = Helpers.formateToDate(someTrip.date);
      }
      if (someTrip.startedAt) {
        this.startedAt = Helpers.formateToTime(someTrip.startedAt);
      }
      if (someTrip.finishedAt) {
        this.finishedAt = Helpers.formateToTime(someTrip.finishedAt);
      }
    }

  }

  referentialsLoaded(ls:Lake[], tts:any[]) {
    ls.forEach((lake) => this.lakes.push(lake));
    tts.forEach((type) => this.types.push(type));
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  next() {
    let hasError = false;
    if (this.trip.lakeId) {
      this.lakeIdError = '';
    } else {
      hasError = true;
      this.lakeIdError = 'Information obligatoire';
    }
    if (this.trip.name) {
      this.nameError = '';
    } else {
      hasError = true;
      this.nameError = 'Information obligatoire';
    }
    if (this.trip.type) {
      this.typeError = '';
    } else {
      hasError = true;
      this.typeError = 'Information obligatoire';
    }

    if (this.trip!.mode == 'Afterwards') {
      if (this.date) {
        this.dateError = '';
        let newDate = new Date(this.date);
        this.trip!.date = newDate;

        if (this.startedAt) {
          this.startedAtError = '';

          let startedAt = Helpers.parseDateTime(this.date, this.startedAt);
          this.trip!.startedAt = startedAt;
        } else {
          this.startedAtError = "Vous devez renseignez l'heure de début";
          hasError = true;
        }

        if (this.finishedAt) {
          this.finishedAtError = '';

          let finishedAt = Helpers.parseDateTime(this.date, this.finishedAt);
          this.trip!.finishedAt = finishedAt;
        } else {
          this.finishedAtError = "Vous devez renseignez l'heure de fin";
          hasError = true;
        }

      } else {
        this.dateError = "Vous devez renseignez la date";
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

  display: flex;
  flex-direction: column;
  justify-content: space-between;

  text-align:center;

  overflow: auto;


  .edit-trip-meta-content {

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
