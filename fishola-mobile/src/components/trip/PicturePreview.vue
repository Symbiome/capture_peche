<template>
  <div class="picture-preview"
       v-on:click="$emit('take-picture')">
    <div v-if="!pictureId" class="no-picture">
      <img src="/img/camera.svg"/>
      <span>Appuyer pour ajouter une photo</span>
    </div>
    <img v-if="pictureId" 
         class="picture" 
         src="/img/omble.png">
  </div>
</template>

<script lang="ts">
import CatchSummary from '@/pojos/CatchSummary';
import {SpeciesWithAlias, Technique, TripBean} from '@/pojos/BackendPojos';

import TripsService from '@/services/TripsService';
import ReferentialService from '@/services/ReferentialService';
import Helpers from '@/pojos/Helpers';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class PicturePreview extends Vue {

  @Prop() pictureId?:string;

  created() {
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.picture-preview {

  height: 165px;
  width: 100%;
  position: absolute;
  top: 0;
  background-color: @gainsboro;

  img.picture {
    height: 100%;
    width: 100%;
    object-fit: cover;
    object-position: 50% 50%;
  }

  .no-picture {

    height: 100%;

    display:flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    span {
      margin-top: 10px;
      color: @pale-sky;
      font-weight: 300;
      font-size: 12px;
      line-height: 16px;
    }
  }
}
</style>
