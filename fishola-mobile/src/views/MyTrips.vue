<template>
  <div class="my-trips">
    <Header />
    <MyTripsHeader count="5" />
  </div>
</template>

<script>
import Header from '@/components/Header.vue'
import MyTripsHeader from '@/components/MyTripsHeader.vue'

export default {
  name: 'my-trips',
  components: {
    Header,
    MyTripsHeader
  }
}
</script>
