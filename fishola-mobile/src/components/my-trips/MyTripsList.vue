<template>
  <div class="pane my-trips-list">
    <div v-if="!loading && trips.length > 0"
         class="pane-content"
         v-on:scroll="scrolled"
         id="scroll-container">
      <div v-for="t in trips" v-bind:key="t.id">
        <MyTripsItem v-bind:trip="t"/>
      </div>
      <div class="bottom-page-spacer"></div>
    </div>
    <div v-if="!loading && trips.length == 0" class="pane-content no-trips">
      <div class="top">
        <img src="/img/illustration_fish.svg"/>
        <span v-if="hasSearchTerm">Aucune sortie de pêche trouvée</span>
        <span v-if="!hasSearchTerm">Aucune sortie de pêche</span>
      </div>
      <div class="bottom" v-if="noTripYet">
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
  @Prop() hasSearchTerm!:boolean;
  @Prop() noTripYet!:boolean;

  moreTripsTimer:any = undefined;

  mounted() {
    // On fait en sorte qu'il n'y ait pas plus d'un appel par seconde
    this.moreTripsTimer = Vue.lodash.throttle(this.askForMoreTrips, 1000);
  }

  scrolled() {
    let elem = document.getElementById('scroll-container');
    if (elem) {
      let delta = elem.scrollHeight - elem.scrollTop - elem.offsetHeight;
      // Quand on arrive à moins de 300px du bas on demande le chargement de la suite
      if (delta < 300) {
        this.moreTripsTimer();
      }
    }
  }

  askForMoreTrips() {
    this.$emit('more-trips');
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.my-trips-list {

  .pane-content {
    padding-left: 0px;
    padding-right: 0px;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
  }

  .no-trips {
    height: 100%;

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
