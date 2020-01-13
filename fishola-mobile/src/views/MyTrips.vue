<template>
  <div class="my-trips page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="page my-trips-page">
      <MyTripsHeader v-bind:count="count"
                     v-bind:sortDown="sortDown"
                     v-on:reverseSortOrder="reverseSortOrder"/>
      <MyTripsSearch/>
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

import FisholaHeader from '@/layout/FisholaHeader.vue'
import MyTripsHeader from '@/components/my-trips/MyTripsHeader.vue'
import MyTripsSearch from '@/components/my-trips/MyTripsSearch.vue'
import MyTripsList from '@/components/my-trips/MyTripsList.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';

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
  count:number = 0;

  constructor() {
    super();
    this.trips = [];
  }

  created() {
    TripsService.listTrips(this.sortDown, this.tripsLoaded);
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
