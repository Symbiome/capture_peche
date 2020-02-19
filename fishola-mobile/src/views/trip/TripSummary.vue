<template>
  <div class="edit-trip-summary page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-summary-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="pane">
        <h1>Fin de pêche</h1>
        <div class="pane-content">
          <SomeTripSummary ref="summary"
                          v-if="trip"
                          v-bind:trip="trip"
                          v-on:trip-modified="onUpdatedTrip"/>
          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter button-text="Envoyer"
                   button-icon="icon-send"
                   v-on:buttonClicked="sendClicked"
                   shortcuts="back,step-4-4,giveup"/>
  </div>
</template>

<script lang="ts">
import TripSummary from '@/pojos/TripSummary';

import TripsService from '@/services/TripsService';

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import SomeTripSummary from '@/components/trip/SomeTripSummary.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

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
export default class TripSummaryVue extends Vue {

  @Prop() id!:string;

  trip?:TripSummary = { id:'', name:'',  mode:'Live', startedAt: new Date(), lakeId:'', date: new Date(), type:'Craft', speciesIds:[] };

  created() {
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  mounted() {
  }

  tripLoaded(someTrip:TripSummary) {
    this.trip = someTrip;
  }

  sendClicked() {
      // On demande au composant enfant de fournir le modèle mis à jour
      let summaryComponent:any = this.$refs.summary;
      summaryComponent.emitUpdatedTrip();
  }

  onUpdatedTrip(trip:any) {
      // On reçoit le modèle mis à jour, on le sauvegarde
      TripsService.sendTrip(trip, this.tripSaved);
  }

  tripSaved() {
    router.push('/trips');
    this.$root.$emit('ask-for-sync-check');
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

.edit-trip-summary-page {

}

</style>
