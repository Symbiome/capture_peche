<template>
  <div class="my-trips">
    <Header />
    <MyTripsHeader v-bind:count="trips.length" />
    <MyTripsSearch/>
    <MyTripsList v-bind:trips="trips"/>
    <Footer/>
    <NewTripButton/>
  </div>
</template>

<script>
import Trip from '@/pojos/Trip';

import Header from '@/components/Header.vue'
import MyTripsHeader from '@/components/MyTripsHeader.vue'
import MyTripsSearch from '@/components/MyTripsSearch.vue'
import MyTripsList from '@/components/MyTripsList.vue'
import NewTripButton from '@/components/NewTripButton.vue'
import Footer from '@/components/Footer.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    Header,
    MyTripsHeader,
    MyTripsSearch,
    MyTripsList,
    NewTripButton,
    Footer
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
