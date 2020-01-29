<template>
  <div class="edit-trip-catchs page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-catchs-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="pane">
        <h1 v-if="duration">{{duration}}</h1>
        <div class="pane-content">
          <div v-if="!trip.catchs || trip.catchs.length == 0" class="no-catch">
            <div class="new-catch-square-button" v-on:click="newCatch">
              <i class="pastille icon-plus"/>
            </div>
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
import Constants from '@/services/Constants';
import TripsService from '@/services/TripsService';
import ReferentialService from '@/services/ReferentialService';

import FisholaHeader from '@/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    FisholaFooter
  }
})
export default class TripCatchs extends Vue {

  @Prop() id!:string;

  trip?:TripMain = { id:'', mode:'Live', startedAt: new Date(), catchs:[] };

  duration?:string = '';
  liveRunning:boolean = false;

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
      if (!this.trip.finishedAt) {
        this.liveRunning = true;
        setInterval(this.computeDuration, 1000);
      }
    }
  }

  editSpecies() {
    router.push({name:'trip-species', params: {id: this.id}});
  }

  computeDuration() {
    if (this.trip!) {
      let startedAt = this.trip!.startedAt;
      if (startedAt) {
        let end = this.trip!.finishedAt || new Date();
        let seconds = Math.floor((end.getTime()-startedAt.getTime())/1000);
        let minutes = Math.floor(seconds/60);
        let hours = Math.floor(minutes/60);
        let result = '';
        if (hours > 0) {
          result += hours + 'h ';
          minutes -= hours * 60;
        }
        if (minutes > 0) {
          result += minutes + 'min ';
          seconds -= hours * 60*60 + minutes * 60;
        }
        result += seconds + 's';
        this.duration = result;
      }
    }
  }

  finish() {
    if (this.trip!.mode == 'Live') {
      this.trip!.finishedAt = new Date();
      this.computeDuration();
      this.liveRunning = false;
    }
    TripsService.saveTripMain(this.trip!, this.tripSaved);
  }

  tripSaved() {
    router.push({name:'trip-summary', params: {id: this.id}});
  }

  newCatch() {
    router.push({name:'catch', params: {tripId: this.id, catchId:Constants.NEW_CATCH_ID}});
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

      .no-catch {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;

        .new-catch-square-button {
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: center;

          background-image: url("/img/illustration_fish_wire.svg");
          background-repeat: no-repeat;
          background-size: 67%;
          background-position: center;

          border: 1px dashed @pale-sky;
          border-radius: 8px;
          height: 295px;
          width: 295px;

          cursor: pointer;

          .pastille {
            width: 70px;
            height: 70px;
            line-height: 30px;
            font-size: 30px;
            color: @white;
            background: @pale-sky;
          }
        }

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
