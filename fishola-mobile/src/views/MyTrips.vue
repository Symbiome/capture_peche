<template>
  <div class="my-trips page-with-header">
    <FisholaHeader />
    <div class="page my-trips-page">
      <MyTripsHeader v-bind:count="trips.length" />
      <MyTripsSearch/>
      <MyTripsList v-bind:trips="trips"/>
      <NewTripButton v-bind:count="trips.length" v-bind:trips="trips"/>
      <FisholaFooter selected="home"/>
    </div>
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
