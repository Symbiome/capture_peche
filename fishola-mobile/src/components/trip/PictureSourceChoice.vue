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
  <div v-show="visible" class="picture-source-chooser">
    <div class="transparent-background" @click="$emit('close')"></div>
    <div class="popup-content">
      <div class="bottom-actions">
        <h4>Ajouter une image</h4>
        <div>
          <div
            v-if="isMobilePlatform"
            class="picture-source-item first-choice"
            @click="takePicture(false)"
          >
            <i class="pic icon-gallery" /><span>Depuis la galerie</span>
          </div>
          <div
            v-if="isMobilePlatform"
            class="picture-source-item"
            @click="takePicture(true)"
          >
            <i class="pic icon-photo" /><span>Depuis l'appareil photo</span>
          </div>
          <div
            ref="webPictureInput"
            v-if="!isMobilePlatform"
            class="picture-source-item"
            @click="takePicture(true)"
          >
            <i class="pic icon-photo" />Choisir une photo
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import PictureTakerService from "@/services/PictureTakerService";
import { Device } from "@capacitor/device";

@Component({})
export default class PictureSourceChoice extends Vue {
  @Prop({ default: false }) directlyOpenGaleryInWebMode: boolean;
  @Prop({ default: false }) visible: boolean;
  pictureSrc = "";
  errorMessage = "";
  isMobilePlatform = true;

  @Watch("visible")
  visbilityChanged(): void {
    if (this.visible) {
      if (!this.isMobilePlatform && this.directlyOpenGaleryInWebMode) {
        // Tentative d'ouvrir l'APN au chargement MAIS
        // Mais : https://stackoverflow.com/questions/33911801/input-file-click-no-working-no-event
        // Et : https://stackoverflow.com/questions/29728705/trigger-click-on-input-file-on-asynchronous-ajax-done/29873845#29873845
        setTimeout(() => {
          this.takePicture(true);
        }, 350);
      }
    }
  }
  mounted(): void {
    Device.getInfo().then((info) => {
      this.isMobilePlatform =
        info &&
        (info.operatingSystem === "android" || info.operatingSystem === "ios");
      // On écoute l'évenement "annuler" sur le fileinput pour ferme la popup
      const emitter = this;
      document.body.onfocus = function() {
        setTimeout(() => {
          const fileInput = document.getElementById("_capacitor-camera-input");
          const selectedFile = (fileInput as HTMLInputElement)?.files?.length;
          if (!selectedFile) {
            emitter.$emit("close");
          }
        }, 350);
      };
    });
  }

  async takePicture(fromCameraIfPossible: boolean) {
    this.pictureSrc = "";
    this.errorMessage = "";
    try {
      this.pictureSrc = await PictureTakerService.INSTANCE.takePicture(
        fromCameraIfPossible
      );
      if (this.pictureSrc) {
        this.$emit("pictureTaken", this.pictureSrc);
      } else {
        this.errorMessage =
          "Une erreur est survenue lors de la prise de photo, veuillez réessayer";
        this.$emit("close");
      }
    } catch (error) {
      this.errorMessage =
        "Une erreur est survenue lors de la prise de photo, veuillez réessayer";
      console.error("Error while taking picture", error);
      this.pictureSrc = "";
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less" scoped>
@import "../../less/main";

.picture-source-chooser {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 100vh;
  width: 100%;
  z-index: 99;
  color: black;
  background-color: rgba(0, 0, 0, 0.6);

  @media screen and (min-width: @desktop-min-width) {
    width: calc(100% - @desktop-menu-width);
    margin-left: @desktop-menu-width;
  }

  .title {
    color: @pelorous;
  }
  .transparent-background {
    height: 68vh;
  }
  .popup-content {
    margin-top: 0px !important;
    height: 32vh;
    overflow-y: auto;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    padding-bottom: 10vh;

    .bottom-actions {
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      background-color: @solitude;
      padding-left: @margin-large;
      padding-right: @margin-large;
      padding-bottom: @margin-medium;

      @media screen and (min-width: @desktop-min-width) {
        padding-left: @margin-large-desktop;
        padding-right: @margin-large-desktop;
      }

      .picture-source-item {
        padding-bottom: 5px;
        cursor: pointer;
        display: flex;
        align-items: center;

        .pic {
          color: @pale-sky !important;
          padding-right: 10px;
          font-size: x-large;
        }

        &.first-choice {
          padding-bottom: 20px;
        }
      }
    }

    .error {
      color: @cardinal;
    }
  }
}
</style>
