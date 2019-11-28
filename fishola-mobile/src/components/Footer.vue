<template>
  <div class="footer">
    <div class="left">
      <i class="fas fa-sign-out-alt"></i>
    </div>
    <div class="middle">
      <i class="fas fa-tachometer-alt"></i>
    </div>
    <div class="right">
      <i class="fas fa-home"></i>
    </div>
  </div>
</template>

<script lang="ts">

import { Component, Prop, Vue } from 'vue-property-decorator';

export default {
  name: 'footer',
  components: {
  }
}

// @Component
// export default class Header extends Vue {
// }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.footer {

  height: 100px;
  width: 100%;
  background-color: @zircon;
  color: @pelorous;

  position: absolute;
  bottom: 0;
  right: 0;

  .left {
    width: 33%;
    float: left;
    text-align: center;
    line-height: 100px;
  }
  .middle {
    width: 33%;
    float: left;
    text-align: center;
    line-height: 100px;
  }
  .right {
    width: 33%;
    float: left;
    text-align: center;
    line-height: 100px;
  }
}
</style>
