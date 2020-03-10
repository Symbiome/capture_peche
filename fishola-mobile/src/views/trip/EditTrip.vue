<template>
  <div class="edit-trip page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-page page">
      <SomeTripHeader v-if="ready"
                      v-bind:trip="trip"/>
      <div class="pane">
        <h1>{{duration}}</h1>
        <div class="pane-content-large">
          <div v-if="modifiable"
              class="edit-trip-modifiable-until">
            <i class="icon-edit"/>
            <div>
              Vous avez encore {{modifiableDuration}}<br/>
              pour modifier cette sortie
            </div>
          </div>
          <div class="edit-trip-catchs catch-preview-list-scrollable"
               v-if="modifiable || trip.catchs.length > 0">
            <CatchPreviewList v-if="ready"
                              v-bind:modifiable="modifiable"
                              v-bind:lakeId="trip.lakeId"
                              v-bind:catchs="trip.catchs"
                              v-on:newCatch="newCatch()"
                              v-on:openCatchFromId="openCatch($event)"/>
          </div>
          <div class="edit-trip-no-catch"
               v-if="!modifiable && trip.catchs.length == 0">
            <i class="icon-fish"/>
            <span>Aucune capture</span>
          </div>
          <SomeTripSummary ref="summary"
                           v-if="ready"
                           v-bind:trip="trip"
                           v-bind:readonly="!modifiable"
                           v-on:trip-modified="onUpdatedTrip"
                           v-on:goEditSpecies="goEditSpecies"
                           v-on:goEditTechnics="goEditTechnics"
                           v-on:goEditType="goEditType"
                           class="summary-pane"/>
          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter v-if="ready && modifiable"
                   button-text="Modifier"
                   button-icon="icon-edit"
                   v-on:buttonClicked="startSave"
                   v-on:deleteClicked="deleteTrip"
                   shortcuts="back,spacer,delete"/>
    <FisholaFooter v-if="ready && !modifiable"
                   shortcuts="back,spacer,blank"/>
  </div>
</template>

<script lang="ts">
import {TripBean} from '@/pojos/BackendPojos';

import TripsService from '@/services/TripsService';
import Constants from '@/services/Constants';
import Helpers from '@/pojos/Helpers';

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import SomeTripSummary from '@/components/trip/SomeTripSummary.vue'
import CatchPreviewList from '@/components/trip/CatchPreviewList.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

export type ActionType = "SaveTrip" | "EditSpecies" | "EditTechnics" | "EditType";

@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    SomeTripSummary,
    CatchPreviewList,
    FisholaFooter
  }
})
export default class EditTrip extends Vue {

  @Prop() id!:string;

  actionRequested:ActionType = "SaveTrip";

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
    catchs: [],
    otherSpecies: ''
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

  startSave() {
      // On demande au composant enfant de fournir le modèle mis à jour
      let summaryComponent:any = this.$refs.summary;
      summaryComponent.emitUpdatedTrip();
  }

  onUpdatedTrip(trip:TripBean) {
      // On reçoit le modèle mis à jour, on le sauvegarde
      TripsService.saveTrip(trip, this.tripSaved);
  }

  goEditSpecies() {
    this.actionRequested = "EditSpecies";
    this.startSave();
  }

  goEditTechnics() {
    this.actionRequested = "EditTechnics";
    this.startSave();
  }

  goEditType() {
    this.actionRequested = "EditType";
    this.startSave();
  }

  tripSaved() {
    if (this.actionRequested == "SaveTrip") {
      router.push('/trips');
      this.$root.$emit('ask-for-sync-check');
    } else if (this.actionRequested == "EditSpecies") {
      router.push({name:'trip-species', params: {id: this.id}});
    } else if (this.actionRequested == "EditTechnics") {
      this.$root.$emit('toaster-warning', 'Saved but ... Work in progress');
    } else if (this.actionRequested == "EditType") {
      this.$root.$emit('toaster-warning', 'Saved but ... Work in progress');
    }
  }

  newCatch() {
    if (this.modifiable) {
      router.push({name:'catch', params: {tripId: this.id, catchId:Constants.NEW_CATCH_ID}});
    }
  }

  openCatch(catchId:string) {
    router.push({name:'catch', params: {tripId: this.id, catchId:catchId}});
  }

  deleteTrip() {
    TripsService.deleteTrip(this.id, this.tripDeleted);
  }

  tripDeleted() {
    router.push('/trips');
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

.edit-trip-page {

  h1 {
    color: @gunmetal;
  }

  .edit-trip-no-catch {

    display: flex;
    flex-direction: row;
    justify-content: center;

    margin-bottom: 20px;

    font-size: 12px;
    line-height: 16px;
    color: @pale-sky;

    span {
      margin-left: 5px;
      font-style: italic;
    }

  }

  .edit-trip-catchs {

    display: flex;
    flex-direction: row;
    align-items: center;
    overflow: auto;

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

  .summary-pane {
    padding-left: 30px;
    padding-right: 30px;
  }

}

</style>
