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
  <div class="register page-with-header shifted-background">
    <FisholaHeader />
    <div class="page register-page keyboardSensitive">
      <div class="register-form keyboardSensitive">
        <h1 class="keyboardSensitive">Nouvelle carte de pêche</h1>

        <form @submit.prevent="saveFile">
          <label>Nom </label>
          <input
            v-model="newLicenceName"
            placeholder="Donnez un nom à cette carte de pêche"
          />

          <label>Fichier au format PDF ou JPEG</label>
          <input type="file" @change="handleFileChange" />

          <label>Date d'expiration</label>
          <input type="date" v-model="newLicenceExpirationDate" />

          <button class="button button-primary" type="submit">
            Enregistrer le fichier
          </button>
        </form>
      </div>
    </div>
    <FisholaFooter shortcuts="back,home,dashboard" />
  </div>
</template>

<script lang="ts">
import router from "@/router";
import { RouterUtils } from "@/router/RouterUtils";

import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import FishingLicenceService from "@/services/FishingLicenceService";

import { Component, Prop, Vue } from "vue-property-decorator";
import { LicenceFromClientBean, LicenceType } from "@/pojos/BackendPojos";
import Constants from "@/services/Constants";

@Component({
  components: {
    FisholaHeader,
    FisholaFooter,
    NewFishingLicence,
    RouterUtils,
    router,
  },
})
export default class NewFishingLicence extends Vue {
  private selectedFile: File | null = null;
  private newLicenceName: string = "";
  private newLicenceExpirationDate: Date = new Date();
  private newLicenceType: LicenceType = "PDF";

  fileTypeMap: {
    [key: string]: string;
    "application/pdf": string;
    "image/jpeg": string;
  } = {
    "application/pdf": "PDF",
    "image/jpeg": "JPEG",
  };

  created() {
    this.resetExpirationDate();
  }

  handleFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    }
  }

  resetExpirationDate() {
    // Expiration date is set to 2 years from now
    const twoYearsLater = new Date();
    twoYearsLater.setFullYear(twoYearsLater.getFullYear() + 2);
    this.newLicenceExpirationDate = twoYearsLater;
  }

  async saveFile(): Promise<void> {
    try {
      if (!this.selectedFile) {
        console.error("Veuillez sélectionner un fichier");
        return;
      }

      // Set newLicenceType according to selectedFile type
      const selectedFileType = this.selectedFile.type;
      if (this.fileTypeMap[selectedFileType]) {
        this.newLicenceType = this.fileTypeMap[selectedFileType] as LicenceType;
      } else {
        console.error("Format de fichier invalide");
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

      let licenceId = await FishingLicenceService.postLicence(licenceDto);

      const licenceURL = Constants.apiUrl(`/v1/licences/${licenceId}`);
      this.$emit("uploaded-licence", licenceURL);

      RouterUtils.pushRouteNoDuplicate(router, "/licences");
    } catch (error) {
      console.error("Erreur lors de la sauvegarde du fichier", error);
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
}
</script>

<style scoped lang="less">
@import "../../less/main";

form {
  margin-top: @vertical-margin-large;

  font-size: @fontsize-form-input;
  line-height: calc(@fontsize-form-input + @line-height-padding-medium);

  // color: @white;

  display: flex;
  flex-direction: column;
  align-items: flex-start;

  label {
    font-weight: 300;
    color: @black;
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
    border: 1px solid @pale-sky;

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
    line-height: calc(
      @fontsize-form-input-desktop + @line-height-padding-medium
    );

    input {
      font-size: @fontsize-form-input-desktop;
      height: 42px;

      &::placeholder {
        font-size: @fontsize-form-input-desktop;
      }
    }
  }
}
</style>
