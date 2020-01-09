<template>
  <div class="my-trips-list">
    <div v-for="t in trips" v-bind:key="t.id">
      <MyTripsItem v-bind:trip="t"/>
    </div>
    <div v-if="trips.length > 0" class="bottom-spacer"></div>
    <div v-if="trips.length == 0 && !loading" class="no-trips">
      <div class="top">
        <img src="/img/illustration_fish.svg"/>
        <span>Aucune sortie de pêche</span>
      </div>
      <div class="bottom">
        <span>Commencez votre <br/>première sortie !</span>
        <i class="icon-triangle"></i>
      </div>
    </div>
    <div v-if="loading" class="loading">
      <div class="spinner">&nbsp;</div>
    </div>
  </div>
</template>

<script lang="ts">

import MyTripsItem from '@/components/my-trips/MyTripsItem.vue';

import {TripLight} from '@/pojos/BackendPojos';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    MyTripsItem
  }
})
export default class MyTripsList extends Vue {
  @Prop() trips!:TripLight[];
  @Prop() loading!:boolean;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.my-trips-list {
  background-color: @white-smoke;
  color: @gunmetal;
  max-height: calc(100vh - 144px - @footer-height);
  overflow: auto;
  padding-top: 30px;
  margin-top: 10px;
  border-top-left-radius: 30px;
  border-top-right-radius: 30px;

  .bottom-spacer {
    height: 19px;
  }

  .no-trips {
    height: 100%;
    background-color: @white-smoke;

    display: flex;
    flex-direction: column;
    justify-content: space-between;

    .top {
      flex: auto;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;

      img {
        width: 40%;
      }

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
      min-height: 100px;
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


  .loading {

    height: 100%;

    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;

    @keyframes spin { 100% { -webkit-transform: rotate(360deg); transform:rotate(360deg); } }

    .spinner {
      height: 60px;
      width: 60px;
      border-radius: 50%;
      border-top: 3px solid @pelorous;
      border-left: 3px solid @pelorous;
      animation:spin 2s linear infinite;
    }

  }

}

</style>
