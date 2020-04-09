<template>
  <div id="app">
    <v-dialog :width="270"/>
    <Toaster/>
    <Menu/>
    <FeedbackModal/>
    <div id="root">
      <router-view/>
    </div>
  </div>
</template>

<script lang="ts">

import Toaster from '@/components/layout/Toaster.vue'
import Menu from '@/components/layout/Menu.vue'
import FeedbackModal from '@/components/layout/FeedbackModal.vue'

import TripsService from '@/services/TripsService';
import PicturesService from '@/services/PicturesService';

import { Component, Vue } from 'vue-property-decorator';

@Component({
  components: {
    Toaster,
    Menu,
    FeedbackModal
  }
})
export default class AppView extends Vue {

    interval?:number;

    created() {
      this.checkOutOfSyncTrips();
      let syncDelay = 30000;
      console.log(`setInterval(${syncDelay/1000}s) pour surveiller les sorties à synchro`);
      this.interval = setInterval(this.checkOutOfSyncTrips, syncDelay);

      this.$root.$on('ask-for-sync-check', this.checkOutOfSyncTrips);
    }

    beforeDestroy() {
      this.$root.$off('ask-for-sync-check');
      clearInterval(this.interval);
    }

    checkOutOfSyncTrips() {
      // console.log("SYNCHO : Recherche des sorties");
      TripsService.syncTrips().then(this.tripsSyncFinished, (e) => {
        console.log("Apparement, il y a un pb de sync", e);
        // Même en cas d'erreur on essaye de synchro les photos
        this.checkOutOfSyncPictures();
      });
    }

    tripsSyncFinished(someTripsSaved:boolean) {
      this.checkOutOfSyncPictures();
      if (someTripsSaved) {
        this.$root.$emit('trips-saved');
      }
    }

    checkOutOfSyncPictures() {
      // console.log("SYNCHO : Recherche des photos");
      PicturesService.syncPictures();
    }
}

</script>

<style lang="less">

@import "less/main";

body {
  background-color: @pelorous;
  margin: 0px;
  height: 100%;
  overflow: hidden;
  overflow-x: hidden;
  width: 100vw;
}

html {
  height: 100%;
}

* {
  box-sizing: border-box;
  -webkit-overflow-scrolling: touch;
}

#app {
  font-family: 'Open Sans', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: @white;
  height: 100%;
  max-height: 100%;
  max-width: 100%;
}

#root {
  height: 100%;
  width: 100%;
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

  z-index: 10;

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

  &.pane-only {
    margin-top: 20px;
  }

}

.picture-background {
  .pane {
    margin-top: 95px;
  }
}

.v--modal-overlay {
  background-color: @black-alpha-70 !important;

  .v--modal {
    border-radius: 14px;
    background-color: @white-smoke;
  }

  .v--modal-box {
    top: calc(100vh / 2 - 80px) !important;

    .dialog-content {
      text-align: center;

      .dialog-c-title {
        color: @black-alpha-90;
        font-size: 17px;
        line-height: 22px;
      }

      .dialog-c-text {
        color: @black-alpha-60;
        font-size: 13px;
        line-height: 18px;
      }
    }

    .vue-dialog-buttons {
      color: @dodger-blue;
      border-top: 1px solid @black-alpha-15;

      button.vue-dialog-button {
        font-size: 17px !important;

        &:not(:first-of-type) {
          border-left: 1px solid @black-alpha-15;
        }
      }
    }
  }
}
</style>
