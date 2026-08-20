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
  <div class="galery-preview-list">
    <GaleryPreview
      :selectedLakeId="selectedLakeId"
      v-for="picURL in firstPictures"
      :key="picURL"
      :pictureSrc="getPicURL(picURL)"
      :enableModal="false"
      :deletable="false"
    />
    <GaleryPreview
      v-if="firstPictures.length == 0"
      :enableModal="false"
      :deletable="false"
    />
  </div>
</template>

<script lang="ts">
import CatchSummary from "@/pojos/CatchSummary";

import GaleryPreview from "@/components/galery/GaleryPreview.vue";
import { PicturePerTripBean } from "@/pojos/BackendPojos";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import Constants from "../../services/Constants";

@Component({
  components: {
    GaleryPreview,
  },
})
export default class GaleryPreviewList extends Vue {
  @Prop() picturesPerTrip: PicturePerTripBean[];
  @Prop() year: number;
  @Prop({ default: "" }) selectedLakeId: string;

  firstPictures: string[] = [];
  
  mounted() {
    if (this.picturesPerTrip && this.picturesPerTrip.length > 0) {
      this.picturesPerTripChanged();
      this.scrollToFirstElement();
    }
  }

  @Watch("picturesPerTrip")
  picturesPerTripChanged() {
    let i = 0;
    this.firstPictures = [];
    while (this.firstPictures.length < 4 && i < this.picturesPerTrip.length) {
      this.firstPictures = this.firstPictures.concat(
        this.picturesPerTrip[i].pictureURLs
      );
      i++;
    }
  }

  scrollToFirstElement() {
    try {
      const squareButton = document.getElementById("new-galery-square-button");
      const scrollableElements = document.getElementsByClassName(
        "galery-preview-list-scrollable"
      );
      if (squareButton && scrollableElements) {
        scrollableElements[0].scrollLeft = squareButton.clientWidth + 2; // +2 pour la bordure
      }
    } catch (someError) {
      console.error(someError);
    }
  }

  newCatch() {
    this.$emit("newCatch");
  }

  openCatch(aCatch: CatchSummary) {
    this.$emit("openCatchFromId", aCatch.id);
  }

  getPicURL(picURL: string): string {
    return Constants.apiUrl(picURL);
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.galery-preview-list {
  height: 100%;
  display: flex;

  padding-left: calc(@margin-large + 5px);
  padding-right: calc(@margin-large + 5px);

  .new-catch {
    height: 100%;

    padding-top: @vertical-margin-xx-small;
    padding-bottom: @vertical-margin-xx-small;

    .new-galery-square-button {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;

      background-image: url("img/illustration_fish_wire.svg");
      background-repeat: no-repeat;
      background-size: auto 75%;
      background-position: center;

      border: 1px dashed @pale-sky;
      border-radius: 8px;
      height: 100%;
      width: calc(100vw - 80px);

      margin-left: @margin-x-small;
      margin-right: @margin-x-small;

      cursor: pointer;

      .pastille {
        width: 70px;
        height: 70px;
        font-size: @pastille-size;
        line-height: calc(@pastille-size);
        color: @white;
        background: @pale-sky;
      }
    }
  }

  @media screen and (min-width: @desktop-min-width) {
    padding-left: calc(@margin-large-desktop + 5px);
    padding-right: calc(@margin-large-desktop + 5px);

    .new-catch {
      .new-galery-square-button {
        width: 295px;

        .pastille {
          width: 35px;
          height: 35px;
          font-size: 15px;
          line-height: calc(15px);
        }
      }
    }
  }
}
</style>
