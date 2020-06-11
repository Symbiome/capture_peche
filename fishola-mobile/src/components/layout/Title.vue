<template>
  <div class="header-title" v-on:click="goHome">
    <img src="img/logo-small.svg" alt="FISHOLA" />
    <span v-if="envName" class="env">({{envName}})</span>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

import router from '@/router';

@Component
export default class Title extends Vue {

  envName?:string = process.env.VUE_APP_ENV;

  goHome() {
    router.push('/trips');
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.header-title {    
  font-size: 24px;
  img {
    height: 30px;
  }
  span.env {
    color: @terra-cotta;
    font-size: 16px;
  }
  @media(max-width:350px) {
    font-size: 14px;
    img {
      height: 20px;
      margin-top:5px;
    }
    span.env {
      font-size: 12px;
    }
  }

  @media(max-width:280px) {
    img {
      height: 15px;
      margin-top:7px;
    }
    span.env {
      display:none;
    }
  }
}
</style>
