<template>
  <div id="app">
    <Toaster/>
    <router-view/>
  </div>
</template>

<script lang="ts">

import Toaster from '@/components/common/Toaster.vue'

import TripsService from '@/services/TripsService';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    Toaster
  }
})
export default class App extends Vue {
    created() {
      this.checkOutOfSyncTrips();
      let syncDelay = 30000;
      console.log(`setInterval(${syncDelay/1000}s) pour surveiller les sorties à synchro`);
      setInterval(this.checkOutOfSyncTrips, syncDelay);
    }

    checkOutOfSyncTrips() {
      console.log("Y'a-t'il des sorties à synchronizer ?");
      TripsService.syncTrips();
    }
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

.page-with-header-and-footer {

  display: flex;
  flex-direction: column;

  height: 100%;

  .page {

    display: flex;
    flex-direction: column;
    justify-content: space-between;

    height: calc(100% - @header-height - @footer-height);
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

.secondary-header {
  padding-top: 10px;
  padding-bottom: 10px;
  padding-left: 20px;
  padding-right: 20px;
  line-height: 20px;
  height: @secondary-header-height;
}

.pane {

  flex:auto;

  display: flex;
  flex-direction: column;
  justify-content: flex-start;

  background-color: @white-smoke;
  border-top-left-radius: 30px;
  border-top-right-radius: 30px;
  padding-top: 0px;
  margin-top: 10px;

  height: calc(100% - @header-height - @secondary-header-height - @footer-height - 10px);

  color: @gunmetal;

  h1 {
    margin-top: 40px;
    margin-bottom: 40px;
    height: 30px;
    font-style: normal;
    font-weight: normal;
    font-size: 22px;
    line-height: 30px;
    color: @pelorous;
    text-align: center;
  }

  .pane-content {

    overflow: auto;

    padding-left: 30px;
    padding-right: 30px;

  }

  .pane-content-large {

    overflow: auto;

  }

}

</style>
