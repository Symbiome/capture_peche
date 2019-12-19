<template>
  <div id="app">
    <Toaster/>
    <router-view/>
  </div>
</template>

<script lang="ts">

import Toaster from '@/components/common/Toaster.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    Toaster
  }
})
export default class App extends Vue {

}

</script>

<style lang="less">

@import "less/main";

body {
  background-color: @pelorous;
  margin: 0px;
  height: 100%;
}

html {
  height: 100%;
}

* {
  box-sizing: border-box;
}

#app {
  font-family: 'Open Sans', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: @white;
  height: 100%;
}


.page-with-header {

  display: flex;
  flex-direction: column;

  height: 100%;

  .page {
    height: calc(100% - @header-height);
  }
}

.full-background {
  background-image: url("/img/background_transparent.png");
  // background-image: url("/img/background.png");
  background-repeat: no-repeat;
  background-size: 100%;
  background-position: center;
}

.shifted-background {
  background-image: url("/img/background.png");
  background-repeat: no-repeat;
  background-size: 100%;
  background-position: center;
  background-position-y: -230px;

}

</style>
