<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #L%
  -->
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
import FeedbackService from '@/services/FeedbackService';
import KeyboardManager from '@/services/KeyboardManager';

import { Component, Vue } from 'vue-property-decorator';
import ReferentialService from './services/ReferentialService';
import DocumentationService from './services/DocumentationService';
import ProfileService from './services/ProfileService';
import GeolocationService from './services/GeolocationService';
import { Plugins, AppState, StatusBarStyle } from '@capacitor/core';
const { SplashScreen, StatusBar} = Plugins;
import router from '@/router';

const { App } = Plugins;

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
       // Configure Keyboard & Status bar
      KeyboardManager.setupKeyboardConfiguration();
      StatusBar.setBackgroundColor({"color": "#1E9BC4"});

      // If app is opened externally (typically from mails when validating account or password forgotten)
      App.addListener('appUrlOpen', (data: any) => {
        // Catch any URL like %%/security/ACTION?t=TOKEN
        console.info("Opening from external url " + data.url);
        const start = data.url.indexOf('security');
        if (start > 0 && data.url.indexOf('?t=') > 0) {
          const actionAndToken = data.url.substring(start + 'security'.length + 1);
          const action = actionAndToken.substring(0, actionAndToken.indexOf('?'));
          const token = actionAndToken.substring(actionAndToken.indexOf('=') + 1);
          if ('reset-password' === action) {
            console.info("Detected reset password request");
            router.push({name:'reset-password', params: {token: token}});
          } else if ('verify' === action) {
            console.info("Detected verify request");
            router.push({name:'verify', params: {token: token}});
          }
          // Hide splashscreen
          SplashScreen.hide();
          StatusBar.show();
        }
      });

      ReferentialService.prepareCaches()
        .then(
          () => console.debug("Préparation des caches du référentiel terminée"),
          (error) => console.error("Erreur lors de la préparation des caches du référentiel", error)
        );
      DocumentationService.prepareCaches()
        .then(
          () => console.debug("Préparation des caches de documentation terminée"),
          (error) => console.error("Erreur lors de la préparation des caches de documentation", error)
        );
      ProfileService.prepareCaches()
        .then(
          () => console.debug("Préparation des caches du profil utilisateur terminée"),
          (error) => console.error("Erreur lors de la préparation des caches du profil utilisateur", error)
        );
      this.checkOutOfSyncTrips();
      const syncDelay = 30000;
      console.debug(`setInterval(${syncDelay/1000}s) pour surveiller les sorties à synchro`);
      this.interval = setInterval(this.checkOutOfSyncTrips, syncDelay);

      this.$root.$on('ask-for-sync-check', this.checkOutOfSyncTrips);     
    }

    beforeDestroy() {
      this.$root.$off('ask-for-sync-check');
      clearInterval(this.interval);
      this.stopWatchingPosition();
    }

    checkOutOfSyncTrips() {
      // console.debug("SYNCHO : Recherche des sorties");
      TripsService.syncTrips().then(this.tripsSyncFinished, (e) => {
        console.error("Apparement, il y a un pb de sync", e);
        // Même en cas d'erreur on essaye de synchro les photos
        this.checkOutOfSyncPicturesAndFeedbacks();
      });
    }

    tripsSyncFinished(someTripsSaved:boolean) {
      this.checkOutOfSyncPicturesAndFeedbacks();
      if (someTripsSaved) {
        this.$root.$emit('trips-saved');
      }
    }

    checkOutOfSyncPicturesAndFeedbacks() {
      // console.debug("SYNCHO : Recherche des photos");
      PicturesService.syncPictures();
      // Check for out of sync feedbacks any time we check for pictures
      FeedbackService.syncFeedbacks();
    }

    stopWatchingPosition() {
      GeolocationService.stopWatchingPosition();
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
    &.keyboardShowing {
      margin-top: env(safe-area-inset-top);
      // Take reduced footer height into account
      height: calc(100%  - env(safe-area-inset-top) - @reduced-footer-height);
    }
  }
}

.full-background {
  background-image: url("/img/background_transparent.png");
  background-repeat: no-repeat;
  background-size: 100%;
  background-position: center;
}

.shifted-background {
  background-image: url("/img/background.png");
  background-repeat: no-repeat;
  background-size: 100% auto;
  background-position: top;
  // Very small resolution: scale down kacground to make it fit widht
  background-position-y: -1vw;
  @media(min-width:200px) {
    background-position-y: -3vw;
  }
  // Resolutions larger than background: strech background width
  @media (min-width: 350px) {
    background-size: cover;
    background-position-y: -4vw;
  }
}

.secondary-header {
  padding-top: @margin-header-top;
  padding-bottom: @margin-header-top;
  padding-left: @margin-medium;
  padding-right: @margin-medium;
  line-height: @fontsize-top1;
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
  margin-top: @vertical-margin-small;

  height: calc(100% - @header-height - @secondary-header-height - @footer-height - 10px);
  &.keyboardShowing {
    // Take reduced footer height into account
    height: calc(100% - env(safe-area-inset-top) - @reduced-footer-height - 10px);
  }
  color: @gunmetal;

  z-index: 10;

  h1 {
    margin-top: calc(@fontsize-title * 1.5);
    margin-bottom: calc(@fontsize-title * 1.5);
    height: calc(@fontsize-title + @line-height-padding-xx-large);
    font-style: normal;
    font-weight: normal;
    font-size: @fontsize-title;
    line-height: calc(@fontsize-title + @line-height-padding-xx-large);
    color: @pelorous;
    text-align: center;

    @media(max-height:579px) {
      margin-top: calc(@fontsize-title * 1.1);
      margin-bottom: calc(@fontsize-title * 1.1);
      line-height: calc(@fontsize-title + @line-height-padding-large);
    }

    &.keyboardShowing {
      margin-top:10px;
      margin-bottom: @vertical-margin-xx-small;
      height: @fontsize-title-keyboardshowing;
      line-height: @fontsize-title-keyboardshowing;
      font-size: @fontsize-title-keyboardshowing;
    }

  }

  .pane-content {

    overflow: auto;

    padding-left: @margin-large;
    padding-right: @margin-large;

    &.large {
      padding-left: unset;
      padding-right: unset;
    }

    &.rounded {
      border-top-left-radius: 30px;
      border-top-right-radius: 30px;
    }
  }

  &.pane-only {
    margin-top: @vertical-margin-medium;
  }

}

.picture-background {
  .pane {
    // TODO responsive
    margin-top: 95px;
  }
}

.v--modal-overlay {
  background-color: @black-alpha-50 !important;

  .v--modal {
    border-radius: 14px;
    background-color: @white-smoke;
  }

  .v--modal-box {
    top: calc(100vh / 2 - 80px) !important;

    .dialog-content {
      text-align: center;

      .dialog-c-title {
        color: @gunmetal;
        font-size: @fontsize-dialog-title;
        line-height: calc(@fontsize-dialog-title + @line-height-padding-large);
      }

      .dialog-c-text {
        color: @pale-sky;
        font-size: calc(@fontsize-dialog-text);
        line-height: calc(@fontsize-dialog-text + @line-height-padding-large);
      }

      ul {
        text-align: left;
      }
    }

    .vue-dialog-buttons {
      color: @pelorous;
      border-top: 1px solid @very-light-grey;

      button.vue-dialog-button {
        font-size: @fontsize-dialog-button !important;

        &:not(:first-of-type) {
          border-left: 1px solid @very-light-grey;
        }
      }
    }
  }
}
</style>
