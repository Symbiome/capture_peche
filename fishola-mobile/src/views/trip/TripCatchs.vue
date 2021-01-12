<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
      <SomeTripHeader v-bind:trip="trip"
                      class="hide-on-desktop"/>
      <div class="pane">
        <h1 class="hide-on-desktop">
          {{duration}}
          <Running v-if="liveRunning"/>
        </h1>
        <h1 class="hide-on-mobile no-margin-pane">
          <BackButton back-event="backButtonClicked"
                      v-on:backButtonClicked="editSpecies"/>
          {{trip.name}}
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

          <div class="buttons-bar hide-on-mobile">
            <div class="button button-primary">
              <button v-on:click="finish">
                Fin de pêche
              </button>
            </div>
            <div class="button button-secondary">
              <button v-on:click="giveup">
                Abandon
              </button>
            </div>
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

import BackButton from '@/components/common/BackButton.vue'
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
    BackButton,
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
    TripsService.finishTripCatchs(this.trip!).then(this.tripSaved);
  }

  tripSaved() {
    router.push({name:'trip-summary', params: {id: this.id}});
  }

  giveup() {
    Helpers.confirm(this.$modal, 'Voulez-vous vraiment abandonner cette sortie ?')
      .then(this.giveupConfirmed);
  }

  giveupConfirmed() {
    TripsService.cancelCreations();
    router.push('/trips');
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

      @media(max-height:610px) {
        margin-top: calc(@fontsize-title * 1);
        margin-bottom: calc(@fontsize-title * 1);
        line-height: calc(@fontsize-title + @line-height-padding-medium);
      }

    }

    .pane-content {
      text-align:center;

      .catchs-list {
        display: flex;
        flex-direction: row;
        align-items: center;
        overflow-x: auto;
        overflow-y: hidden;

        height: calc(100vw - 120px);

        @media(max-height:610px) {
          height: calc(100vw - 100px);           
        }
      }

    }

    .edit-trip-catchs-new-catch-button {
      margin-top: @vertical-margin-large;
      margin-bottom: @vertical-margin-xx-large;

      @media(max-height:610px) {
        margin-top: @vertical-margin-x-small;
      }

      height: 44px;

      @media(max-height:610px) {
        height:80px;
      }
      width: 100%;

      button {

        height: 44px;
        @media(max-height:610px) {
          height: 35px;
        }
        font-style: normal;
        font-weight: bold;
        font-size: @fontsize-button;
        line-height: calc(@fontsize-button + @line-height-padding-x-large);

        color: @pelorous;
        background-color: transparent;

        border: 1px solid @pelorous;
        border-radius: 22px;
        padding-left: @margin-medium;
        padding-right: @margin-medium;

        i {
          margin-right: @margin-x-small;
          font-size: @fontsize-button-big;
        }
      }
    }


  }


  @media screen and (min-width: @desktop-min-width) {
    .pane {

      h1 {
        color: @pelorous;
      }

      .pane-content {
        .catchs-list {
          height: 200px;
        }
      }
    }
  }


}

</style>
