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
  <div class="licences page-with-header-and-footer shifted-background">
    <FisholaHeader />

    <div class="page licences-page">
      <div class="pane pane-only">
        <div class="pane-content rounded">
          <h1 class="no-margin-pane">Cartes de pêche</h1>
          <h2 v-if="elements.length > 0">Mes cartes de pêche</h2>
          <div class="licences-display">
            <div
              class="licences-row"
              v-for="licence in elements"
              v-bind:key="licence.id"
            >
              <span>{{ licence.name }}</span>
              <a @click="openLicenceInNewWindow(licence)" title="Afficher">
                <i class="icon-download" />
              </a>

              <a @click="deleteLicence(licence)" title="Supprimer">
                <i class="icon-delete" />
              </a>
            </div>
          </div>

          <h2>Enregistrer une nouvelle carte de pêche</h2>
          <div class="login-form">
            <form @submit.prevent="saveFile">
              <label>Sélectionner un fichier : </label>
              <input type="file" @change="handleFileChange" />

              <label>Nom de la carte de pêche : </label>
              <input v-model="newLicenceName" required />

              <label>Date d'expiration : </label>
              <input type="date" v-model="newLicencExpirationDate" />

              <button type="submit">Enregistrer le fichier</button>
            </form>
          </div>
          <div class="bottom-page-spacer"></div>
        </div>
      </div>
      <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip" />
    </div>
    <FisholaFooter shortcuts="logout,dashboard,home" selected="dashboard" />
  </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import FishingLicenceService from "@/services/FishingLicenceService";
import TripsService from "@/services/TripsService";
import {
  LicenceResponseBean,
  LicenceFromClientBean,
  LicenceType,
} from "@/pojos/BackendPojos";

import { Component, Prop, Vue } from "vue-property-decorator";
import Constants from "../services/Constants";

@Component({
  components: {
    FisholaHeader,
    RunningOverlay,
    FisholaFooter,
    LicencesView,
  },
})
export default class LicencesView extends Vue {
  hasRunningTrip: boolean = false;
  elements: LicenceResponseBean[] = [];
  private selectedFile: File | null = null;
  private newLicenceName: string = "";
  private newLicenceExpirationDate: Date;
  private newLicenceType: LicenceType = "PDF";

  mounted() {
    this.fetchLicences();
    TripsService.hasRunningTrip().then(
      (result: boolean) => (this.hasRunningTrip = result)
    );

    this.resetNewLicenceExpirationDate();
  }

  // Define default expiration date : 2 years from now
  private resetNewLicenceExpirationDate() {
    const twoYearsLater = new Date();
    twoYearsLater.setFullYear(twoYearsLater.getFullYear() + 2);
    this.newLicenceExpirationDate = twoYearsLater;
  }

  fetchLicences() {
    FishingLicenceService.getAllLicences().then(this.licencesLoaded);
  }

  licencesLoaded(licences: LicenceResponseBean[]) {
    this.elements = [];
    const sortedLicences = Vue.lodash.orderBy(licences, "name");
    sortedLicences.forEach((licence) => this.elements.push(licence));
  }

  handleFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    }
  }

  async saveFile(): Promise<void> {
    try {
      if (!this.selectedFile) {
        console.error("Veuillez sélectionner un fichier");
        return;
      }

      const fileTypeMap: {
        [key: string]: string;
        "application/pdf": string;
        "image/jpeg": string;
      } = {
        "application/pdf": "PDF",
        "image/jpeg": "JPEG",
      };

      const selectedFileType = this.selectedFile.type;

      if (fileTypeMap[selectedFileType]) {
        this.newLicenceType = fileTypeMap[selectedFileType] as LicenceType;
      } else {
        console.error(
          "Format de fichier invalide (doit être un PDF ou une image au format JPEG)"
        );
        return;
      }

      const contentBase64 = await this.getBase64Content(this.selectedFile);

      const licenceDto: LicenceFromClientBean = {
        name: this.newLicenceName,
        expirationDate: this.newLicenceExpirationDate,
        type: this.newLicenceType,
        content: contentBase64,
      };

      let licenceId = await FishingLicenceService.postLicence(licenceDto);
      const licenceURL = Constants.apiUrl(`/v1/licences/${licenceId}`);
      this.$emit("uploaded-licence", licenceURL);

      this.fetchLicences();

      // Reset
      this.selectedFile = null;
      this.newLicenceName = "";
      this.newLicenceType = "PDF";
      this.resetNewLicenceExpirationDate();
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

  async openLicenceInNewWindow(licence: LicenceResponseBean): Promise<void> {
    try {
      const fileBlob: Blob = await FishingLicenceService.getLicence(licence.id);
      const fileUrl = URL.createObjectURL(fileBlob);

      if (licence.type === "PDF") {
        window.open(fileUrl, "_blank");
      } else {
        const img = new Image();
        img.src = fileUrl;
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
    this.fetchLicences();
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../less/main";

.licences-page {
  form {
    display: block;
    max-width: 300px;
    margin: auto;
  }

  input,
  select,
  textarea {
    display: block;
    margin-bottom: 10px;
  }

  .pane .pane-content {
    padding-left: 0px;
    padding-right: 0px;
  }

  .licences-row {
    padding-left: @margin-x-large;
    padding-right: @margin-x-large;
    height: 56px;

    @media (max-height: 579px) {
      height: 46px;
    }

    @media (max-width: 370px) {
      padding-left: @margin-large;
      padding-right: @margin-large;
    }

    border-bottom: 1px solid @solitude;

    display: flex;
    justify-content: space-between;
    align-items: center;

    span {
      font-size: @fontsize-small-paragraph;
      line-height: calc(
        @fontsize-small-paragraph + @line-height-padding-medium
      );
      color: @gunmetal;
    }

    a {
      color: @pelorous;
      font-size: @fontsize-paragraph;
    }

    @media screen and (min-width: @desktop-min-width) {
      span {
        font-size: @fontsize-paragraph;
        line-height: calc(@fontsize-paragraph + @line-height-padding-medium);
      }
      a {
        font-size: @fontsize-paragraph-desktop;
      }
    }

    @media screen and (min-width: 800px) {
      padding-left: @margin-large-desktop;
      padding-right: @margin-large-desktop;
    }
  }
}

@media screen and (max-width: 760px) {
}
</style>
