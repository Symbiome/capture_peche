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
    <FisholaHeader/>
    <div class="page dashboard-page">
      <div class="pane pane-only">

        <div class="pane-content large rounded">

          <h1 class="no-margin-pane">
            <span>
              Tableau de bord
            </span>
            <a @click="askForAsyncExport"
               v-if="!globalMode && asyncExport"
               class="export"
               title="Exporter par email">
              <span>Exporter</span>
              <i class="icon-download"/>
            </a>
            <a v-bind:href="exportUrl"
               v-if="!globalMode && !asyncExport"
               class="export"
               title="Exporter"
               target="_blank">
              <span>Exporter</span>
              <i class="icon-download"/>
            </a>
          </h1>

          <div class="dashboard-modes">
            <div class="dashboard-mode"
                 v-bind:class="globalMode ? '' : 'selected'"
                 v-on:click="showPersonalDashboard"
                 >
              Personnel
            </div>
            <div class="dashboard-mode"
                 v-bind:class="globalMode ? 'selected' : ''"
                 v-on:click="showGlobalDashboard"
                 >
              Global
            </div>
          </div>

          <div class="spinner-wrapper" v-if="!ready">
            <div class="spinner"></div>
          </div>

          <div class="offline" v-if="ready && offline">
            <span>Le tableau de bord n'est pas disponible sans connexion internet</span>
          </div>

          <PersonalDashboard v-if="!globalMode && personalDashboard"
                             :dashboardData="personalDashboard"></PersonalDashboard>

          <GlobalDashboardComponent v-if="globalMode && globalDashboard"
                                    :dashboardData="globalDashboard"></GlobalDashboardComponent>

        </div>

      </div>
      <RunningOverlay class="hiddenWhenKeyboardShows"
                      v-if="hasRunningTrip"/>
    </div>
    <FisholaFooter shortcuts="logout,dashboard,home"
                   selected="dashboard" />
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue';
import RunningOverlay from '@/components/layout/RunningOverlay.vue';
import FisholaFooter from '@/components/layout/FisholaFooter.vue';

import PersonalDashboard from '@/components/charts/PersonalDashboard.vue';
import GlobalDashboardComponent from '@/components/charts/GlobalDashboardComponent.vue';

import DashboardService from '@/services/DashboardService';
import Helpers from '@/services/Helpers';
import TripsService from '@/services/TripsService';
import {DashboardAndSpecies, GlobalDashboardAndSpecies} from '@/services/DashboardService';

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../router';

import moment from 'moment';


@Component({
  components: {
    FisholaHeader,
    RunningOverlay,
    FisholaFooter,
    PersonalDashboard,
    GlobalDashboardComponent
  }
})
export default class DashboardView extends Vue {

  globalMode:boolean = false;

  exportUrl:string = '';

  ready:boolean = false;
  offline:boolean = false;

  asyncExport:boolean = false;

  personalDashboard:DashboardAndSpecies|null = null;
  globalDashboard:GlobalDashboardAndSpecies|null = null;

  hasRunningTrip:boolean = false;

  constructor() {
    super();
  }

  mounted() {
    TripsService.hasRunningTrip()
      .then((result:boolean) => this.hasRunningTrip = result);
    DashboardService.loadDashboardOrTimeout()
      .then(this.personalDashboardLoaded, this.cannotLoad);
    this.exportUrl = DashboardService.getExportUrl();
    Helpers.ifApplication(() => this.asyncExport = true);
  }

  personalDashboardLoaded(data:DashboardAndSpecies) {
    this.personalDashboard = data;
    this.ready = true;
  }

  cannotLoad(error:any) {
    if (error && error.timeoutReached) {
      this.offline = true;
    } else if (error && error.status == 401) {
      this.$root.$emit('toaster-warning', 'Vous n\'êtes plus connecté\u00B7e');
      router.push('/login');
    }
    this.ready = true;
  }

  showPersonalDashboard() {
    this.globalMode = false;
    if (this.personalDashboard) {
      this.ready = true;
    }
  }

  showGlobalDashboard() {
    this.globalMode = true;

    if (!this.globalDashboard) {
      this.ready = false;
      DashboardService.loadGlobalDashboardOrTimeout()
        .then(this.globalDashboardLoaded, this.cannotLoad);
   }
  }

  globalDashboardLoaded(data:GlobalDashboardAndSpecies) {
    this.globalDashboard = data;
    this.ready = true;
  }

  askForAsyncExport() {
    DashboardService.asyncExport()
      .then(
        () => {
          this.$root.$emit('toaster-success', 'Export envoyé par e-mail');
        },
        () => {
          this.$root.$emit('toaster-error', 'Une erreur est survenue pendant l\'export');
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


  @keyframes spin { 100% { -webkit-transform: rotate(360deg); transform:rotate(360deg); } }

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
      animation:spin 2s linear infinite;
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

    text-align:center;

    padding-top: @vertical-margin-large;

    @media(max-height:579px) {
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

      @media(max-width:350px) {
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
        justify-content: space-between;
        align-items: center;

        a.export {
          font-size: 30px;
          margin-left: 0px;
        }
      }

      .dashboard-modes {
        .dashboard-mode {
          width: 40%;
        }
      }

    }
  }

  @media screen and (max-width: 899px) {
    .pane-content {
      h1 {
        a.export {
          span {
            display: none;
          }
        }
      }
    }
  }

  @media screen and (min-width: 900px) {
    .pane-content {
      h1 {
        a.export {
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

  @media screen and (min-width: 431px) {
    .show-if-small {
      display: none;
    }
  }
}

</style>
