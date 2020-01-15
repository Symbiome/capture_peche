<template>
  <div class="edit-trip page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="edit-trip-content">
        <h1>{{duration}}</h1>
        <div v-if="modifiable"
             class="edit-trip-modifiable-until">
          <i class="icon-edit"/>
          <div>
            Vous avez encore {{modifiableDuration}}<br/>
            pour modifier cette sortie
          </div>
        </div>
        <div class="edit-trip-catchs">
          Liste des captures ...
        </div>
        <SomeTripSummary ref="summary"
                         v-if="ready"
                         v-bind:trip="trip"
                         v-bind:readonly="!modifiable"
                         v-on:trip-modified="onUpdatedTrip"/>
        <div class="bottom-page-spacer"></div>
      </div>
    </div>
    <FisholaFooter button-text="Modifier"
                    button-icon="icon-edit"
                    v-on:buttonClicked="saveClicked"
                    shortcuts="back,spacer,delete"/>
  </div>
</template>

<script lang="ts">
import {TripBean} from '@/pojos/BackendPojos';

import TripsService from '@/services/TripsService';
import Helpers from '@/pojos/Helpers';

import FisholaHeader from '@/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import SomeTripSummary from '@/components/trip/SomeTripSummary.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    SomeTripSummary,
    FisholaFooter
  }
})
export default class EditTrip extends Vue {

  @Prop() id!:string;

  // Ce flag permet de ne créer le composant SomeTripSummary que lorsque les données sont prêtes
  ready:boolean = false;

  trip:TripBean = {
    id: '',
    mode: 'Live',
    type: 'Craft',
    name: '',
    lakeId: '',
    speciesIds: [],
    date: new Date(),
    startedAt: new Date(),
    finishedAt: new Date(),
    weatherId: '',
    catchs: []
  };

  duration:string = '';
  modifiable:boolean = false;
  modifiableDuration:string = '';

  created() {
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  mounted() {
  }

  tripLoaded(someTrip:TripBean) {
    this.trip = someTrip;
    if (this.trip.modifiableUntil) {
      this.modifiable = true;
      this.modifiableDuration = Helpers.computeDurationTrunced(new Date(), this.trip.modifiableUntil);
    }
    this.ready = true;
    this.duration = Helpers.computeDuration(this.trip.startedAt, this.trip.finishedAt);
  }

  saveClicked() {
      // On demande au composant enfant de fournir le modèle mis à jour
      let summaryComponent:any = this.$refs.summary;
      summaryComponent.emitUpdatedTrip();
  }

  onUpdatedTrip(trip:TripBean) {
      // On reçoit le modèle mis à jour, on le sauvegarde
      TripsService.sendTrip(trip, this.tripSaved);
  }

  tripSaved() {
    router.push('/trips');
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

.edit-trip-page {

  display: flex;
  flex-direction: column;
  justify-content: space-between;

  text-align:center;

  overflow: auto;


  .edit-trip-content {

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
      color: @gunmetal;
      text-align: center;
    }

    .edit-trip-catchs {

      display: flex;
      flex-direction: column;
      justify-content: center;

      color: @white;
      background-color: @cyprus;
      opacity: 0.8;
      border-radius: 8px;

      height: 200px;
      margin-bottom: 20px;
    }

    .edit-trip-modifiable-until {

      display: flex;
      flex-direction: row;
      justify-content: center;
      align-items: center;

      color: @terra-cotta;
      margin-bottom: 30px;

      i {
        font-size: 20px;
      }
      div {
        margin-left: 10px;
        text-align: left;
      }

    }
  }

}

</style>
