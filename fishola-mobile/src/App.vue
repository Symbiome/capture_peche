<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
    <v-dialog :width="270" />
    <Toaster />
    <component :is="layout()" class="layout">
      <router-view />
    </component>
  </div>
</template>

<script lang="ts">
import Helpers from "@/services/Helpers";
import { RouterUtils } from "@/router/RouterUtils";

import Toaster from "@/components/layout/Toaster.vue";

import TripsService from "@/services/TripsService";
import PicturesService from "@/services/PicturesService";
import FeedbackService from "@/services/FeedbackService";
import KeyboardManager from "@/services/KeyboardManager";

import { Component, Vue } from "vue-property-decorator";
import ReferentialService from "./services/ReferentialService";
import DocumentationService from "./services/DocumentationService";
import ProfileService from "./services/ProfileService";
import GeolocationService from "./services/GeolocationService";
import router from "@/router";
import { StatusBar } from "@capacitor/status-bar";
import { SplashScreen } from "@capacitor/splash-screen";
import { App } from "@capacitor/app";

@Component({
  components: {
    Toaster,
  },
})
export default class AppView extends Vue {
  interval?: number;

  created() {
    this.initApp();
  }

  layout() {
    if (this.$route.meta) {
      return (this.$route.meta.layout || "default") + "-layout";
    } else {
      return "default-layout";
    }
  }

  // TODO AThimel 07/12/2020 : Déplacer ça dans un service dédié à l'initialisation de l'application
  initApp() {
    // Configure Keyboard & Status bar
    Helpers.ifApplication(() => {
      KeyboardManager.setupKeyboardConfiguration();
      StatusBar.setBackgroundColor({ color: "#1E9BC4" });
    });

    // If app is opened externally (typically from mails when validating account or password forgotten)
    App.addListener("appUrlOpen", (data: any) => {
      // Catch any URL like %%/security/ACTION?t=TOKEN
      console.info("Opening from external url " + data.url);
      const start = data.url.indexOf("security");
      if (start > 0 && data.url.indexOf("?t=") > 0) {
        const actionAndToken = data.url.substring(
          start + "security".length + 1
        );
        const action = actionAndToken.substring(0, actionAndToken.indexOf("?"));
        const token = actionAndToken.substring(actionAndToken.indexOf("=") + 1);
        if ("reset-password" === action) {
          console.info("Detected reset password request");
          RouterUtils.pushRouteNoDuplicate(router, {
            name: "reset-password",
            params: { token: token },
          });
        } else if ("verify" === action) {
          console.info("Detected verify request");
          RouterUtils.pushRouteNoDuplicate(router, {
            name: "verify",
            params: { token: token },
          });
        }

        // Hide splashscreen
        Helpers.ifApplication(() => {
          SplashScreen.hide();
          StatusBar.show();
        });
      }
    });

    ReferentialService.prepareCaches().then(
      () => console.debug("Préparation des caches du référentiel terminée"),
      (error) =>
        console.error(
          "Erreur lors de la préparation des caches du référentiel",
          error
        )
    );
    DocumentationService.prepareCaches().then(
      () => console.debug("Préparation des caches de documentation terminée"),
      (error) =>
        console.error(
          "Erreur lors de la préparation des caches de documentation",
          error
        )
    );
    ProfileService.prepareCaches().then(
      () =>
        console.debug("Préparation des caches du profil utilisateur terminée"),
      (error) =>
        console.error(
          "Erreur lors de la préparation des caches du profil utilisateur",
          error
        )
    );
    this.checkOutOfSyncTrips();
    const syncDelay = 30000;
    console.debug(
      `setInterval(${syncDelay / 1000}s) pour surveiller les sorties à synchro`
    );
    this.interval = setInterval(this.checkOutOfSyncTrips, syncDelay);

    this.$root.$on("ask-for-sync-check", this.checkOutOfSyncTrips);
  }

  beforeDestroy() {
    this.$root.$off("ask-for-sync-check");
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

  tripsSyncFinished(someTripsSaved: boolean) {
    this.checkOutOfSyncPicturesAndFeedbacks();
    if (someTripsSaved) {
      this.$root.$emit("trips-saved");
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
  width: 100vw;

  overflow: hidden;
  overflow-x: hidden;
}

html {
  height: 100%;
}

* {
  box-sizing: border-box;
  -webkit-overflow-scrolling: touch;
}

#app {
  font-family: "Open Sans", sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: @white;
  height: 100%;
  max-height: 100%;
  max-width: 100%;
}

.layout {
  height: 100%;
  width: 100%;

  @media screen and (min-width: @desktop-min-width) {
    display: flex;
    flex-direction: row;
  }
}

.page-with-header {
  display: flex;
  flex-direction: column;

  height: 100%;

  .page {
    height: calc(100% - @header-height);

    @media screen and (min-width: @desktop-min-width) {
      height: 100%;
    }
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
      height: calc(100% - env(safe-area-inset-top) - @reduced-footer-height);
    }

    @media screen and (min-width: @desktop-min-width) {
      height: 100%;
    }
  }
}

.full-background {
  background-image: url("~/public/img/background_transparent.png");
  background-repeat: no-repeat;
  background-size: 100%;
  background-position: center;
}

.shifted-background {
  background-image: url("~/public/img/background.png");
  background-repeat: no-repeat;
  background-size: 100% auto;
  background-position: top;
  // Very small resolution: scale down kacground to make it fit widht
  background-position-y: -1vw;
  @media (min-width: 200px) {
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
  flex: auto;

  display: flex;
  flex-direction: column;
  justify-content: flex-start;

  background-color: @white-smoke;
  border-top-left-radius: 30px;
  border-top-right-radius: 30px;
  padding-top: 0px;
  margin-top: @vertical-margin-small;

  height: calc(
    100% - @header-height - @secondary-header-height - @footer-height - 10px
  );
  &.keyboardShowing {
    // Take reduced footer height into account
    height: calc(
      100% - env(safe-area-inset-top) - @reduced-footer-height - 10px
    );
  }
  color: @gunmetal;

  z-index: 10;

  h1 {
    margin-top: @margin-large;
    margin-bottom: @margin-large;
    height: calc(@fontsize-title + @line-height-padding-xx-large);
    font-style: normal;
    font-weight: normal;
    font-size: @fontsize-title;
    line-height: calc(@fontsize-title + @line-height-padding-xx-large);
    color: @pelorous;
    text-align: center;

    @media (max-height: 579px) {
      margin-top: @margin-medium;
      margin-bottom: @margin-medium;
    }

    @media (max-height: 450px) {
      margin-top: @margin-small;
      margin-bottom: @margin-small;
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

  @media screen and (min-width: @desktop-min-width) {
    border-top-left-radius: unset;
    border-top-right-radius: unset;
    padding-top: 0px;
    margin-top: 0px;

    h1 {
      margin-top: @margin-medium;
      margin-bottom: @margin-xx-large;
      font-size: @fontsize-title-desktop;
      height: calc(@fontsize-title-desktop + @line-height-padding-xx-large);
      line-height: calc(
        @fontsize-title-desktop + @line-height-padding-xx-large
      );
      text-align: left;

      &.no-margin-pane {
        margin-left: @margin-large-desktop;
        margin-right: @margin-large-desktop;
      }
    }

    .pane-content {
      padding-left: @margin-large-desktop;
      padding-right: @margin-large-desktop;
    }

    &.pane-only {
      margin-top: 0px;
    }
  }
}

.picture-background {
  .pane {
    // TODO responsive
    margin-top: 95px;

    &.keyboardShowing {
      margin-top: 5px;
    }
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

    @media screen and (min-width: @desktop-min-width) {
      width: 350px !important;
    }

    .dialog-content {
      text-align: center;

      @media screen and (min-width: @desktop-min-width) {
        padding: 20px;
      }

      .dialog-c-title {
        color: @gunmetal;
        font-size: @fontsize-dialog-title;
        line-height: calc(@fontsize-dialog-title + @line-height-padding-large);

        @media screen and (min-width: @desktop-min-width) {
          font-size: calc(@fontsize-dialog-title-desktop);
          line-height: calc(
            @fontsize-dialog-title-desktop + @line-height-padding-large
          );
        }
      }

      .dialog-c-text {
        color: @pale-sky;
        font-size: calc(@fontsize-dialog-text);
        line-height: calc(@fontsize-dialog-text + @line-height-padding-large);

        @media screen and (min-width: @desktop-min-width) {
          font-size: calc(@fontsize-dialog-text-desktop);
          line-height: calc(
            @fontsize-dialog-text-desktop + @line-height-padding-large
          );
        }
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
