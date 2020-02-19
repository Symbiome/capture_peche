<template>
  <div class="pastille color0" v-on:click="$root.$emit('toaster-warning', 'Work in progress');">
    <span>{{initials}}</span>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Avatar extends Vue {

  @Prop() initials:string;

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";

  .pastille.color0 {
    color: @gunmetal;
    background: @avatar-background;
  }

</style>
