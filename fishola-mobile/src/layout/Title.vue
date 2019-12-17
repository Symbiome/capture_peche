<template>
  <div class="header-title">
    <img src="img/logo/logo-ligne-negatif.svg" alt="Fishola" />
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
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

.header-title {
  font-size: 24px;

  img {
    height: 20px;
  }
}
</style>
