<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2022 INRAE - UMR CARRTEL
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
    <div class="page galery-page">
      <div class="pane pane-only">
        <div class="pane-content rounded" id="scroller">
          <div class="two-sides">
            <div class="left-part" id="galery-div">
              <select
                v-if="selectedLakeUUID"
                class="lake-gallery-select"
                placeholder="lake"
                v-model="selectedLakeUUID"
              >
                <option v-for="lake in lakes" :value="lake.id" :key="lake.uuid">
                  {{ lake.name }}
                </option>
              </select>
              <div class="years">
                <div v-for="year in years" :key="'gal-' + year" class="">
                  <h1>{{ year }}</h1>
                  <div
                    class="trip-holder"
                    v-for="ppT in allPicsPerYear[year]"
                    :key="'trippic-' + ppT.tripId"
                  >
                    <span class="trip-date"
                      >{{ formatTripDate(ppT.tripDate) }}
                    </span>
                    <div class="galery-pics-container">
                      <img
                        class="galery-pic"
                        :class="{ selected: getPicURL(picURL) == selectedPic }"
                        v-for="picURL in ppT.pictureURLs"
                        :key="picURL"
                        @click="selectedPic = getPicURL(picURL)"
                        :src="getPicURL(picURL)"
                        :enableModal="false"
                        :deletable="false"
                      />
                    </div>
                  </div>
                </div>
                <div
                  v-if="!years || years.length == 0"
                  class="no-pic-in-galery"
                >
                  <img
                    src="/img/camera.svg"
                    class="galery-pic selected"
                    :enableModal="false"
                    :deletable="false"
                  />
                  Aucune photo
                </div>
              </div>
            </div>
            <div class="right-part">
              <img
                v-if="selectedPic"
                class="main-pic"
                :src="selectedPic"
                :enableModal="false"
                :deletable="false"
              />
            </div>
          </div>
        </div>
        <div class="bottom-page-spacer"></div>
      </div>
    </div>
    <FisholaFooter shortcuts="back,settings,profile" selected="settings" />
  </div>
</template>

<script lang="ts">
import GaleryPreview from "@/components/galery/GaleryPreview.vue";
import { PicturePerTripBean, Lake } from "@/pojos/BackendPojos";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import PicturesService from "@/services/PicturesService";
import ReferentialService from "@/services/ReferentialService";
import Constants from "../../services/Constants";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import Helpers from "../../services/Helpers";

@Component({
  components: {
    GaleryPreview,
    FisholaHeader,
    FisholaFooter,
  },
})
export default class GaleryFull extends Vue {
  @Prop() selectedDefaultPic: string;
  @Prop({ default: "" }) selectedLakeUUIDProp: string;
  allPicsPerYear: Map<number, PicturePerTripBean> = new Map();
  years: string[] = [];
  lakes: Lake[] = [];
  selectedLakeUUID = "";

  selectedPic = "";

  mounted() {
    this.reload();
    this.selectedPic = this.selectedDefaultPic;
  }

  async reload() {
    await this.loadLakes();
    this.selectedLakeUUID = this.selectedLakeUUIDProp;
    this.loadFullGaleryAndSelectCorrectPic();
  }

  @Watch("selectedLakeUUID")
  selectedLakeChanged(): void {
    this.selectedPic = "";
    this.loadFullGaleryAndSelectCorrectPic();
  }

  @Watch("selectedPic")
  selectedPicChanged() {
    if (this.selectedPic) {
      this.$nextTick(() => {
        var selectedPicImg = document.getElementsByClassName("selected");
        var container = document.getElementById("scroller");
        if (selectedPicImg.length && container) {
          var topPos = (selectedPicImg[0] as HTMLElement).offsetTop;
          container.scrollTop = Math.max(0, topPos - 400);
        }
      });
    }
  }

  @Watch("picturesPerTrip")
  async loadFullGaleryAndSelectCorrectPic(): Promise<void> {
    this.allPicsPerYear = await PicturesService.getAllPicsPerYearAndLake(
      this.selectedLakeUUID
    );
    this.years = Object.keys(this.allPicsPerYear).reverse();
    if (
      !this.selectedPic &&
      this.years.length &&
      this.allPicsPerYear &&
      this.allPicsPerYear[this.years[0]]
    ) {
      const ppT = [...this.allPicsPerYear[this.years[0]]][0];
      this.selectedPic = this.getPicURL(ppT.pictureURLs[0]);
    }
  }

  getPicURL(picURL: string): string {
    return Constants.apiUrl(picURL);
  }

  async loadLakes(): Promise<void> {
    this.lakes = [];
    const defaultLake = {
      id: "",
      name: "Tous les lacs",
      exportAs: "",
      latitude: 0,
      longitude: 0,
    };
    this.lakes.push(defaultLake);
    const allLakes = await ReferentialService.getLakes();
    this.lakes = this.lakes.concat(allLakes);
  }

  formatTripDate(input: any): string {
    return Helpers.formatToDateWithoutYear(Helpers.parseLocalDate(input));
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../../less/main";

.galery-page {
  .pane {
    background-color: @galery-background !important;
  }
}

.galery-pics-container {
  width: 100%;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  flex-direction: row;
  .galery-pic {
    object-fit: cover;
    width: 134px;
    height: 93px;

    &.selected {
      border: 5px solid @orange-odd;
    }
  }
}

.two-sides {
  display: flex;

  .left-part {
    .lake-gallery-select {
      background-color: white;
      margin-top: 10px;
      margin-bottom: 10px;
      padding: 10px;
      margin-left: 0px;
      height: 40px;
      border: 1px solid @pale-sky;
      border-radius: 3px;
    }
    h1 {
      color: white;
      font-size: 20px;
      padding: 0px;
      margin-bottom: 5px;
      margin-top: 5px;
      margin-left: 0px;
      margin-right: 0px;
    }
    width: 40vw;
    min-width: 450px;
    max-width: 100vw;

    .trip-holder {
      font-size: 16px;
      color: white;
      padding-bottom: 20px;
      .trip-date {
        display: block;
        padding-bottom: 5px;
      }
    }
    @media screen and (max-width: 1300px) {
      width: 100%;
    }
  }
  .right-part {
    width: 100%;
    padding-top: 20px;
    .main-pic {
      position: fixed;
      object-fit: contain;
      width: 50vw;
      height: 95vh;
    }

    @media screen and (max-width: 1300px) {
      display: none;
    }
  }

  .no-pic-in-galery {
    height: 100%;
    width: 134px;
    height: 93px;
    margin-top: 40px;
    background-color: #e7e7e7;
    border-top-left-radius: 8px;
    border-top-right-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background-color: @link-water;

    cursor: pointer;

    span {
      margin-top: @vertical-margin-small;
      color: @pale-sky;
      font-weight: 300;
      font-size: @fontsize-small-paragraph;
      line-height: calc(
        @fontsize-small-paragraph + @line-height-padding-medium
      );
    }
  }
}
</style>
