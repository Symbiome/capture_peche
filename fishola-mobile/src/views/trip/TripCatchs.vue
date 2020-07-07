<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #L%
  -->
<template>
  <div class="edit-trip-catchs page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-catchs-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="pane">
        <h1>
          {{duration}}
          <Running v-if="liveRunning"/>
        </h1>
        <div class="pane-content large">
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
              Nouvelle capture
            </button>
          </div>
        </div>
      </div>
    </div>
    <FisholaFooter button-text="Fin de pêche"
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
import Helpers from '@/services/Helpers';

import Running from '@/components/common/Running.vue'
import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import CatchPreviewList from '@/components/trip/CatchPreviewList.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    Running,
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
    console.debug("Trip chargé", someTrip);
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
        this.duration = Helpers.renderDurationNoSeconds(this.trip!.startedAt);
      } else {
        this.duration = Helpers.renderDurationNoSeconds(this.trip!.startedAt, this.trip!.finishedAt);
      }
    }
  }

  finish() {
    if (this.trip!.mode == 'Live') {
      this.computeDuration();
      this.liveRunning = false;
    }
    TripsService.finishTripCatchs(this.trip!, this.tripSaved);
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

    .pane-content {
      text-align:center;

      .catchs-list {
        display: flex;
        flex-direction: row;
        align-items: center;
        overflow-x: auto;
        overflow-y: hidden;

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
        font-size: @fontsize-button;
        line-height: calc(@fontsize-button + 7px);

        color: @pelorous;
        background-color: transparent;

        border: 1px solid @pelorous;
        border-radius: 22px;
        padding-left: 20px;
        padding-right: 20px;

        i {
          margin-right: 5px;
          font-size: @fontsize-button-big;
        }
      }
    }


  }

}

</style>
