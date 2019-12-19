<template>
  <div class="my-trips page-with-header shifted-background">
    <FisholaHeader />
    <div class="page my-trips-page">
      <MyTripsHeader v-bind:count="trips.length"
                     v-on:newMockTrip="newMockTrip"/>
      <MyTripsSearch/>
      <MyTripsList v-bind:trips="trips"/>
      <FisholaFooter shortcuts="logout,dashboard,home"
                     v-bind:button-icon="trips.length == 0 ? 'icon-fishing':'icon-plus'"
                     v-bind:button-text="trips.length == 0 ? 'Commencer':'Nouveau'"
                     v-on:buttonClicked="newTrip"
                     selected="home"/>
    </div>
  </div>
</template>

<script>
import router from '@/router'
import TripLight from '@/pojos/TripLight';

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
  
  trips = [];

  constructor() {
    super();
    this.trips = [];
  }

  mounted() {
  }

  newMockTrip() {

    let tripsBacklog = [];
    tripsBacklog.push(new TripLight('0', 'Sortie du mardi 3 décembre', 'Lac du Bourget', 'Mardi 3 décembre'));
    tripsBacklog.push(new TripLight('1', 'Pêche d\'anniversaire', 'Lac Léman', 'Jeudi 21 novembre'));
    tripsBacklog.push(new TripLight('2', 'Avec Miguel', 'Lac d\'Annecy', 'Samedi 15 octobre'));
    tripsBacklog.push(new TripLight('3', 'Sortie du lundi 2 septembre', 'Lac d\'Aiguebelette', 'Lundi 2 septembre'));
    tripsBacklog.push(new TripLight('4', 'Entre filles', 'Lac du Bourget', 'Lundi 28 août'));
    tripsBacklog.push(new TripLight('5', 'Sortie du mardi 3 décembre', 'Lac du Bourget', 'Mardi 3 décembre'));
    tripsBacklog.push(new TripLight('6', 'Pêche d\'anniversaire', 'Lac Léman', 'Jeudi 21 novembre'));
    tripsBacklog.push(new TripLight('7', 'Avec Miguel', 'Lac d\'Annecy', 'Samedi 15 octobre'));
    tripsBacklog.push(new TripLight('8', 'Sortie du lundi 2 septembre', 'Lac d\'Aiguebelette', 'Lundi 2 septembre'));
    tripsBacklog.push(new TripLight('9', 'Entre filles', 'Lac du Bourget', 'Lundi 28 août'));

    tripsBacklog[0].canBeModified = true;
    tripsBacklog[0].duration = '2h02min';
    tripsBacklog[1].duration = '1h22min';
    tripsBacklog[2].duration = '3h17min';
    tripsBacklog[3].duration = '2h58min';
    tripsBacklog[4].duration = '2h49min';
    tripsBacklog[5].duration = '2h02min';
    tripsBacklog[6].duration = '1h22min';
    tripsBacklog[7].duration = '3h17min';
    tripsBacklog[8].duration = '2h58min';
    tripsBacklog[9].duration = '2h49min';

    this.trips.push(tripsBacklog[this.trips.length % tripsBacklog.length]);

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
