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
    <PictureModal
      class="picture-modal-gallery-full"
      :forceMobileMode="true"
      v-if="selectedPic && modalOpened && !loading"
      :deleteButton="false"
      @closeModal="modalOpened = false"
      @seeTrip="seeTrip(tripId)"
      :src="selectedPic"
      :otherPics="tripPics"
      :readOnly="true"
    >
      <p style="text-align: center">
        {{ tripTitle }} <br />
        {{ tripDate }} | {{ tripLake }}
      </p>
    </PictureModal>

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
                        @click="
                          selectedPic = getPicURL(picURL);
                          picturePerTripChanged(ppT);
                          modalOpened = true;
                        "
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
              <img
                v-else
                src="/img/camera.svg"
                class="main-pic no-pic"
                :enableModal="false"
                :deletable="false"
              />
              <div class="main-pic-bottom">
                <div class="main-pic-bottom-delete-button" v-if="selectedPic">
                  <button @click="seeTrip(tripId)">Voir la sortie</button>
                </div>
                <div class="main-pic-bottom-bar" v-if="selectedPic">
                  {{ tripTitle }} <br />
                  {{ tripDate }} | {{ tripLake }}
                </div>
                <div class="main-pic-bottom-bar" v-else>
                  Aucune photo sélectionnée
                </div>
              </div>
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
import PictureContentWithOrder from "@/pojos/PictureContentWithOrder";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import PicturesService from "@/services/PicturesService";
import ReferentialService from "@/services/ReferentialService";
import Constants from "../../services/Constants";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import Helpers from "../../services/Helpers";
import router from "../../router";
import PictureModal from "../trip/PictureModal.vue";

@Component({
  components: {
    GaleryPreview,
    FisholaHeader,
    PictureModal,
    FisholaFooter,
  },
})
export default class GaleryFull extends Vue {
  @Prop() selectedDefaultPic: string;
  @Prop({ default: "" }) selectedLakeUUIDProp: string;
  allPicsPerYear: Map<number, PicturePerTripBean> = new Map();
  years: string[] = [];
  lakes: Lake[] = [];
  tripPics = [];
  selectedLakeUUID = "";
  tripDate = "";
  tripLake = "";
  tripTitle = "";
  tripId = "";
  modalOpened = true;
  selectedPic = "";
  loading = true;

  mounted() {
    this.reload(true);
  }

  async reload(useSelectedDefaultPic: boolean) {
    this.loading = true;
    await this.loadLakes();
    this.selectedLakeUUID = this.selectedLakeUUIDProp;
    await this.loadFullGaleryAndSelectCorrectPic();
    if (useSelectedDefaultPic && this.selectedDefaultPic) {
      this.selectedPic = this.selectedDefaultPic;
    }
    this.$nextTick(() => {
      this.loading = false;
    });
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
      this.years.forEach((year) => {
        // @ts-ignore
        this.allPicsPerYear[year].forEach((ppT: PicturePerTripBean) => {
          ppT.pictureURLs.forEach((picURL: string) => {
            if (this.getPicURL(picURL) == this.selectedPic) {
              this.picturePerTripChanged(ppT);
            }
          });
        });
      });
      this.modalOpened = true;
    }
  }

  picturePerTripChanged(ppT: PicturePerTripBean) {
    this.tripDate = this.formatTripDate(ppT.tripDate);
    this.tripLake = ppT.tripLakeName;
    this.tripId = ppT.tripId;
    this.tripTitle = ppT.tripName;
    this.tripPics = ppT.pictureURLs.map((pictureURL) => {
      let pic: PictureContentWithOrder = {};
      pic.content = this.getPicURL(pictureURL);
      return pic;
    });
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
      // @ts-ignore
      this.allPicsPerYear[this.years[0]]
    ) {
      // @ts-ignore
      const ppT = [...this.allPicsPerYear[this.years[0]]][0];
      this.selectedPic = this.getPicURL(ppT.pictureURLs[0]);
      this.picturePerTripChanged(ppT);
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

  async seeTrip(tripId: string) {
    router.push("/trips/" + tripId);
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../../less/main";

.picture-modal-gallery-full {
  display: none;
  @media screen and (max-width: 1300px) {
    display: block;
  }
}
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

    @media screen and (max-width: 1300px) {
      width: 180px;
      height: 130px;
    }

    &.selected {
      border: 5px solid @orange-odd;
    }
  }
}

.two-sides {
  display: flex;

  .left-part {
    min-width: 450px;
    max-width: 100vw;
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
    position: absolute;
    right: 0px;
    width: 55vw;
    height: 100vh;
    .main-pic {
      position: fixed;
      right: 0px;
      object-fit: contain;
      width: 55vw;
      height: 95vh;
      background-color: @galery-pick-background;

      &.no-pic {
        object-fit: none;
      }
    }

    .main-pic-bottom {
      position: absolute;
      right: 0px;
      width: 55vw;
      bottom: 0px;

      display: flex;
      flex-direction: column;
      align-items: center;

      .main-pic-bottom-delete-button {
        position: absolute;
        top: -20px;
        z-index: 10;

        height: 41px;
        width: fit-content;

        display: flex;
        justify-content: center;

        z-index: 10;

        div {
          height: 41px;
        }

        button {
          height: 100%;
          width: 100%;
          border-radius: 22px;

          font-style: normal;
          font-weight: bold;
          font-size: @fontsize-button;
          line-height: calc(@fontsize-button + @line-height-padding-x-large);

          color: @white;
          background-color: @terra-cotta;

          border: 0px;
          padding-left: @margin-medium;
          padding-right: @margin-medium;
        }
      }

      .main-pic-bottom-bar {
        height: 76px;
        padding: 20px;

        @media (max-height: 650px) {
          height: 56px;
        }

        background-color: @cyprus;
        font-size: @fontsize-button;
        line-height: calc(@fontsize-button + @line-height-padding-x-large);
        color: @white;
        width: 100%;

        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: space-between;

        .right,
        .left {
          height: fit-content;
          text-align: center;
          margin-left: @margin-large;
          margin-right: @margin-large;

          @media (max-width: 400px) {
            margin-left: @margin-medium;
            margin-right: @margin-medium;
          }
        }
      }
    }

    @media screen and (max-width: 1300px) {
      display: none;
    }

    @media screen and (max-width: 1500px) {
      width: 40vw;
      .main-pic {
        width: 40vw;
      }

      .main-pic-bottom {
        width: 40vw;
      }
    }

    @media screen and (min-width: 1500px) and (max-width: 1600px) {
      width: 45vw;
      .main-pic {
        width: 45vw;
      }

      .main-pic-bottom {
        width: 45vw;
      }
    }

    @media screen and (min-width: 1600px) and (max-width: 1750px) {
      width: 50vw;
      .main-pic {
        width: 50vw;
      }

      .main-pic-bottom {
        width: 50vw;
      }
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
