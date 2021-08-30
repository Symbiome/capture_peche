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
    <div class="transparent-background" @click="$emit('close')"></div>
    <div class="pane popup-content">
      <div class="pane-content">
        <h1>Mesure automatique</h1>
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
            id="sourcePicture"
            class="picture-display"
            @load="sourcePictureLoaded"
            :src="measurementPictureSrc"
          />
        </div>
        <!-- No picture taken : display slider + camera/gallery choice -->
        <div v-if="!measurementPictureSrc">
          <div class="preconisations">
            <h4>Préconisations</h4>
            SLIDER
          </div>
          <div class="bottom-actions">
            <h4>Ajouter une image</h4>
            <div v-if="openCVLoaded">
              <div
                class="picture-source-item"
                @click="takePictureAndTryToMeasure(false)"
              >
                <i class="pic icon-photo" />Depuis la galerie
              </div>
              <div
                class="picture-source-item"
                @click="takePictureAndTryToMeasure(true)"
              >
                <i class="pic icon-photo" />Depuis l'appareil photo
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
                <i class="icon-success" /> {{ fishSize }}mm
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
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import PictureTakerService from "@/services/PictureTakerService";
import FisholaOpenCVService from "@/services/opencv/FisholaOpenCVService";
import { DetectedShape } from "@/services/opencv/DetectedShape";
import { OpenCVDetectionConfig } from "@/services/opencv/OpenCVDetectionConfig";

@Component({
  components: {},
})
export default class MeasurementPicturePopup extends Vue {
  measurementPictureSrc = "";
  markerSourceSRC = "";
  calculating = false;
  openCVLoaded = false;
  errorMessage = "";
  openCVConfig = new OpenCVDetectionConfig();
  fishSize = 0;
  markerFound = false;

  mounted(): void {
    this.markerSourceSRC = this.openCVConfig.defaultMarkerSrc;
    FisholaOpenCVService.INSTANCE.loadOpenCVIfNeeded().then(() => {
      this.openCVLoaded = FisholaOpenCVService.INSTANCE.isOpenCVReady();
    });
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
      this.errorMessage =
        "Une erreur est survenue lors de la prise de photo, veuillez réessayer";
      console.log("Error while taking measure picture", error);
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
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
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

  .transparent-background {
    height: 30vh;
  }
  .popup-content {
    margin-top: 0px !important;
    height: 70vh;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;

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

        .pic {
          color: @pale-sky !important;
          padding-right: 10px;
          font-size: large;
        }
      }
    }

    .picture-display {
      max-height: 50vh;
      max-width: 90vw;
      padding-left: 5vw;
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
    left: calc(45vw - 60px);
    display: block;
  }
}
</style>
