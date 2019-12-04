<template>
  <div class="my-trips-list">
    <div v-for="t in trips" v-bind:key="t.id">
      <MyTripsItem v-bind:trip="t"/>
    </div>
    <div v-if="trips.length == 0" class="no-trips">
      <div class="top">
        <img src="/img/illustration_fish.svg"/>
        <span>Aucune sortie de pêche</span>
      </div>
      <div class="bottom">
        <span>Commencez votre <br/>première sortie !</span>
        <i class="icon-triangle"></i>
      </div>
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

  .no-trips {
    height: calc(100vh - 274px);
    background-color: @white-smoke;
    // border: 1px solid red;

    display: flex;
    flex-direction: column;

        justify-content: space-between;


    .top {
      height: calc(100% - 176px);
      display: flex;
      flex-direction: column;
      justify-content: center;

      span {
        font-size: 18px;
        line-height: 25px;
        color: @pale-sky;
        text-align: center;
        margin-top: 30px;
      }
    }

    .bottom {
      height: 176px;
      display: flex;
      flex-direction: column;
      justify-content: center;

      text-align: center;

      span {
        color: @pelorous;
        font-size: 18px;
        line-height: 25px;
      }
      i {
        color: @terra-cotta;
        font-size: 12px;
        margin-top: 10px;
      }
    }
  }
}

</style>
