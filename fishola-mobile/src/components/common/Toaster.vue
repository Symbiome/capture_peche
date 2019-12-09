<template>
  <div class="toaster error">
    <div><i class="icon-warning"/>{{errorMessage}}</div>
  </div>
</template>

<script lang="ts">

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Toaster extends Vue {

  @Prop() errorMessage!: string;

  mounted() {
    console.log(this.errorMessage);
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";

  .toaster {
    position:absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 50px;
    z-index: 999;

    // @keyframes example {
    //   from {background-color: red;}
    //   to {background-color: yellow;}
    // }

    display: flex;
    justify-content: center;
    align-items: center;

    div {

      font-size: 10px;
      line-height: 12px;

      i {
        margin-right: 6px;
        font-size: 12px;
      }

    }
  }

  .toaster.error {
    color: @white;
    background: @cardinal;
  }

</style>
