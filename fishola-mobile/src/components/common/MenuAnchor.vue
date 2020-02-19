<template>
  <div class="pastille filled" v-on:click="openMenu">
    <i class="icon-menu"></i>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class MenuAnchor extends Vue {

  openMenu() {
    this.$root.$emit('open-menu');
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

</style>
