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
  <div class="dashboard page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="page dashboard-page">
      <div class="pane pane-only">
        <div id="scrollable" class="pane-content large rounded">
          <h1 class="no-margin-pane">
            <span> Mes données </span>
            <div class="selects-holder">
              <select placeholder="lake" v-model="selectedLakeUUID">
                <option v-for="lake in lakes" :value="lake.id" :key="lake.uuid">
                  {{ lake.name }}
                </option>
              </select>
              <select placeholder="Année" v-model="year">
                <option v-for="dashboardYear in getDashboardYears()" :value="dashboardYear" :key="dashboardYear">
                  {{ dashboardYear }}
                </option>
              </select>
            </div>
            <a v-bind:href="exportUrl" v-if="visualizationMode !== 'evolution' && !asyncExport" id="export-button"
              class="export" title="Exporter" target="_blank">
              <span>Exporter</span>
              <i class="icon-download" />
            </a>
          </h1>

          <div class="dashboard-modes">
            <div class="dashboard-mode" v-bind:class="visualizationMode === 'evolution' ? '' : 'selected'"
              v-on:click="showPersonalDashboard">
              Tableau de bord
            </div>
            <div class="dashboard-mode" v-bind:class="visualizationMode === 'evolution' ? 'selected' : ''"
              v-on:click="changeVisualizationMode('evolution')">
              Évolution
            </div>
          </div>

          <div class="spinner-wrapper" v-if="!ready">
            <div class="spinner"></div>
          </div>

          <div class="offline" v-if="ready && offline">
            <span>Le tableau de bord n'est pas disponible sans connexion
              internet</span>
          </div>
          <PersonalDashboard v-if="visualizationMode !== 'evolution' && personalDashboard" :year="year"
            :dashboardData="personalDashboard" :selectedLakeUUID="selectedLakeUUID"></PersonalDashboard>
          <p v-if="visualizationMode === 'evolution'">Evolution </p>
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

import PersonalDashboard from "@/components/charts/PersonalDashboard.vue";

import DashboardService from "@/services/DashboardService";
import Helpers from "@/services/Helpers";
import TripsService from "@/services/TripsService";
import {
  DashboardAndSpecies
} from "@/services/DashboardService";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import router from "../router";
import { RouterUtils } from "@/router/RouterUtils";
import { Lake } from "@/pojos/BackendPojos";

import ReferentialService from "../services/ReferentialService";

@Component({
  components: {
    FisholaHeader,
    RunningOverlay,
    FisholaFooter,
    PersonalDashboard
  },
})
export default class DashboardPersonalView extends Vue {
  @Prop()
  visualizationMode: string;

  exportUrl: string = "";

  ready: boolean = false;
  offline: boolean = false;

  asyncExport: boolean = false;

  personalDashboard: DashboardAndSpecies | null = null;

  hasRunningTrip: boolean = false;
  year: number = new Date().getFullYear();
  selectedLakeUUID = "";
  lakes: Lake[] = [];
  isFirstLoad = true;

  changeVisualizationMode(newMode: string) {
    this.$router.push({ params: { visualizationMode: newMode } });
  }

  created() {
    if (localStorage && localStorage.latestSelectedLakeUUID && localStorage.latestSelectedLakeUUID != "all") {
      this.selectedLakeUUID = localStorage.latestSelectedLakeUUID
    }
    this.loadLakes();
  }

  mounted() {
    TripsService.hasRunningTrip().then(
      (result: boolean) => (this.hasRunningTrip = result)
    );
    DashboardService.loadDashboardOrTimeout(this.year, "").then(
      this.personalDashboardLoaded,
      this.cannotLoad
    );
    this.exportUrl = DashboardService.getExportUrl();
    Helpers.ifApplication(() => (this.asyncExport = true));
    const scrolllistener = document.getElementById("scrollable");
    scrolllistener?.addEventListener("scroll", (_event: Event) => {
      const exportButton = document.getElementById("export-button");
      if (exportButton && exportButton.style) {
        if (scrolllistener.scrollTop > 50) {
          exportButton.style.display = "none";
        } else {
          exportButton.style.display = "block";
        }
      }
    });
  }

  @Watch("year")
  @Watch("selectedLakeUUID")
  yearOrSelectedLakesChanged(): void {
    if (this.selectedLakeUUID) {
      localStorage.latestSelectedLakeUUID = this.selectedLakeUUID
    } else {
      localStorage.latestSelectedLakeUUID = "all"
    }
    if (this.visualizationMode == 'dashboard') {
      DashboardService.loadDashboardOrTimeout(
        this.year,
        this.selectedLakeUUID
      ).then(this.personalDashboardLoaded, this.cannotLoad);
    } else {
      // TODO load evolution
    }
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
    try {
      const allLakes = await ReferentialService.getLakes();
      this.lakes = this.lakes.concat(allLakes);
    } catch (e) {
      // Silent catch, no more lakes will be added
    }
  }

  getDashboardYears(): number[] {
    const rangeDesc = (from: number, to: number) =>
      [...Array(Math.floor(from - to) + 1)].map((_, i) => from + i * -1);
    return rangeDesc(new Date().getFullYear(), 2020);
  }

  personalDashboardLoaded(data: DashboardAndSpecies) {
    // If no data for current year and this is first load, select year - 1 by default
    if (
      this.isFirstLoad &&
      data.dashboard &&
      data.dashboard.latestTripsCatchs.length == 0
    ) {
      this.year = this.year - 1;
      this.yearOrSelectedLakesChanged();
    } else {
      this.personalDashboard = data;
      this.ready = true;
    }
    this.isFirstLoad = false;
  }

  cannotLoad(error: any) {
    if (error && error.timeoutReached) {
      this.offline = true;
    } else if (error && error.status == 401) {
      this.$root.$emit("toaster-warning", "Vous n'êtes plus connecté\u00B7e");
      RouterUtils.pushRouteNoDuplicate(router, "/login");
    }
    this.ready = true;
  }

  showPersonalDashboard() {
    this.changeVisualizationMode('dashboard')
    if (this.personalDashboard) {
      this.ready = true;
    }
  }

  askForAsyncExport() {
    DashboardService.asyncExport().then(
      () => {
        this.$root.$emit("toaster-success", "Export envoyé par e-mail");
      },
      () => {
        this.$root.$emit(
          "toaster-error",
          "Une erreur est survenue pendant l'export"
        );
      }
    );
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../less/main";

.dashboard-page {
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  text-align: center;

  @keyframes spin {
    100% {
      -webkit-transform: rotate(360deg);
      transform: rotate(360deg);
    }
  }

  .alias {
    font-style: italic;
    color: @pale-sky;
  }

  .spinner-wrapper {
    width: 100%;
    height: 200px;

    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;

    .spinner {
      height: 60px;
      width: 60px;
      border-radius: 50%;
      border-top: 3px solid @pelorous;
      border-left: 3px solid @pelorous;
      animation: spin 2s linear infinite;
    }
  }

  .offline {
    height: 200px;
    padding-left: @margin-xx-large;
    padding-right: @margin-xx-large;
    display: flex;
    flex-direction: column;
    justify-content: center;

    span {
      color: @carrot-orange;
      font-size: @fontsize-span-big;
      line-height: calc(@fontsize-span-big + @line-height-padding-x-large);
    }
  }

  .not-enough-data {
    height: 50px;

    span {
      font-style: italic;
      font-size: @fontsize-button;
      line-height: calc(@fontsize-button + @line-height-padding-x-large);
      color: @pale-sky;
      text-align: center;
      margin-top: @vertical-margin-large;
    }
  }

  .pane-content {
    overflow: auto;

    text-align: center;

    padding-top: @vertical-margin-large;

    @media (max-height: 579px) {
      padding-top: @vertical-margin-small;
    }

    @media screen and (min-width: @desktop-min-width) {
      padding-top: 0px;
    }

    color: @gunmetal;

    h1 a.export {
      font-size: 18px;
      margin-left: 50px;
      color: @pale-sky;
    }

    .dashboard-modes {
      width: 100%;
      display: flex;
      flex-direction: row;
      justify-content: space-evenly;

      .dashboard-mode {
        color: @pale-sky;
        padding-bottom: 5px;
        padding-left: 20px;
        padding-right: 20px;
        cursor: pointer;

        &.selected {
          color: @gunmetal;
          border-bottom: 2px solid @pelorous;
        }
      }
    }

    .two-sections {
      display: flex;
      flex-direction: column;
      width: 100%;

      @media screen and (min-width: 1074px) {
        display: flex;
        flex-direction: row;
        justify-content: center;
        padding-left: @margin-large;
        padding-right: @margin-large;

        .section {
          width: 50%;

          &.shrinked {
            padding-left: @margin-large;
            padding-right: @margin-large;
          }
        }
      }
    }

    .section {
      margin-top: 50px;
    }

    .shrinked {
      padding-left: @margin-large;
      padding-right: @margin-large;

      @media (max-width: 350px) {
        padding-left: @margin-medium;
        padding-right: @margin-medium;
      }

      @media screen and (min-width: @desktop-min-width) {
        padding-left: @margin-large-desktop;
        padding-right: @margin-large-desktop;
      }
    }

    h2 {
      i {
        color: @pelorous;
        margin-right: @margin-small;
      }

      font-style: normal;
      font-weight: normal;
      font-size: @fontsize-title;
      line-height: calc(@fontsize-title + @line-height-padding-xx-large);
      text-align: left;

      @media screen and (max-width: 430px) {
        span.hide-if-small {
          display: none;
        }
      }
    }
  }

  @media screen and (min-width: @desktop-min-width) {
    .pane-content {
      h1 {
        display: flex;
        flex-direction: row;
        justify-content: flex-start;
        align-items: center;

        a.export {
          font-size: 30px;
          margin-left: 0px;
          margin-left: auto;
        }
      }

      .selects-holder {
        margin-left: 40px;
        margin-top: -10px;
      }

      .dashboard-modes {
        .dashboard-mode {
          width: 40%;
        }
      }
    }
  }

  @media screen and (max-width: 1180px) {
    .pane-content {
      h1 {
        margin-top: 60px;
        flex-direction: column;
        justify-content: center;
        align-items: flex-start;
      }

      .selects-holder {
        margin-left: 0px;
        margin-top: 0px;
        margin-bottom: 20px;
      }
    }
  }

  @media screen and (max-width: 899px) {
    .pane-content {
      h1 {
        margin-top: 60px;
        flex-direction: column;
        justify-content: center;
        align-items: flex-start;

        a.export {
          position: absolute;
          float: right;
          top: 20px;
          right: 18px;

          span {
            display: none;
          }
        }
      }
    }
  }

  @media screen and (max-width: 770px) {
    .pane-content {
      h1 {
        height: 60px;
        margin-top: 0px;

        a.export {
          position: absolute;
          float: right;
          top: 80px;
          right: 18px;
        }
      }
    }
  }

  @media screen and (min-width: 900px) {
    .pane-content {
      h1 {
        a.export {
          position: relative;
          top: -115px;
          right: 18px;
          font-weight: bold;
          line-height: 22px;
          height: 33px;
          padding: 5px;
          border-radius: 16px;
          padding-left: 16px;
          padding-right: 16px;
          border: 0px;
          text-decoration: none;
          color: @white;
          background-color: @carrot-orange;
          font-size: 16px;

          span {
            margin-right: 10px;
          }
        }
      }
    }
  }

  @media screen and (min-width: 1178px) {
    .pane-content {
      h1 {
        a.export {
          top: 0px;
        }
      }
    }
  }

  @media screen and (min-width: 431px) {
    .show-if-small {
      display: none;
    }
  }
}

.selects-holder {
  select {
    background-color: white;
    padding: 10px;
    height: 40px;
    border: 1px solid @pale-sky;
    border-radius: 3px;
    margin-left: 10px;
  }
}
</style>
