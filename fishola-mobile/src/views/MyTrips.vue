<template>
  <div class="my-trips">
    <FisholaHeader />
    <MyTripsHeader v-bind:count="trips.length" />
    <MyTripsSearch/>
    <MyTripsList v-bind:trips="trips"/>
    <FisholaFooter/>
    <NewTripButton/>
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
    this.trips.push(new Trip('0', 'Sortie du mardi 3 décembre', 'Lac du Bourget', 'Mardi 3 décembre'));
    this.trips.push(new Trip('1', 'Pêche d\'anniversaire', 'Lac Léman', 'Jeudi 21 novembre'));
    this.trips.push(new Trip('2', 'Avec Miguel', 'Lac d\'Annecy', 'Samedi 15 octobre'));
    this.trips.push(new Trip('4', 'Sortie du lundi 2 septembre', 'Lac d\'Aiguebelette', 'Lundi 2 septembre'));
    this.trips.push(new Trip('3', 'Entre filles', 'Lac du Bourget', 'Lundi 28 août'));
  }
}

</script>
