<template>
  <div class="header-title">
    Fishola
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

@font-face {
    font-family: 'Bridamount';
    src: url('/bridamount.ttf');
}

.header-title {
  font-family: 'bridamount', 'Avenir', Helvetica, Arial, sans-serif;
  font-size: 24px;
}
</style>
