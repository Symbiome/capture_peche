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
import Helpers from '../../pojos/Helpers';

@Component
export default class RunningOverlay extends Vue {

  label: string = '';
  startedAt: string;

  trip?:TripMain;
  interval?:number;

  mounted() {
    TripsService.getRunningTrip()
      .then(this.tripLoaded);
  }

  beforeDestroy() {
    clearInterval(this.interval);
  }

  tripLoaded(trip:TripMain) {
    this.trip = trip;

    if (trip.mode == 'Live') {
      this.startedAt = trip.startedAt;
      this.computeDuration();
      this.interval = setInterval(this.computeDuration, 1000);
    } else {
      this.label = trip.name;
    }

  }

  computeDuration() {
    this.label = Helpers.renderDuration(this.startedAt);
  }

  goToRunningTrip() {
    router.push({name:'trip-catchs', params: {id: Constants.RUNNING_ID}});
  }

  finish() {
    TripsService.finishTrip(this.trip!, this.tripSaved);
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
      font-size: 18px;
      line-height: 25px;

      color: @white;
      background-color: @terra-cotta;

      border: 0px;
      padding-left: 20px;
      padding-right: 20px;

    }

  }

  .running-overlay-bar {
    height: 76px;
    background-color: @cyprus;
    font-size: 18px;
    line-height: 25px;
    color: @white;
    width: 100%;

    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;

    .right, .left {
      height: fit-content;
      text-align: center;
      margin-left: 30px;
      margin-right: 30px;
    }
  }
}

</style>
