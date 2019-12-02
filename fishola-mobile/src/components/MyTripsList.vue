<template>
  <div class="my-trips-list">
    <MyTripsItem/>
    <MyTripsItem/>
    <MyTripsItem/>
    <MyTripsItem/>
    <MyTripsItem/>
  </div>
</template>

<script lang="ts">

import MyTripsItem from '@/components/MyTripsItem.vue';

import { Component, Prop, Vue } from 'vue-property-decorator';

export default {
  name: 'my-trips-list',
  components: {
    MyTripsItem
  }
}

// @Component
// export default class Header extends Vue {
// }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.my-trips-list {
  background-color: @white-smoke;
  color: @gunmetal;
  height: 100%;
  padding-top: 30px;
  margin-top: 10px;
  border-top-left-radius: 30px;
  border-top-right-radius: 30px;

  p {
    margin: 0px;
    padding-left: 30px;
    border-bottom: 1px solid @gainsboro;
  }

  hr { 
    color: @gainsboro;
  }
}

</style>
