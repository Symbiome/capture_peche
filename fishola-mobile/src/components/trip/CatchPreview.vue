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
  <div class="catch-preview">
    <div class="preview-top">
      <div class="meta">
        <div class="meta-row" v-if="metaMode == 'size' && aCatch.size">
          <i class="icon-size" /> {{ aCatch.size }} cm<br />
        </div>
        <div class="meta-row" v-if="metaMode == 'weight'">
          <i class="icon-weight" /> {{ aCatch.weight }} g<br />
        </div>
        <div class="meta-row" v-if="caughtAtLabel">
          <i class="icon-clock" /> {{ caughtAtLabel }}<br />
        </div>
        <div class="meta-row">
          {{ techniqueLabel }}
          <span v-if="aCatch.keep"> - conservé</span>
          <span v-if="!aCatch.keep"> - relâché</span>
        </div>
      </div>

      <div class="preview-picture">
        <PicturePreview
          v-bind:src="pictureSrc"
          v-bind:enableModal="false"
          v-bind:deletable="false"
        />
      </div>
    </div>
    <div class="preview-bottom">
      <div class="bottom-left" v-if="bottom == 'species'">
        <i class="icon-fish" />
        {{ speciesLabel }}
      </div>
      <div class="bottom-left" v-if="bottom != 'species'">
        <Top v-bind:n="top" />
      </div>
      <div class="bottom-right">
        Voir
        <button v-on:click="$emit('openCatch')">
          <i class="icon-arrow" />
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import CatchSummary from "@/pojos/CatchSummary";
import { SpeciesWithAlias, Technique, TripBean } from "@/pojos/BackendPojos";

import PicturePreview from "@/components/trip/PicturePreview.vue";
import Top from "@/components/common/Top.vue";

import PicturesService from "@/services/PicturesService";
import { SpeciesWithAliasAndTechnique } from "@/services/ReferentialService";
import ReferentialService from "@/services/ReferentialService";
import Constants from "@/services/Constants";

import { Component, Prop, Vue } from "vue-property-decorator";

@Component({
  components: {
    PicturePreview,
    Top,
  },
})
export default class CatchPreview extends Vue {
  @Prop() lakeId: string;
  @Prop() aCatch: CatchSummary;
  @Prop({ default: true }) modifiable: boolean;

  @Prop({ default: "size" }) metaMode: string;
  @Prop({ default: "species" }) bottom: string;

  caughtAtLabel: string = "";
  techniqueLabel: string = "";
  speciesLabel: string = "";
  top: number = 0;

  pictureSrc: string = "";

  created() {
    if (this.aCatch.caughtAt) {
      this.caughtAtLabel = this.aCatch.caughtAt;
    }

    ReferentialService.getSpeciesAndTechniques(this.lakeId).then(
      this.referentialLoaded
    );

    if (this.bottom != "species") {
      this.top = parseInt(this.bottom.substring(4));
    }

    this.getCatchPreviewPic();
  }

  async getCatchPreviewPic() {
    try {
      // Get pictures stored locally (not yet synchronized) and get latest as preview
      const localPics = await PicturesService.getPicturesFromLocalDB(
        this.aCatch.id
      );
      if (localPics.length) {
        this.pictureSrc = localPics[0].content;
      } else if (this.aCatch.hasPicture) {
        // Otherwise, get preview from server gallery picture (if any)
        this.pictureSrc = Constants.apiUrl(
          `/v1/pictures/${this.aCatch.id}/preview`
        );
      } else if (this.aCatch.hasMeasurementPicture) {
        // Otherwise, get preview from server measurement picture (if any)
        this.pictureSrc = Constants.apiUrl(
          `/v1/pictures/measure/${this.aCatch.id}/preview`
        );
      }
    } catch (e) {
      // Silent catch, no pictures will be displayed
    }
  }

  referentialLoaded(data: SpeciesWithAliasAndTechnique) {
    // Au cas où l'espèce ne soit pas encore sur le back, on prend la 'other' par défaut
    this.speciesLabel = this.aCatch.otherSpecies || "";
    data.species.forEach((s) => {
      if (this.aCatch.speciesId == s.id) {
        this.speciesLabel = s.alias ? s.alias : s.name;
      }
    });
    data.techniques.forEach((t) => {
      if (this.aCatch.techniqueId == t.id) {
        this.techniqueLabel = t.name;
      }
    });
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../../less/main";

.catch-preview {
  width: calc(100vw - 80px);
  height: 100%;
  padding: @vertical-margin-xx-small;

  display: flex;
  flex-direction: column;
  justify-content: center;

  .preview-top {
    flex: 1;
    background-color: @gainsboro;

    border-top-left-radius: 8px;
    border-top-right-radius: 8px;

    position: relative;

    cursor: pointer;

    .meta {
      position: absolute;
      z-index: 20;
      width: fit-content;
      height: calc(108px + env(safe-area-inset-top));
      background: @cyprus;
      opacity: 0.8;
      border-radius: 8px;

      margin-top: @vertical-margin-medium;
      margin-left: @margin-medium;

      padding: @vertical-margin-medium;

      @media (max-width: 350px) {
        margin-top: @vertical-margin-small;
        margin-left: @margin-small;
        padding: @vertical-margin-small;
      }

      font-size: @fontsize-small-paragraph;
      line-height: calc(
        @fontsize-small-paragraph + @line-height-padding-medium
      );
      color: @white;
      text-align: left;

      .meta-row {
        padding-bottom: @vertical-margin-small;

        i {
          margin-right: @margin-small;
        }
      }
    }

    .preview-picture {
      position: absolute;
      top: 0px;
      left: 0px;
      height: 100%;
      width: 100%;
      z-index: 15;

      img.picture {
        border-top-left-radius: 8px;
        border-top-right-radius: 8px;
      }
    }
  }

  .preview-bottom {
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;

    height: 50px;
    background-color: @white;
    border-bottom-left-radius: 8px;
    border-bottom-right-radius: 8px;

    cursor: pointer;

    .bottom-left {
      font-size: @fontsize-span-big;
      margin-left: @margin-medium;
      i {
        color: @pelorous;
        margin-right: @margin-small;
      }
    }

    .bottom-right {
      font-size: @fontsize-small-paragraph;
      margin-right: @margin-medium;

      button {
        width: 32px;
        height: 20px;
        background-color: @summer-sky;
        color: @white;
        border: 0px;
        border-radius: 50px;
        margin-left: @margin-small;
      }
    }
  }

  @media screen and (min-width: @desktop-min-width) {
    width: 295px;
  }
}
</style>
