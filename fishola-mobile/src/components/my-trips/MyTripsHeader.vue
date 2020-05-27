<template>
  <div class="my-trips-header secondary-header">
    <div>
      <span>Mes sorties</span>
    </div>
    <div class="header-icons">
      <div class="header-icons-group" v-on:click="$emit('reverseSortOrder')">
        <i class="icon-calendar"></i>
        <i class="icon-chevron" v-if="sortDown"></i>
        <i class="icon-chevron icon-chevron-up" v-if="!sortDown"></i>
      </div>
      <div class="header-icons-group">
        <span v-if="offline">?</span>
        <span v-if="!offline">{{count}}</span>
        <i class="icon-fishing"></i>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class MyTripsHeader extends Vue {
  @Prop() offline!:boolean;
  @Prop() count!:number;
  @Prop() sortDown!:boolean;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.my-trips-header {

  display: flex;
  justify-content: space-between;

  font-weight: bold;
  font-size: 14px;

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
