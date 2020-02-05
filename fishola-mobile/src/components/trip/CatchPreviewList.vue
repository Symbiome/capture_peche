<template>
  <div class="catch-preview-list">
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

  created() {
  }

  reversedCatchs() {
    if (this.catchs) {
      return this.catchs.slice().reverse();
    } else {
      return this.catchs;
    }
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

  .preview-wrapper {
  }

}
</style>
