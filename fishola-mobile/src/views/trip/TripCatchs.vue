<template>
  <div class="edit-trip-catchs page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-catchs-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="pane">
        <h1>{{duration}}</h1>
        <div class="pane-content-large">
          <div class="catchs-list catch-preview-list-scrollable">
            <CatchPreviewList v-if="ready"
                              v-bind:modifiable="true"
                              v-bind:lakeId="trip.lakeId"
                              v-bind:catchs="trip.catchs"
                              v-on:newCatch="newCatch()"
                              v-on:openCatchFromId="openCatch($event)"/>
          </div>
          <div class="edit-trip-catchs-new-catch-button">
            <button v-on:click="newCatch">
              <i class="icon-fish"/>
              Capture
            </button>
          </div>
        </div>
      </div>
    </div>
    <FisholaFooter button-text="Terminer"
                   v-on:buttonClicked="finish"
                   shortcuts="back,step-3-4,giveup"
                   back-event="onBackButton"
                   v-on:onBackButton="editSpecies"/>
  </div>
</template>

<script lang="ts">
import TripMain from '@/pojos/TripMain';
import CatchSummary from '@/pojos/CatchSummary';
import Constants from '@/services/Constants';
import TripsService from '@/services/TripsService';
import ReferentialService from '@/services/ReferentialService';

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import CatchPreviewList from '@/components/trip/CatchPreviewList.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';
import Helpers from '../../pojos/Helpers';

@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    CatchPreviewList,
    FisholaFooter
  }
})
export default class TripCatchsView extends Vue {

  @Prop() id!:string;

  trip?:TripMain = { id:'', name:'', mode:'Live', startedAt: '', catchs:[] };

  duration?:string = '';
  liveRunning:boolean = false;

  ready:boolean = false;

  interval?:number;

  created() {
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  mounted() {
  }

  tripLoaded(someTrip:TripMain) {
    console.log("Trip chargé", someTrip);
    this.trip = someTrip;

    if (this.trip.mode == 'Live') {
      this.computeDuration();
      if (this.id == Constants.RUNNING_ID) {
        this.liveRunning = true;
        this.interval = setInterval(this.computeDuration, 1000);
      }
    }

    this.ready = true;
  }

  beforeDestroy() {
    clearInterval(this.interval);
  }

  editSpecies() {
    router.push({name:'trip-species', params: {id: this.id}});
  }

  computeDuration() {
    if (this.trip! && this.trip!.startedAt) {
      let seconds;
      if (this.id == Constants.RUNNING_ID) {
        this.duration = Helpers.renderDuration(this.trip!.startedAt);
      } else {
        this.duration = Helpers.renderDuration(this.trip!.startedAt, this.trip!.finishedAt);
      }
    }
  }

  finish() {
    if (this.trip!.mode == 'Live') {
      this.computeDuration();
      this.liveRunning = false;
    }
    TripsService.finishTrip(this.trip!, this.tripSaved);
  }

  tripSaved() {
    router.push({name:'trip-summary', params: {id: this.id}});
  }

  newCatch() {
    router.push({name:'catch', params: {tripId: this.id, catchId:Constants.NEW_CATCH_ID}});
  }

  openCatch(catchId:string) {
    router.push({name:'catch', params: {tripId: this.id, catchId:catchId}});
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

.edit-trip-catchs-page {

  .pane {

    h1 {
      color: @gunmetal;
    }

    .pane-content-large {
      text-align:center;

      .catchs-list {
        display: flex;
        flex-direction: row;
        align-items: center;
        overflow: auto;

        height: calc(100vw - 80px);

      }

    }

    .edit-trip-catchs-new-catch-button {
      margin-top: 30px;
      margin-bottom: 50px;

      height: 44px;
      width: 100%;

      button {

        height: 44px;
        font-style: normal;
        font-weight: bold;
        font-size: 18px;
        line-height: 25px;

        color: @pelorous;
        background-color: transparent;

        border: 1px solid @pelorous;
        border-radius: 22px;
        padding-left: 20px;
        padding-right: 20px;

        i {
          margin-right: 5px;
          font-size: 20px;
        }
      }
    }


  }

}

</style>
