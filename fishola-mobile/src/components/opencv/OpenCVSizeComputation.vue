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
    <FisholaHeader />
    <div class="page">
      <div class="pane pane-only">
        <div class="pane-content large rounded">
          <div class="measure-tests">
            <h4 class="title">
              OpenCV -
              <span v-if="detectMarker">Détection de marqueur</span>
              <span v-else>Mesure automatique de poisson</span>
              <button @click="detectMarker = !detectMarker">
                <span v-if="detectMarker">Mesures</span>
                <span v-else>Marqueur</span>
              </button>

              <input
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
              <span v-html="resultText"/>
            </div>

            <div class="result" v-show="calculated && !calculating">
              <div class="result-item" v-if="!detectMarker">
                <canvas id="canvasOutput1"></canvas><br />
                <caption>
                  Original
                </caption>
              </div>
              <div class="result-item" v-if="!detectMarker">
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

            <div class="params">
              <div class="params-item">
                <img
                  style="display:none"
                  id="sourcePicture"
                  alt="No Image"
                  @load="onNewImageSourceLoad"
                  :src="imageSourceSRC"
                />
                Taille marqueur (mm)
                <input
                  type="number"
                  style="width:100px"
                  id="markerSizeInCm"
                  @change="launchSizeComputation"
                  v-model.number="markerSizeInCm"
                />
                <br />
                Redimensionnement (px)
                <input
                  type="number"
                  style="width:100px"
                  id="resizeSize"
                  @change="launchSizeComputation"
                  v-model.number="resizeSize"
                /><br />
              </div>
              <div v-if="!detectMarker" class="params-item">
                % d'occupation minimal (0 à 1)
                <input
                  type="number"
                  style="width:100px"
                  id="minSizeRatio"
                  step="0.1"
                  @change="launchSizeComputation"
                  v-model.number="minSizeRatio"
                />
                <br />
                Ratio hauteur/largeur min (0 à 1)
                <input
                  type="number"
                  style="width:100px"
                  id="minWithLengthRatio"
                  step="0.1"
                  @change="launchSizeComputation"
                  v-model.number="minWithLengthRatio"
                />
                <br />
                Ratio hauteur/largeur min (0 à 1)
                <input
                  type="number"
                  style="width:100px"
                  id="maxWithLengthRatio"
                  step="0.1"
                  @change="launchSizeComputation"
                  v-model.number="maxWithLengthRatio"
                />
              </div>
              <div v-else class="params-item">
                Resize image size
                <input
                  type="number"
                  style="width:100px"
                  id="resizeSize"
                  @change="launchSizeComputation"
                  v-model.number="resizeSize"
                />
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
                  id="marker"
                  alt=""
                  @load="onNewMarkerSourceLoad"
                  :src="markerSourceSRC"
                />
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
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import FisholaOpenCVService from "@/services/opencv/FisholaOpenCVService";
import { DetectedShape } from  "@/services/opencv/DetectedShape";

@Component({
  components: {
    FisholaHeader,
    FisholaFooter,
  },
})
export default class OpenCVSizeComputation extends Vue {
  @Prop({ default: "measure" }) mode: string;
  imageSourceSRC = "";
  markerSourceSRC = "/tests/unit/assets/markers/marker.jpg";
  minSizeRatio = 0.15;
  minWithLengthRatio = 0.1;
  maxWithLengthRatio = 0.8;
  markerSizeInCm = 133;
  resizeSize = 250;
  detectMarker = true;
  resultText = ""

  openCVLoaded = false;
  calculated = false;
  calculating = false;

  mounted(): void {
    this.detectMarker = this.mode == "marker";
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

  async onNewImageSourceLoad(e: Event): Promise<void> {
    this.launchSizeComputation();
  }

  async launchSizeComputation(): Promise<void> {
    const imageElement = document.getElementById("sourcePicture");
    this.calculating = true;
    if (this.detectMarker) {
      const markerElement = document.getElementById("marker");
      if (markerElement) {
        await FisholaOpenCVService.INSTANCE.detectMarker(
          imageElement,
          markerElement,
          this.resizeSize
        );
        this.calculated = true;
        this.calculating = false;
      }
    } else {
      const detectedShapes: Array<DetectedShape> = await FisholaOpenCVService.INSTANCE.calculateAndDrawFishSizes(
        imageElement,
        this.minSizeRatio,
        this.minWithLengthRatio,
        this.maxWithLengthRatio,
        this.resizeSize,
        this.markerSizeInCm,
        true
      );
      this.calculated = true;
      this.calculating = false;

      const markers = detectedShapes.filter((shape: DetectedShape) => shape.isMarker).length
      const ignored = detectedShapes.filter((shape: DetectedShape) => !shape.isMarker && !shape.isFish).length
      const notIgnored = detectedShapes.filter((shape: DetectedShape) => shape.isFish || shape.isMarker)
      let result = ": " + detectedShapes.length + " formes (" + markers + " marqueurs, " + (notIgnored.length - markers) + " poissons, " + ignored + " ignorées)"
      notIgnored.forEach((shape: DetectedShape) => {
        result += "<br/>- "
        if (shape.isFish) {
          result +=" Poisson "
        } else {
          result += " Marqueur "
        }
        result += shape.calculatedLenght + "mm (" + Math.round(shape.width) + "px * " + Math.round(shape.height) + " px) - left " + Math.round(shape.leftX) + " top " + Math.round(shape.topY)
      })
      this.resultText = result
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
