<template>
  <div class="top" v-bind:class="'top-' + n">
    <span>{{n}}</span>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Top extends Vue {
  @Prop() n:number;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";

  .top {
    font-weight: bold;
    font-size: 12px;
    line-height: 20px;
  }

  .top-2, .top-3, .top-4, .top-5 {
    height:20px;
    width:20px;
    border-radius: 50%;
  }

  .top-1 {
    color: @white;
    position: relative;
    z-index: 1;
    line-height: 18px;
  }

  .top-1::after {
    content: '\f11b'; // Correspond à .icon-star:before dans _icons.less
    font-family: 'Fishola-Icons';
    color: @terra-cotta;
    position: absolute;
    left: -6px;
    top: -1px;
    font-size: 20px;
    line-height: 20px;
    z-index: -1;
  }

  .top-2, .top-3 {
    background-color: @cyprus;
    color: @white;
  }

  .top-4, .top-5 {
    background-color: transparent;
    color: @cyprus;
    border: 1px solid @cyprus;
    line-height: 18px;
  }

</style>
