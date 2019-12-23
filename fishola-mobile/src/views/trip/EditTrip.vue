<template>
  <div class="edit-trip page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="edit-trip-content">
        <h1 v-if="duration">{{duration}}</h1>
        <div v-if="trip.catchs.length == 0" class="no-catch">
          <img src="/img/illustration_fish_wire.svg"/>
          <span>Aucune capture</span>
        </div>
        <div class="edit-trip-end">
          <button v-if="liveRunning"
                  v-on:click="finish">
            <i class="icon-stop"/>
            Fin de pêche
          </button>
        </div>
      </div>
    </div>
    <FisholaFooter button-icon="icon-fish"
                    button-text="Capture"
                    v-on:buttonClicked="newCatch"
                    shortcuts="back,step-3-4,giveup"
                    back-event="onBackButton"
                    v-on:onBackButton="editSpecies"/>
  </div>
</template>

<script lang="ts">
import Trip from '@/pojos/Trip';
import Species from '@/pojos/Species';
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
export default class EditTrip extends Vue {

  @Prop() id!:string;

  trip?:Trip = new Trip();

  duration?:string = '';
  liveRunning:boolean = false;

  created() {
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  mounted() {
  }

  tripLoaded(someTrip:any) {
    console.log("Trip chargé", someTrip);
    this.trip = someTrip;

    if (this.trip!.mode == 'live') {
      this.computeDuration();
      if (!this.trip!.finishedAt) {
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
    this.trip!.finishedAt = new Date();
    this.computeDuration();
    this.liveRunning = false;
    TripsService.saveTrip(this.trip!, this.tripSaved);
  }

  tripSaved() {
    router.push({name:'trip-summary', params: {id: this.id}});
  }

  newCatch() {
    this.$root.$emit('toaster-warning', 'Work in progress');
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
    justify-content: space-between;

    text-align:center;

    background-color: @white-smoke;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
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


    .no-catch {
      flex: auto;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;

      img {
        width: 200px;
      }

      span {
        font-weight: 300;
        font-size: 20px;
        line-height: 27px;
        color: @pale-sky;
        text-align: center;
        margin-top: 30px;
      }
    }

    .edit-trip-end {
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

        color: @white;
        background-color: @summer-sky;

        border: 0px;
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
