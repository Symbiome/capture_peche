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
            </h4>
            <div id="status">
              <span v-if="!openCVLoaded">OpenCV.js is loading...</span>
              <span v-else>OpenCV.js is ready</span>
            </div>

            <div class="params">
              <div v-if="!detectMarker" class="params-item">
                Lefmost object size(mm)
                <input
                  type="text"
                  style="width:100px"
                  id="leftSizeObjectSizeMm"
                  :value="leftSizeObjectSizeMm"
                />
                Resize image size
                <input
                  type="text"
                  style="width:100px"
                  id="fixedSize"
                  :value="fixedSize"
                /><br />
                Min % of detected forms (between 0 and 1)
                <input
                  type="text"
                  style="width:100px"
                  id="minCoverrage"
                  :value="minCoverrage"
                />
              </div>
              <div v-else class="params-item">
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
              <div class="inputoutput">
                <img
                  style="display:none"
                  id="imageSrc"
                  alt="No Image"
                  @load="onNewImageSourceLoad"
                  :src="imageSourceSRC"
                />
                <div class="caption">
                  Photo de prise
                  <input
                    type="file"
                    id="fileInput"
                    name="file"
                    @change="changeSourceImage"
                  />
                </div>
              </div>
            </div>
            <br />

            <div id="calculating">
              <span v-if="calculating">Calcul en cours...</span>
              <span v-if="!calculating">Aucun calcul en cours</span>
            </div>

            <div class="result">
              <div
                class="result-item"
                v-if="!detectMarker"
              >
                <canvas id="canvasOutput1"></canvas><br />
                <caption>
                  Photo originale
                </caption>
              </div>
              <div
                class="result-item"
                v-if="!detectMarker"
              >
                <canvas id="canvasOutput2"></canvas><br />
                <caption>
                  Photo traitée
                </caption>
              </div>
              <div  class="result-item">
                <canvas id="canvasOutput3"></canvas><br />
                <caption>
                  Calcul
                </caption>
              </div>
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

@Component({
  components: {
    FisholaHeader,
    FisholaFooter,
  },
})
export default class OpenCVSizeComputation extends Vue {
  @Prop({ default: "marker" }) mode;
  imageSourceSRC = "";
  markerSourceSRC = "/tests/unit/assets/markers/marker.jpg";
  minCoverrage = 0.15;
  leftSizeObjectSizeMm = 133;
  fixedSize = 150;
  detectMarker = true;

  openCVLoaded = false;
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
    const imageElement = e.target as HTMLElement;
    this.calculating = true;
    if (this.detectMarker) {
      const markerElement = document.getElementById("marker");
      console.error("Detect marker ", markerElement);
      if (markerElement) {
        await FisholaOpenCVService.INSTANCE.detectMarker(
          imageElement,
          markerElement
        );
        this.calculating = false;
      }
    } else {
      console.error("Calculate size", e);
      await FisholaOpenCVService.INSTANCE.calculateSizes(
        imageElement,
        this.minCoverrage,
        this.leftSizeObjectSizeMm,
        this.fixedSize
      );
      this.calculating = false;
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
}
.result-item {
  width: 300px;
  height: 300px;
  canvas {
    background-color: green;
  }
}
</style>
