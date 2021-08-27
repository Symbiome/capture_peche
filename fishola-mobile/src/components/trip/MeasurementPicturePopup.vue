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
        <h3 v-if="errorMessage" class="errorMessage">{{ errorMessage }}</h3>
        <h3 v-if="!openCVLoaded">CHARGEMENT</h3>
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
          <div v-if="calculating">
            Loading...
          </div>
          <!-- Calulation is over: display result -->
          <div v-else></div>
        </div>
        <!-- pictures required for measurement -->
        <img id="marker" v-show="false" :src="markerSourceSRC" />
        <canvas
          v-show="measurementPictureSrc && !calculating"
          id="resultCanvas"
          class="picture-display"
        />
        <img
          v-show="measurementPictureSrc && calculating"
          id="sourcePicture"
          class="picture-display"
          :src="measurementPictureSrc"
        />
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

  mounted(): void {
    this.markerSourceSRC = this.openCVConfig.defaultMarkerSrc;
    FisholaOpenCVService.INSTANCE.loadOpenCVIfNeeded().then(() => {
      this.openCVLoaded = FisholaOpenCVService.INSTANCE.isOpenCVReady();
    });
  }

  async takePictureAndTryToMeasure(
    fromCameraIfPossible: boolean,
    numberOfRetries: number
  ) {
    this.calculating = true;
    this.errorMessage = "";
    try {
      // Step 1: take picture
      this.measurementPictureSrc = await PictureTakerService.INSTANCE.takePicture(
        fromCameraIfPossible
      );

      // Step 2: make sure opencv is loaded and launch measurement
      const imageElement = document.getElementById("sourcePicture");
      const markerElement = document.getElementById("marker");
      console.error(this.openCVLoaded, imageElement, markerElement);
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
        const fishes = detectedShapes.filter(
          (shape: DetectedShape) => shape.isFish
        ).length;
        console.error(
          "Found " + markers + " markers and " + fishes + " fishes "
        );
        this.calculating = false;
      } else {
        this.errorMessage =
          "Impossible de déterminer la mesure automatiquement, veuillez réessayer";
      }

      return;
      // Step 2: launch calculation
    } catch (error) {
      this.errorMessage =
        "Une erreur est survenue lors de la prise de photo, veuillez réessayer";
      console.log("Error while taking measure picture", error);
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
    }

    .errorMessage {
      color: red;
    }
  }
}
</style>
