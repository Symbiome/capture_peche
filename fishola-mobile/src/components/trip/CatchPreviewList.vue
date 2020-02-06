<template>
  <div class="catch-preview-list">
    <div class="new-catch" 
         v-if="modifiable">
      <div class="new-catch-square-button"
           v-on:click="newCatch">
        <i class="pastille icon-plus"/>
      </div>
    </div>
    <div v-for="c in reversedCatchs()"
          v-bind:key="c.id"
          class="preview-wrapper"
          v-on:click="openCatch(c)">
      <CatchPreview v-bind:lakeId="lakeId"
                    v-bind:aCatch="c"
                    v-on:openCatch="openCatch(c)"/>
    </div>
  </div>
</template>

<script lang="ts">
import CatchSummary from '@/pojos/CatchSummary';
import {SpeciesWithAlias, TripBean} from '@/pojos/BackendPojos';

import CatchPreview from '@/components/trip/CatchPreview.vue'

import TripsService from '@/services/TripsService';
import ReferentialService from '@/services/ReferentialService';
import Helpers from '@/pojos/Helpers';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    CatchPreview
  }
})
export default class CatchPreviewList extends Vue {

  @Prop() lakeId:string;
  @Prop() catchs:CatchSummary[];
  @Prop() modifiable:boolean;

  created() {
  }

  reversedCatchs() {
    if (this.catchs) {
      return this.catchs.slice().reverse();
    } else {
      return this.catchs;
    }
  }

  newCatch() {
    this.$emit('newCatch');
  }

  openCatch(aCatch:CatchSummary) {
    this.$emit('openCatchFromId', aCatch.id);
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.catch-preview-list {

  height: 100%;
  display: flex;

  padding-left: 35px;
  padding-right: 35px;

  .new-catch {

    height: 100%;

    padding-top: 5px;
    padding-bottom: 5px;

    .new-catch-square-button {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;

      background-image: url("/img/illustration_fish_wire.svg");
      background-repeat: no-repeat;
      background-size: auto 75%;
      background-position: center;

      border: 1px dashed @pale-sky;
      border-radius: 8px;
      height: 100%;
      width: calc(100vw - 80px);

      margin-left: 5px;
      margin-right: 5px;

      cursor: pointer;

      .pastille {
        width: 70px;
        height: 70px;
        line-height: 30px;
        font-size: 30px;
        color: @white;
        background: @pale-sky;
      }

    }
  }

  .preview-wrapper {
  }

}
</style>
