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
  <div class="new-licence page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="page new-licence-page keyboardSensitive">
      <div class="pane pane-only">
        <h1 class="no-margin-pane">
          <BackButton class="hide-on-mobile" />
          Modifier ma carte de pêche
        </h1>
        <div class="pane-content rounded">
          <div class="container" v-if="!loading">
            <div class="container-form keyboardSensitive">
              <form @submit.prevent="saveLicence">
                <label>Nom de la carte de pêche </label>
                <input v-model="licenceName" :class="{ 'field-error': nameError }"
                  placeholder="Nommez la carte de pêche" />

                <label>Date d'expiration </label>
                <input v-if="licenceExpirationDate" type="date" v-model="licenceExpirationDate" />
              </form>
            </div>
          </div>
          <div class="save">
            <button class="button hide-on-mobile" type="submit" @click="saveLicence">
              Modifier
            </button>
          </div>
        </div>
      </div>
    </div>

    <FisholaFooter shortcuts="back,home,dashboard" v-bind:button-text="getButtonText()"
      v-on:buttonClicked="saveLicence" />
  </div>
</template>

<script lang="ts">
import { RouterUtils } from "@/router/RouterUtils";

import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";
import BackButton from "@/components/common/BackButton.vue";

import FishingLicenceService from "@/services/FishingLicenceService";

import { Component, Prop, Vue } from "vue-property-decorator";
import { LicenceFromClientBean, LicenceType } from "@/pojos/BackendPojos";
import Helpers from "@/services/Helpers";

@Component({
  components: {
    FisholaHeader,
    FisholaFooter,
    BackButton,
    NewFishingLicence,
    RouterUtils,
  },
})
export default class NewFishingLicence extends Vue {
  @Prop() id: string;
  private nameError: boolean = false;
  licenceName = ""
  licenceType: LicenceType = "PDF";
  licenceExpirationDate = "";
  loading = true;

  async created() {
    const licence = await FishingLicenceService.getLicence(this.id);
    this.licenceName = licence.name;
    // @ts-ignore
    this.licenceExpirationDate = Helpers.toDateInputString(licence.expirationDate);
    this.licenceType = licence.type;
    this.loading = false;
  }

  async saveLicence(): Promise<void> {
    try {
      if (this.licenceName.trim() === "") {
        this.nameError = true;
        this.$root.$emit(
          "toaster-error",
          "Vous devez nommer la carte de pêche"
        );
        return;
      }

      const licenceDto: LicenceFromClientBean = {
        name: this.licenceName,
        // @ts-ignore
        expirationDate: this.licenceExpirationDate,
        content: "",
        type: this.licenceType
      };

      await FishingLicenceService.putLicence(licenceDto, this.id);
      this.$root.$emit(
        "toaster-success",
        "Une nouvelle carte de pêche a été ajoutée."
      );

      RouterUtils.pushRouteNoDuplicate(this.$router, "/profile/profile");
    } catch (error: any) {
      console.error("Erreur lors de la sauvegarde du fichier", error);
      if (error.content !== undefined && error.content.error !== undefined) {
        this.$root.$emit("toaster-error", error.content.error);
      } else {
        this.$root.$emit(
          "toaster-error",
          "Une erreur inattendue s'est produite. Veuillez réessayer."
        );
      }
    }
  }

  getButtonText() {
    return "Modifier";
  }

  buttonClicked() {
    this.saveLicence();
  }
}
</script>

<style scoped lang="less">
@import "../../less/main";

.page {
  form {
    margin-top: @vertical-margin-large;

    font-size: @fontsize-form-input;
    line-height: calc(@fontsize-form-input + @line-height-padding-medium);

    display: flex;
    flex-direction: column;
    align-items: flex-start;

    label {
      font-weight: 300;
      color: @black;
    }

    input:not([type="file"]) {
      border: 1px solid @pale-sky;
    }

    input[type="file"] {
      padding-left: 0;
    }

    input {
      padding-left: @margin-small;
      padding-right: @margin-small;
      margin-top: @vertical-margin-xx-small;
      margin-bottom: @vertical-margin-large;

      width: 100%;
      height: 38px;
      border-radius: 4px;

      background: transparent;

      color: @gunmetal;
      font-size: @fontsize-form-input;
      font-family: "Open Sans", sans-serif;

      &::placeholder {
        font-style: italic;
        font-weight: normal;
        font-size: @fontsize-form-input;
        color: @pale-sky;
      }
    }

    input.field-error {
      border: 1px solid @cardinal !important;
    }

    div {
      height: calc(@fontsize-form-error + @line-height-padding-medium);
    }

    div.field-error {
      background-color: transparent;
      color: @cardinal;
      font-size: @fontsize-form-error;
      line-height: calc(@fontsize-form-error + @line-height-padding-medium);
    }

    @media screen and (min-width: @desktop-min-width) {
      font-size: @fontsize-form-input-desktop;
      line-height: calc(@fontsize-form-input-desktop + @line-height-padding-medium );

      input {
        font-size: @fontsize-form-input-desktop;
        height: 42px;
        width: 70%;

        &::placeholder {
          font-size: @fontsize-form-input-desktop;
        }
      }
    }
  }

  .preview-item {
    max-width: 100%;
    max-height: 250px;
  }

  .save {
    position: fixed;
    bottom: 50px;
    right: 50px;
    display: flex;
    justify-content: center;
    align-items: center;

    .button {
      margin-left: 0px;
      margin-right: 0px;

      height: 50px;
      border-radius: 50px;

      font-style: normal;
      font-weight: bold;
      font-size: @fontsize-button;
      line-height: calc(@fontsize-button + @line-height-padding-x-large);

      border: 0px;
      padding-left: @margin-medium;
      padding-right: @margin-medium;

      background-color: @terra-cotta;
      color: @white;
    }
  }

  @media screen and (min-width: @desktop-min-width) {
    .container {
      display: flex;
      flex-wrap: wrap;
    }

    .container-form,
    .container-preview {
      flex: 1;
      min-width: 400px;
    }

    .container-preview {
      padding-top: @margin-large;
    }

    embed {
      height: 100%;
    }
  }
}
</style>
