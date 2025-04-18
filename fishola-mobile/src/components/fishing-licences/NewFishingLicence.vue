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
          Nouvelle carte de pêche
        </h1>
        <div class="pane-content rounded">
          <div class="container">
            <div class="container-form keyboardSensitive">
              <form @submit.prevent="saveFile">
                <label>Nom de la carte de pêche </label>
                <input v-model="newLicenceName" :class="{ 'field-error': nameError }"
                  placeholder="Nommez la carte de pêche" />

                <label>Date d'expiration (Défaut :
                  {{ formattedDate(getDefaultDate()) }})</label>
                <input type="date" v-model="newLicenceExpirationDate" />

                <label>Sélectionnez un fichier au format PDF, PNG ou JPEG</label>
                <input type="file" @change="handleFileChange"
                  accept="application/pdf, image/jpeg, image/jpg, image/png" />
              </form>
            </div>
            <div class="container-preview">
              <embed class="preview-item" v-if="url" :src="url" />
            </div>
          </div>
          <div class="save">
            <button class="button hide-on-mobile" type="submit" @click="saveFile">
              Enregistrer
            </button>
          </div>
        </div>
      </div>
    </div>

    <FisholaFooter shortcuts="back,home,dashboard" v-bind:button-text="getButtonText()" v-on:buttonClicked="saveFile" />
  </div>
</template>

<script lang="ts">
import { RouterUtils } from "@/router/RouterUtils";

import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";
import BackButton from "@/components/common/BackButton.vue";

import FishingLicenceService from "@/services/FishingLicenceService";

import { Component, Vue } from "vue-property-decorator";
import { LicenceFromClientBean, LicenceType } from "@/pojos/BackendPojos";

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
  private selectedFile: File | null = null;
  private newLicenceName: string = "";
  private newLicenceExpirationDate: Date = new Date();
  private newLicenceType: LicenceType = "PDF";
  private url: string = "";
  private nameError: boolean = false;

  fileTypeMap: {
    [key: string]: string;
    "application/pdf": string;
    "image/jpeg": string;
    "image/png": string;
  } = {
      "application/pdf": "PDF",
      "image/jpeg": "JPEG",
      "image/png": "PNG",
    };

  formattedDate(date: Date): string {
    var dayOptions: Intl.DateTimeFormatOptions = {
      month: "numeric",
      day: "numeric",
      year: "numeric",
    };

    const dateString = date.toLocaleDateString("fr-FR", dayOptions);
    return dateString;
  }

  created() {
    this.newLicenceExpirationDate = this.getDefaultDate();
    this.newLicenceName = "Carte " + this.newLicenceExpirationDate.getFullYear();
  }

  handleFileChange(event: Event): void {
    try {
      const input = event.target as HTMLInputElement;
      if (input.files && input.files.length > 0) {
        this.selectedFile = input.files[0];
        this.url = URL.createObjectURL(this.selectedFile);
      }
    } catch (error) {
      console.error("Erreur pendant la gestion du changement de fichier : ", error);
    }
  }

  getDefaultDate(): Date {
    const currentDate = new Date();
    const currentYear = currentDate.getFullYear();
    const date = new Date("December 31, " + currentYear + " 17:00:00 GMT+1:00");
    return date;
  }

  async saveFile(): Promise<void> {
    try {
      if (this.newLicenceName.trim() === "") {
        this.nameError = true;
        this.$root.$emit(
          "toaster-error",
          "Vous devez nommer la carte de pêche"
        );
        return;
      }

      if (!this.selectedFile) {
        this.$root.$emit("toaster-error", "Vous devez sélectionner un fichier");
        return;
      }

      // Set newLicenceType according to selectedFile type
      const selectedFileType = this.selectedFile.type;
      if (this.fileTypeMap[selectedFileType]) {
        this.newLicenceType = this.fileTypeMap[selectedFileType] as LicenceType;
      } else {
        this.$root.$emit("toaster-error", "Format de fichier invalide");
        return;
      }

      // Encode the selectedFile content into a Base 64 string
      const contentBase64 = await this.getBase64Content(this.selectedFile);

      // Build a DTO for the licence to save, then send it to the back-end
      const licenceDto: LicenceFromClientBean = {
        name: this.newLicenceName,
        expirationDate: this.newLicenceExpirationDate,
        type: this.newLicenceType,
        content: contentBase64,
      };

      await FishingLicenceService.postLicence(licenceDto);
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

  getBase64Content(file: any): Promise<string> {
    return new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);

      reader.onload = function () {
        const base64String = reader.result as string;
        const contentBase64 = base64String.split(",")[1];
        resolve(contentBase64);
      };

      reader.onerror = function (error) {
        reject(error);
      };
    });
  }

  getButtonText() {
    return "Enregistrer";
  }

  buttonClicked() {
    this.saveFile();
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
