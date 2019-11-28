<template>
  <div class="my-trips">
    <Header />
    <MyTripsHeader count="5" />
    <MyTripsSearch/>
    <MyTripsList/>
    <Footer/>
    <NewTripButton/>
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import MyTripsHeader from '@/components/MyTripsHeader.vue'
import MyTripsSearch from '@/components/MyTripsSearch.vue'
import MyTripsList from '@/components/MyTripsList.vue'
import NewTripButton from '@/components/NewTripButton.vue'
import Footer from '@/components/Footer.vue'

export default {
  name: 'my-trips',
  components: {
    Header,
    MyTripsHeader,
    MyTripsSearch,
    MyTripsList,
    NewTripButton,
    Footer
  }
}
</script>
