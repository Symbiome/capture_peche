<template>
  <div class="some-trip-header">
    <div>
      <span v-if="trip">{{trip.name}}</span>
      <span v-if="!trip">Nouvelle sortie</span>
    </div>
    <div class="header-icons">
      <div class="header-icons-group">
        <span v-if="trip">{{catchs}}</span>
        <span v-if="!trip">0</span>
        <i class="icon-fish"></i>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import TripMeta from '@/pojos/TripMeta';
import {TripBean} from '@/pojos/BackendPojos';
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class SomeTripHeader extends Vue {
  @Prop() trip?:TripMeta;

  catchs:number = 0;

  created() {
    this.catchs = this.getCatchs(this.trip);
  }

  getCatchs(object:any) {
    if (object && object.catchs) {
      return object.catchs.length;
    }
    return 0;
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.some-trip-header {

  display: flex;
  justify-content: space-between;

  height: 49px;
  padding-left: 20px;
  padding-right: 20px;
  padding-top: 10px;
  padding-bottom: 10px;
  margin-bottom: 10px;

  font-weight: bold;
  font-size: 14px;
  line-height: 19px;

  .header-icons {
    display: flex;
    flex-direction: row;
    align-items: center;

    .header-icons-group {
      display: flex;
      margin-left: 20px;
      margin-right: 0px;
      align-items: center;

      * {
        margin-left: 5px;
      }

      .icon-chevron {
        margin-top: 6px;
        font-size: 7px;
      }
    }
  }
}
</style>
