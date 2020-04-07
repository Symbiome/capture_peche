<template>
  <div class="my-trips page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="page my-trips-page">
      <MyTripsHeader v-bind:count="count"
                     v-bind:sortDown="sortDown"
                     v-on:reverseSortOrder="reverseSortOrder"/>
      <MyTripsSearch v-model="term"/>
      <MyTripsList v-bind:trips="trips"
                   v-bind:hasSearchTerm="!!currentSearchTerm"
                   v-bind:noTripYet="totalCount == 0"
                   v-bind:loading="loading"
                   v-on:more-trips="loadNextPage"
                   v-on:trip-selected="tripSelected"
                   v-on:trip-unselected="tripUnselected"/>
      <RunningOverlay v-if="hasRunningTrip"/>
    </div>
    <FisholaFooter shortcuts="logout,dashboard,home"
                   v-bind:hideButton="hasRunningTrip"
                   v-bind:button-icon="selectedTripIds.length == 0 ? (totalCount == 0 ? 'icon-fishing':'icon-plus'): 'icon-delete'"
                   v-bind:button-text="selectedTripIds.length == 0 ? (totalCount == 0 ? 'Commencer':'Nouveau') : 'Supprimer'"
                   v-on:buttonClicked="footerButtonClicked"
                   selected="home"/>
  </div>
</template>

<script lang="ts">
import router from '@/router'

import {TripLight} from '@/pojos/BackendPojos';

import TripsService from '@/services/TripsService';
import {TripsAndCount} from '@/services/TripsService';

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import MyTripsHeader from '@/components/my-trips/MyTripsHeader.vue'
import MyTripsSearch from '@/components/my-trips/MyTripsSearch.vue'
import MyTripsList from '@/components/my-trips/MyTripsList.vue'
import RunningOverlay from '@/components/layout/RunningOverlay.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Watch, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    MyTripsHeader,
    MyTripsSearch,
    MyTripsList,
    RunningOverlay,
    FisholaFooter
  }
})
export default class MyTripsView extends Vue {
  
  trips:TripLight[] = [];
  loading:boolean = true;
  sortDown:boolean = true;
  term:string = '';
  currentSearchTerm:string = '';
  currentPage:number = 0;
  count:number = 0;
  totalCount:number = -1;

  refreshTimer:any = undefined;

  hasRunningTrip:boolean = false;

  selectedTripIds:string[] = [];

  @Watch('term')
  onTermChanged(value: string, oldValue: string) {
    if (this.refreshTimer) {
      this.refreshTimer.cancel();
    }
    this.refreshTimer = Vue.lodash.debounce(this.loadTrips, 1000);
    this.refreshTimer();
  }

  constructor() {
    super();
    this.trips = [];
  }

  loadTrips() {
    this.currentPage = 0;
    this.currentSearchTerm = this.term;
    this.selectedTripIds = [];
    TripsService.listTrips(this.sortDown, this.term)
      .then(this.tripsLoaded, this.loadError);
    this.loading = true;
  }

  loadNextPage() {
    if (this.trips.length < this.count) {
      TripsService.listTrips(this.sortDown, this.currentSearchTerm, this.currentPage + 1)
        .then(this.moreTripsLoaded, this.loadError);
    }
  }

  created() {
    this.loadTrips();
    TripsService.hasRunningTrip()
      .then((result:boolean) => this.hasRunningTrip = result);
  }

  mounted() {
    this.$root.$on('trips-saved', () => {
      console.log("La liste des sorties a été mise à jour, on rafraichit ...");
      this.loadTrips();
    });
  }

  beforeDestroy() {
    // On fait en sortie de ne plus écouter les évènements si le composant n'est plus actif
    this.$root.$off('trips-saved');
  }

  tripsLoaded(data:TripsAndCount) {
    this.trips.slice();
    this.trips = data.trips;
    this.loading = false;
    this.count = data.count;

    // On considère que le premier appel renvoie toujours le total
    if (this.totalCount == -1) {
      this.totalCount = data.count;
    }
  }

  moreTripsLoaded(data:TripsAndCount) {
    this.currentPage++;
    data.trips.forEach((t) => this.trips.push(t));
  }

  loadError(data:any) {
    console.log("Erreur au chargmeent des sorties", data);
    if (data.status == 401) {
      router.push('/login');
    }
  }

  reverseSortOrder() {
    this.sortDown = !this.sortDown;
    this.loadTrips();
  }

  footerButtonClicked() {
    if (this.selectedTripIds.length == 0) {
      this.newTrip();
    } else {
      let message = "Voulez-vous supprimer cette sortie ?";
      if (this.selectedTripIds.length > 1) {
        message = "Voulez-vous supprimer ces sorties ?";
      }
      if (confirm(message)) {
        this.deleteSelectedTrips();
      }
    }
  }

  newTrip() {
    router.push('/trips/new');
  }

  tripSelected(tripId:string) {
    this.selectedTripIds.push(tripId);
  }

  tripUnselected(tripId:string) {
    let index = this.selectedTripIds.indexOf(tripId);
    if (index != -1) {
      this.selectedTripIds.splice(index, 1);
    }
  }

  deleteSelectedTrips() {
    TripsService.deleteTrips(this.selectedTripIds)
      .then(this.tripsDeleted);
  }

  tripsDeleted() {
    let plural = this.selectedTripIds.length > 1 ? 's' : '';
    let message = `${this.selectedTripIds.length} sortie${plural} supprimée${plural}`;
    this.$root.$emit('toaster-success', message);
    this.loadTrips();
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../less/main";

.my-trips-page {

  display: flex;
  flex-direction: column;

  .my-trips-list {
    flex-grow: 1;
    overflow: auto;
  }

}

</style>
