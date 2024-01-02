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
  <div class="licence-item">
    <div class="item-selection">
      <input
        type="checkbox"
        v-bind:id="'checkbox-' + licence.id"
        class="pelorous-checkbox"
        v-model="selected"
      />
      <label v-bind:for="'checkbox-' + licence.id"></label>
    </div>

    <div id="preview" class="hide-on-mobile" v-on:click="openLicence()">
      <img
        v-if="licence.type === 'JPEG'"
        class="preview-img"
        :src="licenceUrl"
        alt="Miniature"
      />
      <img v-else :src="previewPdf" alt="PDF Preview" />
    </div>

    <div class="item-description" v-on:click="openLicence()">
      <div class="item-row">
        <div class="left-part name">{{ licence.name }}</div>
        <div class="right-part">Expire le {{ formattedDate() }}</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import FishingLicenceService from "@/services/FishingLicenceService";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import { LicenceResponseBean } from "@/pojos/BackendPojos";

@Component({
  components: {
    FishingLicenceItem,
    FishingLicenceService,
  },
})
export default class FishingLicenceItem extends Vue {
  @Prop() licence!: LicenceResponseBean;

  selected: boolean = false;
  date: string = "";
  licenceUrl: string = "";
  previewPdf: string = "";

  async mounted() {
    const fileBlob: Blob = await FishingLicenceService.getLicence(
      this.licence.id
    );
    this.licenceUrl = URL.createObjectURL(fileBlob);
  }

  formattedDate(): string {
    var dayOptions: Intl.DateTimeFormatOptions = {
      month: "numeric",
      day: "numeric",
      year: "numeric",
    };

    const date = new Date(this.licence.expirationDate);
    const dateString = date.toLocaleDateString("fr-FR", dayOptions);
    return dateString;
  }

  @Watch("selected")
  onSelectedChanged(value: boolean, _oldValue: boolean) {
    if (value) {
      this.$emit("selected");
    } else {
      this.$emit("unselected");
    }
  }

  async openLicence(): Promise<void> {
    try {
      if (this.licence.type === "PDF") {
        window.open(this.licenceUrl, "_blank");
      } else {
        const img = new Image();
        img.src = this.licenceUrl;
        const imgWindow = window.open("", "_blank");
        if (imgWindow) {
          imgWindow.document.write(img.outerHTML);
        } else {
          console.error("La fenêtre n'a pas pu être ouverte.");
        }
      }
    } catch (error) {
      console.error("Erreur lors du téléchargement du fichier", error);
    }
  }

  deleteLicence(licence: LicenceResponseBean) {
    FishingLicenceService.deleteLicence(licence.id);
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../../less/main";

.licence-item {
  display: flex;
  align-items: center;

  margin: 0px;
  padding-right: @margin-large;
  padding-top: @vertical-margin-medium;
  padding-bottom: @vertical-margin-medium;

  border-bottom: 1px solid @gainsboro;

  width: 100%;
  height: 110px;

  #preview {
    display: flex;
    padding-left: @margin-medium;
    width: 200px;
    justify-content: center;
  }

  .preview-img {
    max-width: 100%;
    max-height: 110px;
  }

  .item-selection {
    width: 16px;
    height: 16px;

    input {
      margin: 0px;
    }
  }

  .item-description {
    margin-left: @margin-medium;
    width: 100%;

    font-size: @fontsize-small-paragraph;
    line-height: calc(@fontsize-small-paragraph + @line-height-padding-x-large);

    .item-row {
      display: flex;
      justify-content: space-between;
      margin-top: @vertical-margin-xx-small;

      color: @pale-sky;

      .name {
        font-weight: bold;
        font-size: @fontsize-header-paragraph;
        color: @gunmetal;
      }

      .left-part {
        i {
          margin-right: @margin-small;
          color: @pale-sky;
        }
      }

      .right-part {
        i {
          margin-left: @margin-small;
          color: @pelorous;
        }
      }
    }
  }

  @media (max-width: 500px) {
    padding-right: @margin-medium;

    .item-description {
      margin-left: @margin-small;

      .item-row {
        display: flex;
        flex-direction: column;

        .left-part {
          i {
            margin-right: @margin-x-small;
          }
        }

        .right-part {
          i {
            margin-left: @margin-x-small;
          }
        }
      }
    }
  }

  // @media (max-height: 650px) {
  //   height: 90px;

  //   .item-description {
  //     line-height: calc(@fontsize-small-paragraph + @line-height-padding-small);
  //   }
  // }

  @media screen and (min-width: @desktop-min-width) and (max-width: 1200px) {
    .item-row {
      display: flex;
      flex-direction: column;
    }
  }

  @media screen and (min-width: @desktop-min-width) {
    height: 110px;
    padding-right: @margin-large-desktop;
    &:hover {
      background-color: @gainsboro;
    }

    #preview,
    .item-description {
      cursor: pointer;
    }

    .item-description {
      margin-left: @margin-medium;
      width: 100%;

      font-size: @fontsize-paragraph;
      line-height: calc(@fontsize-paragraph + @line-height-padding-x-large);

      .item-row {
        .name {
          font-size: @fontsize-paragraph-desktop;
        }
      }
    }
  }
}
</style>
