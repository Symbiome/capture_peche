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
          <h1 class="no-margin-pane">
            OpenCV -
            <span v-if="detectMarker">Détection de marqueur</span>
            <span v-else>Mesure automatique de poisson</span>
            <button @click="detectMarker = !detectMarker">Switch</button>
          </h1>
          <div class="measure-tests">
            <div v-if="!detectMarker">
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
            <div v-else>
              <div class="caption">
                target
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
            <br />
            <div class="inputoutput">
              <img
                style="display:none"
                id="imageSrc"
                alt="No Image"
                @load="onNewImageSourceLoad"
                :src="imageSourceSRC"
              />
              <div class="caption">
                imageSrc
                <input
                  type="file"
                  id="fileInput"
                  name="file"
                  @change="changeSourceImage"
                />
              </div>
            </div>
            <p id="status" v-if="!openCVLoaded">OpenCV.js is loading...</p>
            <p id="status" v-else>OpenCV.js is ready</p>
            <div class="inputoutput">
              <div style="float:left;background-color:orange;">
                <canvas id="canvasOutput1"></canvas><br />
                <caption>
                  Photo originale
                </caption>
              </div>
              <div style="clear:both" />
              <div style="float:left;background-color:yellow;">
                <canvas id="canvasOutput2"></canvas><br />
                <caption>
                  Photo traitée
                </caption>
              </div>

              <div style="clear:both" />
              <div style="float:left;background-color:green;">
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
import { Component, Vue } from "vue-property-decorator";
import FisholaOpenCVService from "@/services/opencv/fish-analyser";

@Component({
  components: {
    FisholaHeader,
    FisholaFooter,
  },
})
export default class OpenCVSizeComputation extends Vue {
  imageSourceSRC = "";
  markerSourceSRC = "";
  minCoverrage = 0.15;
  leftSizeObjectSizeMm = 133;
  fixedSize = 150;
  detectMarker = true;
  openCVLoaded = false;
  cv: any;

  created(): void {
    this.loadOpenCVIfNeeded();
  }

  /**
   * Loads the opencv.js library if required (i.e. was never loaded).
   */
  async loadOpenCVIfNeeded() {
    // @ts-ignore : if OpenCV is loaded, window.cv is defined
    if (!window.cv) {
      // If not, let's create an async script to load it
      const callbackTarget = this;
      var head = document.getElementsByTagName("head").item(0);
      var script = document.createElement("script");
      script.setAttribute("type", "text/javascript");
      script.setAttribute("src", "/js/opencv.js");
      script.setAttribute("async", "");
      // Add a callback allowing this.openCVReady() to be called when OpenCV Is ready
      script.setAttribute(
        "onLoad",
        "document.dispatchEvent(new Event('open-cv-loaded'))"
      );
      document.addEventListener(
        "open-cv-loaded",
        function(e) {
          callbackTarget.openCVReady();
        },
        false
      );
      // @ts-ignore
      head.appendChild(script);
    } else {
      this.openCVReady();
    }
  }

  /**
   * Callback called when OpenCV has done loading and is ready
   */
  openCVReady(): void {
    // @ts-ignore Get OpenCV instance from window.cv
    this.cv = window.cv;
    this.openCVLoaded = true;
  }

  changeSourceImage(e: Event): void {
    console.error("changedSourceImage", e);
    let eventTaget = e.target;
    // @ts-ignore
    if (eventTaget != null && eventTaget["files"]) {
      // @ts-ignore
      this.imageSourceSRC = URL.createObjectURL(eventTaget["files"][0]);
    }
  }

  changeMarkerImage(e: Event): void {
    console.error("changeMarkerImage", e);
    let eventTaget = e.target;
    // @ts-ignore
    if (eventTaget != null && eventTaget["files"]) {
      // @ts-ignore
      this.markerSourceSRC = URL.createObjectURL(eventTaget["files"][0]);
    }
  }

  onNewMarkerSourceLoad(e: Event): void {
    console.error("onNewMarkerSourceLoad", e);
    let eventTaget = e.target;
    // @ts-ignore
    if (eventTaget != null && eventTaget["files"]) {
      // @ts-ignore
      this.markerSourceSRC = URL.createObjectURL(eventTaget["files"][0]);
    }
  }

  onNewImageSourceLoad(e: Event): void {
    const imageElement = e.target as HTMLElement;
    if (this.detectMarker) {
      const markerElement = document.getElementById("marker");
      console.error("Detect marker ", markerElement);
      if (markerElement) {
        FisholaOpenCVService.INSTANCE.detectMarker(
          this.cv,
          imageElement,
          markerElement
        );
      }
    } else {
      console.error("Calculate size", e);
      FisholaOpenCVService.INSTANCE.calculateSizes(
        this.cv,
        imageElement,
        this.minCoverrage,
        this.leftSizeObjectSizeMm,
        this.fixedSize
      );
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
</style>
