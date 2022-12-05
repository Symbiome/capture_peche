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
              <select placeholder="lake" v-model="selectedLakeUUID">
                <option v-for="lake in lakes" :value="lake.id" :key="lake.uuid">
                  {{ lake.name }}
                </option>
              </select>
              <div v-for="year in years" :key="'gal-' + year" class="">
                <h1>{{ year }}</h1>
                <div
                  v-for="ppT in allPicsPerYear[year]"
                  :key="'trippic-' + ppT.tripId"
                >
                  {{ ppT.tripLakeName }} {{ ppT.tripDate }} <br />
                  <div
                    class="dashboard-top-catchs catch-preview-list-scrollable"
                  >
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
            </div>
            <div class="right-part">
              <img
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
  }

  async reload() {
    await this.loadLakes();
    await this.selectedDefaultPicChanged();
    this.selectedLakeUUID = this.selectedLakeUUIDProp;
    this.loadFullGaleryAndSelectCorrectPic();
  }

  @Watch("selectedDefaultPic")
  selectedDefaultPicChanged(): void {
    this.selectedPic = this.selectedDefaultPic;
  }

  @Watch("picturesPerTrip")
  @Watch("selectedLakeUUID")
  async loadFullGaleryAndSelectCorrectPic(): Promise<void> {
    this.allPicsPerYear = await PicturesService.getAllPicsPerYearAndLake(
      this.selectedLakeUUID
    );
    this.years = Object.keys(this.allPicsPerYear).reverse();
    if (!this.selectedPic) {
      this.selectedPic = this.selectedDefaultPic;
    }
    if (
      !this.selectedPic &&
      this.years.length &&
      this.allPicsPerYear &&
      this.allPicsPerYear.values &&
      this.allPicsPerYear.values()
    ) {
      const ppT = [...this.allPicsPerYear.values()][0];
      this.selectedPic = this.getPicURL(ppT.pictureURLs[0]);
    }
    this.$nextTick(() => {
      var selectedPicImg = document.getElementsByClassName("selected");
      var container = document.getElementById("scroller");
      if (selectedPicImg.length && container) {
        var topPos = (selectedPicImg[0] as HTMLElement).offsetTop;
        container.scrollTop = Math.max(0, topPos - 400);
      }
    });
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
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../../less/main";

.galery-preview-list {
  height: 100%;
  display: flex;

  padding-left: calc(@margin-large + 5px);
  padding-right: calc(@margin-large + 5px);

  @media screen and (min-width: @desktop-min-width) {
    padding-left: calc(@margin-large-desktop + 5px);
    padding-right: calc(@margin-large-desktop + 5px);
  }
}

.galery-pic {
  object-fit: cover;
  width: 230px;
  height: 230px;
  padding: 10px;

  &.selected {
    border: 10px solid red;
  }
}

.two-sides {
  display: flex;

  .left-part {
    width: 35vw;
  }
  .right-part {
    height: 60vh;
    width: 65vw;
    .main-pic {
      width: 60vw;
    }
  }
}
</style>
