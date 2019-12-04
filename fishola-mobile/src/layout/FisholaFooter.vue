<template>
  <div class="footer">
    <a class="footer-element pastille unfilled" v-on:click="logout">
      <i class="icon-logout"></i>
    </a>
    <div class="footer-element pastille unfilled">
      <i class="icon-dashboard"></i>
    </div>
    <div class="footer-element pastille filled">
      <i class="icon-home"></i>
    </div>
  </div>
</template>

<script lang="ts">

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class FisholaFooter extends Vue {

  logout() {
    window.alert("logout");
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.footer {

  display: flex;
  justify-content: space-around;
  align-items: center;

  height: 100px;
  width: 100%;
  background-color: @zircon;
  color: @pelorous;

  position: absolute;
  bottom: 0;
  right: 0;

  .footer-element {
    height: 40px;
    width: 40px;
    line-height: 40px;
    text-align: center;

    i {
      font-size: 19px;
    }
  }
}
</style>
