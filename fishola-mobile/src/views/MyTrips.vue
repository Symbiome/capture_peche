<template>
  <div class="my-trips page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="page my-trips-page">
      <MyTripsHeader v-bind:count="count"
                     v-bind:sortDown="sortDown"
                     v-on:reverseSortOrder="reverseSortOrder"/>
      <MyTripsSearch v-model="term"/>
      <img v-bind:src="imageContent"/>
      <MyTripsList v-bind:trips="trips" v-bind:loading="loading"/>
    </div>
    <FisholaFooter shortcuts="logout,dashboard,home"
                    v-bind:button-icon="trips.length == 0 ? 'icon-fishing':'icon-plus'"
                    v-bind:button-text="trips.length == 0 ? 'Commencer':'Nouveau'"
                    v-on:buttonClicked="newTrip"
                    selected="home"/>
  </div>
</template>

<script lang="ts">
import router from '@/router'

import {TripLight} from '@/pojos/BackendPojos';

import TripsService from '@/services/TripsService';

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import MyTripsHeader from '@/components/my-trips/MyTripsHeader.vue'
import MyTripsSearch from '@/components/my-trips/MyTripsSearch.vue'
import MyTripsList from '@/components/my-trips/MyTripsList.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Watch, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    MyTripsHeader,
    MyTripsSearch,
    MyTripsList,
    FisholaFooter
  }
})
export default class MyTrips extends Vue {
  
  trips:TripLight[] = [];
  loading:boolean = true;
  sortDown:boolean = true;
  term:string = '';
  count:number = 0;

  @Watch('term')
  onTermChanged(value: string, oldValue: string) {
    console.log("New value", value);
  }

  imageContent:string = '';

  constructor() {
    super();
    this.trips = [];
  }

  created() {
    TripsService.listTrips(this.sortDown, this.tripsLoaded);
  }

  mounted() {
    this.$root.$on('trips-saved', () => {
      console.log("La liste des sorties a été mise à jour, on rafraichit ...");
      TripsService.listTrips(this.sortDown, this.tripsLoaded);
    });
  }

  beforeDestroy() {
    // On fait en sortie de ne plus écouter les évènements si le composant n'est plus actif
    this.$root.$off('trips-saved');
  }

  tripsLoaded(ts:TripLight[], count:number) {
    this.trips.slice();
    this.trips = ts;
    this.loading = false;
    this.count = count;
  }

  reverseSortOrder() {
    this.sortDown = !this.sortDown;
    TripsService.listTrips(this.sortDown, this.tripsLoaded);
  }

  newTrip() {
    router.push('/trips/new');
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
    flex: auto;
  }

}

</style>
