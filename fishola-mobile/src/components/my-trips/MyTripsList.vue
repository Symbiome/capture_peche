<template>
  <div class="my-trips-list">
    <div v-for="t in trips" v-bind:key="t.id">
      <MyTripsItem v-bind:trip="t"/>
    </div>
  </div>
</template>

<script lang="ts">

import MyTripsItem from '@/components/my-trips/MyTripsItem.vue';
import Trip from '@/pojos/Trip';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    MyTripsItem
  }
})
export default class MyTripsList extends Vue {
  @Prop() trips!:Trip[];
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

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
