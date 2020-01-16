<template>
  <div class="header-title">
    <img src="img/logo-small.svg" alt="Fishola" />
    <span v-if="envName" class="env">({{envName}})</span>
    <!--
    <img src="img/logo-FISHOLA-logo-negatif.svg" alt="Fishola" />
    <img src="img/logo-FISHOLA-nom-negatif.svg" alt="Fishola" />
    -->
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Title extends Vue {

  envName?:string = process.env.VUE_APP_ENV;

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.header-title {
  font-size: 24px;

  img {
    height: 30px;
  }

  span.env {
    color: @terra-cotta;
    font-size: 16px;
  }
}
</style>
