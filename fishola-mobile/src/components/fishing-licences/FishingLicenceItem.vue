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
      <input type="checkbox" :id="'checkbox-' + licence.id" class="pelorous-checkbox" v-model="selected" />
      <label v-bind:for="'checkbox-' + licence.id"></label>
    </div>

    <div id="preview" class="hide-on-mobile" @click="openLicence()">
      <img v-if="licence.type === 'JPEG'" class="preview-img" :src="licenceUrl" alt="Aperçu de la carte" />
      <div v-else src="/img/dashboard.png" class="preview-img pdf-img">
        <i class="icon-fishing" />
      </div>
    </div>

    <div class="item-description" @click="openLicence()">
      <div class="item-row">
        <div class="left-part name">{{ licence.name }}</div>
        <div class="right-part">Expire le {{ formattedDate() }}</div>
        <i class="icon-edit edit hide-on-mobile" @click="editLicence()" />
      </div>
    </div>

    <div class="item-edit hide-on-desktop" @click="editLicence()">
      <i class="icon-edit edit" />
    </div>
  </div>
</template>

<script lang="ts">
import FishingLicenceService from "@/services/FishingLicenceService";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import { LicenceResponseBean } from "@/pojos/BackendPojos";
import Helpers from '@/services/Helpers';

@Component({
  components: {
    FishingLicenceItem
  },
})
export default class FishingLicenceItem extends Vue {
  @Prop() licence!: LicenceResponseBean;

  selected: boolean = false;
  date: string = "";
  licenceUrl: string = "";

  async mounted() {

  }

  formattedDate(): string {
    var dayOptions: Intl.DateTimeFormatOptions = {
      month: "numeric",
      day: "numeric",
      year: "numeric",
    };
    // @ts-ignore
    const date = Helpers.parseLocalDate(this.licence.expirationDate);
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
    // @ts-ignore
    this.$router.push({
      name: 'licence-fullscreen',
      params: {
        'id': this.licence.id,
        'type': this.licence.type
      }
    });
  }

  editLicence() {
    // @ts-ignore
    this.$router.push({
      name: 'licence-edit',
      params: {
        'id': this.licence.id
      }
    });
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
    width: 140px;
    justify-content: center;
  }

  .preview-img {
    width: 80px;
    height: 80px;
    border-radius: 5px;
    object-fit: cover;
    border: 2px solid @pelorous;
  }

  .item-selection {
    width: 16px;
    height: 16px;

    input {
      margin: 0px;
    }
  }

  .edit {
    color: @pelorous;
  }

  .pdf-img {
    font-size: @pastille-size;
    line-height: calc(@pastille-size);
    color: @pelorous;
    background: @white;
    margin: auto;
    text-align: center;
    padding-top: 20px;
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
