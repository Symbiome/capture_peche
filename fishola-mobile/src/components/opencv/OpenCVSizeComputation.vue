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
  <div class="page-with-header-and-footer shifted-background">
    <div class="page">
      <div class="pane pane-only">
        <div class="pane-content large rounded">
          <div class="measure-tests">
            <h4 class="title">
              OpenCV - Mesure automatique de poisson

              <input
                v-if="openCVLoaded"
                type="file"
                id="fileInput"
                name="file"
                @change="changeSourceImage"
              />
            </h4>
            <div id="calculating">
              <span v-if="calculating">Calcul en cours...</span>
              <span v-if="!calculating && !calculated"
                >Aucun calcul en cours</span
              >
              <span v-if="!calculating && calculated">Calcul terminé</span>
              <span id="resultText" v-if="!calculating" v-html="resultText" />
            </div>

            <div class="result" v-show="calculated && !calculating">
              <div class="result-item">
                <canvas id="canvasOutput1"></canvas><br />
                <caption>
                  Original
                </caption>
              </div>
              <div class="result-item">
                <canvas id="canvasOutput2"></canvas><br />
                <caption>
                  Traité
                </caption>
              </div>
              <div class="result-item">
                <canvas id="canvasOutput3"></canvas><br />
                <caption>
                  Résultat
                </caption>
              </div>
            </div>

            <div class="params" v-if="openCVLoaded">
              <div class="params-item">
                <img
                  style="display: none"
                  id="sourcePicture"
                  alt="No Image"
                  @load="onNewImageSourceLoad"
                  :src="imageSourceSRC"
                />
                Taille marqueur (mm)
                <input
                  type="number"
                  style="width: 100px"
                  id="markerSizeInCm"
                  @change="launchSizeComputation"
                  v-model.number="config.markerSizeInMm"
                />
                <br />
                Redimensionnement (px)
                <input
                  type="number"
                  style="width: 100px"
                  id="resizeSize"
                  @change="launchSizeComputation"
                  v-model.number="config.resizeSize"
                /><br />
                <div class="caption">
                  Marqueur
                  <input
                    type="file"
                    id="markerFile"
                    name="file"
                    @change="changeMarkerImage"
                  />
                </div>
                <img
                  style="width: 80px; height: 80px"
                  id="marker"
                  alt=""
                  @load="onNewMarkerSourceLoad"
                  :src="markerSourceSRC"
                />
              </div>
              <div class="params-item">
                % d'occupation minimal (0 à 1)
                <input
                  type="number"
                  style="width: 100px"
                  id="minSizeRatio"
                  step="0.1"
                  @change="launchSizeComputation"
                  v-model.number="config.minSizeRatio"
                />
                <br />
                % d'occupation maximal (0 à 1)
                <input
                  type="number"
                  style="width: 100px"
                  id="maxSizeRatio"
                  step="0.1"
                  @change="launchSizeComputation"
                  v-model.number="config.maxSizeRatio"
                />
                <br />
                Ratio hauteur/largeur min (0 à 1)
                <input
                  type="number"
                  style="width: 100px"
                  id="minWithLengthRatio"
                  step="0.1"
                  @change="launchSizeComputation"
                  v-model.number="config.minWidthLengthRatio"
                />
                <br />
                Ratio hauteur/largeur min (0 à 1)
                <input
                  type="number"
                  style="width: 100px"
                  id="maxWithLengthRatio"
                  step="0.1"
                  @change="launchSizeComputation"
                  v-model.number="config.maxWidthLengthRatio"
                />
                FeatureMatching si 1 seul candidat
                <input
                  type="checkbox"
                  style="width: 100px"
                  id="alwaysCheckMarkerCandidates"
                  v-model.number="config.alwaysCheckMarkerCandidates"
                /><br />
                La photo contient un marqueur
                <input
                  type="checkbox"
                  style="width: 100px"
                  id="pictureIsSupposedToContainMarker"
                  v-model.number="config.pictureIsSupposedToContainMarker"
                /><br />
                La photo contient un poisson
                <input
                  type="checkbox"
                  style="width: 100px"
                  id="pictureIsSupposedToContainFish"
                  v-model.number="config.pictureIsSupposedToContainFish"
                /><br />
              </div>
            </div>
            <div id="status">
              <span v-if="!openCVLoaded">OpenCV.js is loading...</span>
              <span v-else>OpenCV.js is ready</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import FisholaOpenCVService from "@/services/opencv/FisholaOpenCVService";
import { DetectedShape } from "@/services/opencv/DetectedShape";
import { OpenCVDetectionConfig } from "@/services/opencv/OpenCVDetectionConfig";

@Component({})
export default class OpenCVSizeComputation extends Vue {
  imageSourceSRC = "";
  markerSourceSRC = "";
  config: OpenCVDetectionConfig = new OpenCVDetectionConfig();
  resultText = "";

  openCVLoaded = false;
  calculated = false;
  calculating = false;
  attempsCount = 0;
  stressTestMode = false;

  mounted(): void {
    this.markerSourceSRC = this.config.defaultMarkerSrc;
    FisholaOpenCVService.INSTANCE.loadOpenCVIfNeeded().then(() => {
      this.openCVLoaded = FisholaOpenCVService.INSTANCE.isOpenCVReady();
    });
  }

  changeSourceImage(e: Event): void {
    let eventTaget = e.target;
    // @ts-ignore
    if (eventTaget != null && eventTaget["files"]) {
      // @ts-ignore
      this.imageSourceSRC = URL.createObjectURL(eventTaget["files"][0]);
    }
  }

  changeMarkerImage(e: Event): void {
    let eventTaget = e.target;
    // @ts-ignore
    if (eventTaget != null && eventTaget["files"]) {
      // @ts-ignore
      this.markerSourceSRC = URL.createObjectURL(eventTaget["files"][0]);
      console.error(this.markerSourceSRC);
    }
  }

  onNewMarkerSourceLoad(e: Event): void {
    let eventTaget = e.target;
    // @ts-ignore
    if (eventTaget != null && eventTaget["files"]) {
      // @ts-ignore
      this.markerSourceSRC = URL.createObjectURL(eventTaget["files"][0]);
    }
  }

  onNewImageSourceLoad(_e: Event): void {
    this.calculating = true;
    this.$forceUpdate();
    setTimeout(this.launchSizeComputation, 200);
  }

  async launchSizeComputation(): Promise<void> {
    this.attempsCount++;
    const imageElement = document.getElementById("sourcePicture");
    const markerElement = document.getElementById("marker");
    this.calculating = true;
    if (imageElement && markerElement) {
      try {
        const detectedShapes: Array<DetectedShape> =
          await FisholaOpenCVService.INSTANCE.calculateAndDrawFishSizes(
            imageElement,
            markerElement,
            this.config,
            "canvasOutput3"
          );

        const markers = detectedShapes.filter(
          (shape: DetectedShape) => shape.isMarker
        ).length;
        const ignored = detectedShapes.filter(
          (shape: DetectedShape) => !shape.isMarker && !shape.isFish
        ).length;
        const notIgnored = detectedShapes.filter(
          (shape: DetectedShape) => shape.isFish || shape.isMarker
        );
        let result =
          ": <span id='shapesNumber'>" +
          (detectedShapes.length - ignored) +
          "</span> formes (" +
          markers +
          " marqueurs, " +
          (notIgnored.length - markers) +
          " poissons, " +
          ignored +
          " ignorées)";
        notIgnored.forEach((shape: DetectedShape) => {
          result += "<br/>- ";
          if (shape.isFish) {
            result += " Détecté : Poisson ";
          } else {
            result += " Détecté : Marqueur ";
          }
          result +=
            shape.calculatedLenght +
            "mm (" +
            Math.round(shape.width) +
            "px * " +
            Math.round(shape.height) +
            " px) - left " +
            Math.round(shape.leftX) +
            " top " +
            Math.round(shape.topY);
        });
        this.resultText = result;

        this.calculated = true;
        this.calculating = false;

        if (this.stressTestMode) {
          console.info("Automatic relaunch #" + this.attempsCount);
          setTimeout(this.onNewImageSourceLoad, 800);
        }
      } catch (error) {
        console.error("****", error);
      }
    }
  }
}
</script>

<style scoped lang="less">
@import "../../less/main";
.measure-tests {
  padding: 20px;
  padding-left: 30px;
}
.params {
  display: flex;
  align-items: flex-start;
}
.params-item {
  width: 400px;
}
.result {
  display: flex;
  align-items: flex-start;
  padding-bottom: 10px;
}
.result-item {
  width: 300px;
  canvas {
    background-color: green;
  }
}
</style>
