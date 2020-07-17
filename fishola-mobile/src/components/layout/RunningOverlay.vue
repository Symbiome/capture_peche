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
  <div class="running-overlay">
    <div class="running-overlay-see-button">
      <button v-on:click="goToRunningTrip">
        Voir
      </button>
    </div>
    <div class="running-overlay-bar">
      <div class="left">
        {{label}}
        <Running v-if="live" :negative="true"/>
      </div>
      <div class="right" v-on:click="finish">
        Fin <i class="icon icon-stop"/>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import TripsService from '@/services/TripsService';
import Constants from '@/services/Constants';

import router from '@/router';

import { Component, Prop, Vue } from 'vue-property-decorator';
import TripMain from '@/pojos/TripMain';
import Helpers from '@/services/Helpers';

import Running from '@/components/common/Running.vue'

@Component({
  components: {
    Running
  }
})
export default class RunningOverlay extends Vue {

  label: string = '';
  startedAt: string;

  trip?:TripMain;
  interval?:number;
  live:boolean = true;

  mounted() {
    TripsService.getRunningTrip()
      .then(this.tripLoaded);
  }

  beforeDestroy() {
    clearInterval(this.interval);
  }

  tripLoaded(trip:TripMain) {
    this.trip = trip;

    this.live = trip.mode == 'Live';
    if (this.live) {
      this.startedAt = trip.startedAt;
      this.computeDuration();
      this.interval = setInterval(this.computeDuration, 1000);
    } else {
      this.label = trip.name;
    }

  }

  computeDuration() {
    this.label = Helpers.renderDurationNoSeconds(this.startedAt);
  }

  goToRunningTrip() {
    router.push({name:'trip-catchs', params: {id: Constants.RUNNING_ID}});
  }

  finish() {
    TripsService.finishTripCatchs(this.trip!, this.tripSaved);
  }

  tripSaved() {
    router.push({name:'trip-summary', params: {id: Constants.RUNNING_ID}});
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.running-overlay {
  position: relative;
  width: 100%;

  display: flex;
  flex-direction: column;
  align-items: center;

  .running-overlay-see-button {
    position: absolute;
    top: -20px;
    z-index: 10;

    height: 41px;
    width: fit-content;

    display: flex;
    justify-content: center;

    z-index: 10;

    div {
      height: 41px;
    }

    button {
      height: 100%;
      width: 100%;
      border-radius: 22px;

      font-style: normal;
      font-weight: bold;
      font-size: @fontsize-button;
      line-height: calc(@fontsize-button + @line-height-padding-x-large);

      color: @white;
      background-color: @terra-cotta;

      border: 0px;
      padding-left: @margin-medium;
      padding-right: @margin-medium;

    }

  }

  .running-overlay-bar {
    // TODO responsive
    height: 76px;
    background-color: @cyprus;
    font-size: @fontsize-button;
    line-height: calc(@fontsize-button + @line-height-padding-x-large);
    color: @white;
    width: 100%;

    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;

    .right, .left {
      height: fit-content;
      text-align: center;
      margin-left: @margin-large;
      margin-right: @margin-large;
    }
  }
}

</style>
