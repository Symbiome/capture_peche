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
  <div class="picture-source-chooser">
    <img
      v-show="false"
      id="sourcePicture"
      style="position:absolute"
      @load="sourcePictureLoaded(0)"
      :src="measurementPictureSrc"
    />
    <div class="transparent-background" @click="$emit('close')"></div>
    <div class="pane popup-content">
      <div class="pane-content">
        <h2 class="title">Mesure automatique</h2>
        <h4 v-if="errorMessage" class="error">{{ errorMessage }}</h4>
        <!-- pictures required for measurement -->
        <div class="picture-holder">
          <img id="marker" v-show="false" :src="markerSourceSRC" />
          <canvas
            v-show="measurementPictureSrc && !calculating"
            id="resultCanvas"
            class="picture-display"
          />
          <img
            v-show="calculating"
            class="picture-display"
            :src="measurementPictureSrc"
          />
        </div>
        <!-- No picture taken : display slider + camera/gallery choice -->
        <div v-if="!measurementPictureSrc">
          <MeasurementPictureSlider />
          <div class="bottom-actions">
            <h4>Ajouter une image</h4>
            <div v-if="openCVLoaded">
              <div
                v-if="isMobilePlatform"
                class="picture-source-item first-choice"
                @click="takePictureAndTryToMeasure(false)"
              >
                <i class="pic icon-gallery" /><span>Depuis la galerie</span>
              </div>
              <div
                v-if="isMobilePlatform"
                class="picture-source-item"
                @click="takePictureAndTryToMeasure(true)"
              >
                <i class="pic icon-photo" /><span>Depuis l'appareil photo</span>
              </div>
              <div
                v-if="!isMobilePlatform"
                class="picture-source-item"
                @click="takePictureAndTryToMeasure(true)"
              >
                <i class="pic icon-photo" />Choisir une photo de mesure
              </div>
            </div>

            <div v-else>
              Chargement en cours...
            </div>
          </div>
        </div>
        <!-- Picture taken -->
        <div v-else>
          <!-- Still calculating : show taken picture & loader !-->
          <div v-if="calculating" class="loading">
            <div class="spinner">&nbsp;</div>
          </div>
          <!-- Calulation is over: display result -->
          <div v-else>
            <h4>
              <span class="measure">Mesure </span>
              <!-- Correct measure -->
              <div class="success" v-if="markerFound && fishSize">
                <i class="icon-success" /> {{ Math.round(fishSize / 10) }}cm
              </div>
              <div class="warning" v-if="markerFound && fishSize">
                <i class="icon-warning" /> Cette fonctionnalité est
                expérimentale, cette mesure peut être imprécise
              </div>
              <div class="error" v-else>
                <i class="icon-error" />
                <span v-if="!markerFound">
                  Impossible de détecter le marqueur
                </span>
                <span v-else>
                  Impossible de détecter le poisson
                </span>
                <br />
                Veuillez vérifier que votre photo suit bien les préconisations
              </div>
            </h4>
            <div class="bottom-actions validate-redo">
              <div class="button button-primary button-main">
                <button v-if="markerFound && fishSize" @click="validate">
                  <i class="icon-fish" />
                  Valider
                </button>
                <button v-else @click="measurementPictureSrc = ''">
                  <i class="icon-redo" />
                  Recommencer
                </button>
              </div>
              <div class="button-minor-left">
                <button
                  @click="measurementPictureSrc = ''"
                  v-if="markerFound && fishSize"
                >
                  <i class="icon-redo" />
                </button>
              </div>
              <div class="button-minor-right">
                <button v-on:click="$emit('close')">
                  Abandon
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import PictureTakerService from "@/services/PictureTakerService";
import FisholaOpenCVService from "@/services/opencv/FisholaOpenCVService";
import { DetectedShape } from "@/services/opencv/DetectedShape";
import { OpenCVDetectionConfig } from "@/services/opencv/OpenCVDetectionConfig";
import { Device } from "@capacitor/device";
import MeasurementPictureSlider from "@/components/trip/MeasurementPictureSlider.vue";
import { MeasureAndPic } from "@/services/opencv/MeasureAndPic";

@Component({
  components: { MeasurementPictureSlider },
})
export default class MeasurementPicturePopup extends Vue {
  @Prop() measurementPicture: string;
  measurementPictureSrc = "";
  markerSourceSRC = "";
  calculating = false;
  openCVLoaded = false;
  errorMessage = "";
  openCVConfig = new OpenCVDetectionConfig();
  fishSize = 0;
  markerFound = false;
  isMobilePlatform = true;

  created(): void {
    Device.getInfo().then((info) => {
      this.isMobilePlatform =
        info &&
        (info.operatingSystem === "android" || info.operatingSystem === "ios");
    });
  }
  mounted(): void {
    this.markerSourceSRC = this.openCVConfig.defaultMarkerSrc;
    this.errorMessage = "";
    FisholaOpenCVService.INSTANCE.loadOpenCVIfNeeded().then(() => {
      this.openCVLoaded = FisholaOpenCVService.INSTANCE.isOpenCVReady();
    });
  }

  @Watch("measurementPicture")
  measurementPictureChanged() {
    if (this.measurementPicture) {
      this.measurementPictureSrc = this.measurementPicture;
    }
  }

  async takePictureAndTryToMeasure(fromCameraIfPossible: boolean) {
    this.measurementPictureSrc = "";
    this.calculating = true;
    this.errorMessage = "";
    this.fishSize = 0;
    this.markerFound = false;
    try {
      // Step 1: take picture
      this.measurementPictureSrc = await PictureTakerService.INSTANCE.takePicture(
        fromCameraIfPossible
      );
      // Step 2: launch calculation
    } catch (error) {
      console.error("Error while taking measure picture", error);
      this.measurementPictureSrc = "";
      this.calculating = false;
    }
  }

  async sourcePictureLoaded() {
    try {
      console.info(
        "Source picture loaded, launching calculation",
        this.calculating
      );
      const imageElement = document.getElementById("sourcePicture");
      const markerElement = document.getElementById("marker");
      if (this.openCVLoaded && imageElement && markerElement) {
        this.openCVConfig.drawDebugCanvas = false;
        const detectedShapes: Array<DetectedShape> = await FisholaOpenCVService.INSTANCE.calculateAndDrawFishSizes(
          imageElement,
          markerElement,
          this.openCVConfig,
          "resultCanvas"
        );

        const markers = detectedShapes.filter(
          (shape: DetectedShape) => shape.isMarker
        ).length;
        this.markerFound = markers === 1;
        const fishes = detectedShapes.filter(
          (shape: DetectedShape) => shape.isFish
        );
        if (fishes.length === 1) {
          this.fishSize = fishes[0].calculatedLenght;
        }
        this.calculating = false;
      } else {
        this.errorMessage =
          "Impossible de déterminer la mesure automatiquement, veuillez réessayer";
      }

      return;
      // Step 2: launch calculation
    } catch (error) {
      this.errorMessage =
        "Impossible de déterminer la mesure automatiquement, veuillez réessayer";
    }
    this.measurementPictureSrc = "";
    this.calculating = false;
  }

  validate() {
    const measureAndPic = new MeasureAndPic(
      this.fishSize,
      this.measurementPictureSrc
    );
    this.$emit("measurementPictureTaken", measureAndPic);
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
  background-color: rgba(0, 0, 0, 0.6);

  @media screen and (min-width: @desktop-min-width) {
    width: calc(100% - @desktop-menu-width);
    margin-left: @desktop-menu-width;
  }

  .title {
    color: @pelorous;
    font-weight: normal;
    font-size: calc(@fontsize-title + 5px);
  }
  .transparent-background {
    height: 18vh;
  }
  .popup-content {
    margin-top: 0px !important;
    height: 100vh;
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

    .validate-redo {
      padding-bottom: 20px;
      padding-top: 30px;
    }

    .picture-holder {
      background-color: @gainsboro;
    }
    .picture-display {
      max-height: 40vh;
      max-width: 90vw;
      display: block;
      margin-left: auto;
      margin-right: auto;
    }

    .error {
      color: @cardinal;
    }
    .success {
      color: @lime-green;
    }
    .warning {
      color: @terra-cotta;
    }

    .measure {
      float: left;
      padding-right: 10px;
    }
  }

  @keyframes spin {
    100% {
      -webkit-transform: rotate(360deg);
      transform: rotate(360deg);
    }
  }

  .spinner {
    height: 60px;
    width: 60px;
    border-radius: 50%;
    border-top: 3px solid @pelorous;
    border-left: 3px solid @pelorous;
    animation: spin 2s linear infinite;
    position: absolute;
    top: 48vh;
    left: calc(50vw - 30px);
    display: block;
  }

  @media screen and (min-width: @desktop-min-width) {
    .spinner {
      left: calc(50vw - 30px - @desktop-menu-width / 2);
    }
  }

  .button-main {
    margin-left: auto;
    margin-right: auto;
    margin-top: -55px;
  }

  .button-minor-left {
    margin-right: auto;
    float: left;
    button {
      color: @pelorous;
      font-weight: bold;
      background-color: rgba(0, 0, 0, 0);
      border: 0px solid black;
      font-size: 18px;
    }
  }

  .button-minor-right {
    margin-left: auto;
    float: right;
    button {
      color: @pelorous;
      font-weight: bold;
      background-color: rgba(0, 0, 0, 0);
      border: 0px solid black;
      font-size: 16px;
    }
  }
}
</style>
