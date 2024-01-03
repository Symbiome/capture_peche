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
          <h1 class="no-margin-pane">Mes cartes de pêche</h1>
          <FishingLicenceList
            v-bind:licences="licences"
            v-on:licence-selected="licenceSelected"
            v-on:licence-unselected="licenceUnselected"
          />

          <div class="bottom-page-spacer"></div>

          <div class="create-and-delete hide-on-mobile">
            <div
              v-if="!hasRunningTrip"
              class="button button-primary"
              :class="{ delete: selectedLicencesIds.length > 0 }"
            >
              <div class="button button-primary">
                <button @click="buttonClicked" class="new-button">
                  <i :class="getButtonIcon()" />
                  {{ getButtonText() }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip" />
    </div>
    <FisholaFooter
      shortcuts="back,home,dashboard"
      v-bind:hideButton="hasRunningTrip"
      v-bind:button-icon="getButtonIcon()"
      v-bind:button-text="getButtonText()"
      v-on:buttonClicked="buttonClicked"
    />
  </div>
</template>

<script lang="ts">
import router from "@/router";
import { RouterUtils } from "@/router/RouterUtils";

import Helpers from "@/services/Helpers";

import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import FishingLicenceList from "@/components/fishing-licences/FishingLicenceList.vue";
import NewFishingLicence from "@/components/fishing-licences/NewFishingLicence.vue";

import FishingLicenceService from "@/services/FishingLicenceService";
import TripsService from "@/services/TripsService";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import { LicenceResponseBean } from "@/pojos/BackendPojos";

@Component({
  components: {
    FisholaHeader,
    RunningOverlay,
    FisholaFooter,
    FishingLicencesView,
    FishingLicenceList,
    NewFishingLicence,
  },
})
export default class FishingLicencesView extends Vue {
  hasRunningTrip: boolean = false;
  licences: LicenceResponseBean[] = [];

  selectedLicencesIds: string[] = [];

  constructor() {
    super();
    this.licences = [];
  }

  mounted() {
    this.fetchAllLicences();
  }

  created() {
    this.fetchAllLicences();
    TripsService.hasRunningTrip().then(
      (result: boolean) => (this.hasRunningTrip = result)
    );
  }

  fetchAllLicences() {
    FishingLicenceService.getAllLicences().then(this.loadLicences);
  }

  loadLicences(licences: LicenceResponseBean[]) {
    this.licences = [];
    const sortedLicences = Vue.lodash.orderBy(licences, "name");
    sortedLicences.forEach((licence) => this.licences.push(licence));
  }

  licenceSelected(licenceId: string) {
    this.selectedLicencesIds.push(licenceId);
  }

  licenceUnselected(licenceId: string) {
    const index = this.selectedLicencesIds.indexOf(licenceId);
    if (index != -1) {
      this.selectedLicencesIds.splice(index, 1);
    }
  }

  getButtonIcon() {
    return this.selectedLicencesIds.length == 0 ? "icon-plus" : "icon-delete";
  }

  getButtonText() {
    return this.selectedLicencesIds.length == 0
      ? "Nouvelle carte"
      : "Supprimer";
  }

  buttonClicked() {
    if (this.selectedLicencesIds.length == 0) {
      RouterUtils.pushRouteNoDuplicate(router, "/licences/new");
    } else {
      let message = "Voulez-vous supprimer cette carte ?";
      if (this.selectedLicencesIds.length > 1) {
        message = "Voulez-vous supprimer ces cartes ?";
      }
      Helpers.confirm(this.$modal, message).then(this.deleteSelectedLicences);
    }
  }

  deleteSelectedLicences() {
    let message;
    if (this.selectedLicencesIds.length > 1) {
      message = "Les cartes sélectionnées ont été supprimées.";
    } else {
      message = "La carte sélectionnée a été supprimée.";
    }

    this.selectedLicencesIds.forEach((licenceId) =>
      FishingLicenceService.deleteLicence(licenceId).then(this.fetchAllLicences)
    );

    this.$root.$emit("toaster-success", message);
    this.selectedLicencesIds = [];
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../less/main";

.licences-page {
  display: flex;
  flex-direction: column;

  .licences-list {
    flex-grow: 1;
    overflow: auto;
    padding-bottom: 100px;
  }

  .bottom {
    position: absolute;
    bottom: 0px;
    width: 100%;
    margin-left: -30px;
  }

  @media screen and (max-width: 1150px) and (min-width: 770px) {
    .new-button {
      font-size: 14px;
    }
  }

  @media screen and (min-width: @desktop-min-width) {
    background-color: @white-smoke;

    h1 {
      font-style: normal;
      font-weight: normal;

      color: @pelorous;
      margin-top: @margin-medium;
      margin-bottom: @margin-xx-large;
      font-size: @fontsize-title-desktop;
      height: calc(@fontsize-title-desktop + @line-height-padding-xx-large);
      line-height: calc(
        @fontsize-title-desktop + @line-height-padding-xx-large
      );
      text-align: left;

      margin-left: @margin-large-desktop;
      margin-right: @margin-large-desktop;
    }

    .create-and-delete {
      display: flex;
      flex-direction: row;
      align-items: center;
      .button {
        margin-left: 0px;
        margin-right: 0px;

        &.delete {
          button {
            background-color: @cardinal;
          }
        }
      }
    }

    .bottom {
      position: absolute;
      bottom: 0px;
      width: calc(100% - @desktop-menu-width);
      margin-left: -66px;
    }
  }
}
</style>
