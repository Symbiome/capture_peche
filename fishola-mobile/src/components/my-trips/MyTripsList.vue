<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #L%
  -->
<template>
  <div class="pane my-trips-list">
    <div v-if="!loading && trips.length > 0"
         class="pane-content"
         v-on:scroll="scrolled"
         id="scroll-container">
      <div v-for="t in trips" v-bind:key="t.id">
        <MyTripsItem v-bind:trip="t"
                     v-on:selected="tripSelected(t.id)"
                     v-on:unselected="tripUnselected(t.id)"/>
      </div>
      <div v-if="offline" class="offline-with-trips">
        <div class="top">
          <img src="/img/illustration_fish_wire.svg"/>
          <span>Pas de connexion internet</span>
        </div>
        <div class="bottom">
          <span>Vous pouvez créer<br/>une sortie !</span>
          <i class="icon-triangle"></i>
        </div>
      </div>
      <div class="bottom-page-spacer"></div>
    </div>
    <div v-if="!loading && !offline && trips.length == 0" class="pane-content no-trips">
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
    <div v-if="!loading && offline && trips.length == 0" class="pane-content no-trips offline">
      <div class="top">
        <img src="/img/illustration_fish_wire.svg"/>
        <span>Pas de connexion internet</span>
      </div>
      <div class="bottom">
        <span>Vous pouvez créer<br/>une sortie !</span>
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
  @Prop() offline!:boolean;
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

  tripSelected(tripId:string) {
    this.$emit('trip-selected', tripId);
  }

  tripUnselected(tripId:string) {
    this.$emit('trip-unselected', tripId);
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
        font-size: @fontsize-span-big;
        line-height: calc(@fontsize-span-big + 7px);
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
        font-size: @fontsize-span-big;
        line-height: calc(@fontsize-span-big + 7px);
      }
      i {
        color: @terra-cotta;
        font-size: @fontsize-small-paragraph;
        margin-top: 10px;
      }
    }

    &.offline {
      .top {
        span {
         color: @carrot-orange;
        }
      }
    }
  }

  .offline-with-trips {

    margin-top: 30px;

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
        font-size: @fontsize-span-big;
        line-height: calc(@fontsize-span-big + 7px);
        color: @carrot-orange; 
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
         font-size: @fontsize-span-big;
        line-height: calc(@fontsize-span-big + 7px);
      }
      i {
        color: @terra-cotta;
        font-size: @fontsize-small-paragraph;
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
