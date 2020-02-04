<template>
  <div class="catch-preview">
    <div class="preview-top no-picture">
      <div class="meta">
        <div class="meta-row">
          <i class="icon-size"/> {{aCatch.size}} cm<br/>
        </div>
        <div class="meta-row" v-if="caughtAtLabel">
          <i class="icon-clock"/> {{caughtAtLabel}}<br/>
        </div>
        <div class="meta-row">
          {{techniqueLabel}}
          <span v-if="!aCatch.keep"> - relâché</span>
        </div>
      </div>
    </div>
    <div class="preview-bottom">
      <div class="bottom-left">
        <i class="icon-fish"/>
        {{speciesLabel}}
      </div>
      <div class="bottom-right">
        Voir
        <button v-on:click="$emit('openCatch');"><i class="icon-arrow"/></button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import CatchSummary from '@/pojos/CatchSummary';
import {SpeciesWithAlias, TripBean} from '@/pojos/BackendPojos';

import TripsService from '@/services/TripsService';
import ReferentialService from '@/services/ReferentialService';
import Helpers from '@/pojos/Helpers';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class CatchPreview extends Vue {

  @Prop() lakeId: string;
  @Prop() aCatch: CatchSummary;

  caughtAtLabel:string = '';
  techniqueLabel:string = '';
  speciesLabel:string = '';

  created() {
    if (this.aCatch.caughtAt) {
      this.caughtAtLabel = Helpers.formatToTime(this.aCatch.caughtAt);
    }

    ReferentialService.getSpeciesAndTechniques(this.lakeId, this.speciesLoaded);

  }

  speciesLoaded(species:SpeciesWithAlias[],techniques:Technique[]) {
    species.forEach(s => {
      if (this.aCatch.speciesId == s.id) {
        this.speciesLabel = s.name;
      }
    });
    techniques.forEach(t => {
      if (this.aCatch.techniqueId == t.id) {
        this.techniqueLabel = t.name;
      }
    });
  }


}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.catch-preview {
  width: 295px;
  height: 100%;
  // border-radius: 8px;
  padding: 5px;
  // border: 1px solid @gunmetal;

  display: flex;
  flex-direction: column;
  justify-content: center;

  .preview-top {
    flex: 1;
    background-color: @gainsboro;

    border-top-left-radius: 8px;
    border-top-right-radius: 8px;
    padding: 20px;

    &.no-picture {

      background-image: url("/img/camera.svg");
      background-repeat: no-repeat;
      background-position: center;
    }

    .meta {
      width: fit-content;
      height: 108px;
      background: @cyprus;
      opacity: 0.8;
      border-radius: 8px;

      padding: 20px;
      font-size: 12px;
      line-height: 16px;
      color: @white;
      text-align: left;

      .meta-row {
        padding-bottom: 10px;

        i {
          margin-right: 10px;
        }
      }
    }
  }

  .preview-bottom {
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;

    height: 50px;
    background-color: @white;
    border-bottom-left-radius: 8px;
    border-bottom-right-radius: 8px;

    .bottom-left {
      font-size: 18px;
      margin-left: 20px;
      i {
        color: @pelorous;
        margin-right: 10px;
      }
    }

    .bottom-right {
      font-size: 12px;
      margin-right: 20px;

      button {
        width: 32px;
        height: 20px;
        background-color: @summer-sky;
        color: @white;
        border: 0px;
        border-radius: 50px;
        margin-left: 10px;
      }
    }
  }
}
</style>
