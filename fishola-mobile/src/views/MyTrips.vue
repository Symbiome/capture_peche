<template>
  <div class="my-trips">
    <FisholaHeader />
    <MyTripsHeader v-bind:count="trips.length" />
    <MyTripsSearch/>
    <MyTripsList v-bind:trips="trips"/>
    <FisholaFooter/>
    <NewTripButton v-bind:count="trips.length" v-bind:trips="trips"/>
  </div>
</template>

<script>
import Trip from '@/pojos/Trip';

import FisholaHeader from '@/layout/FisholaHeader.vue'
import MyTripsHeader from '@/components/my-trips/MyTripsHeader.vue'
import MyTripsSearch from '@/components/my-trips/MyTripsSearch.vue'
import MyTripsList from '@/components/my-trips/MyTripsList.vue'
import NewTripButton from '@/components/my-trips/NewTripButton.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    MyTripsHeader,
    MyTripsSearch,
    MyTripsList,
    NewTripButton,
    FisholaFooter
  }
})
export default class MyTrips extends Vue {
  
  trips = [];

  constructor() {
    super();
    this.trips = [];
  }

  mounted() {
  }
}

</script>
