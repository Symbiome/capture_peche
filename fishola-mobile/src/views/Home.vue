<template>
  <div class="home">
    <Header />
  </div>
</template>

<script>
// @ is an alias to /src
import Header from '@/components/Header.vue'
// import HelloWorld from '@/components/HelloWorld.vue'

export default {
  name: 'home',
  components: {
    Header,
    // HelloWorld
  }
}
</script>
