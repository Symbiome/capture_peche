<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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

        <div class="spinner-wrapper" v-if="!ready">
          <div class="spinner"></div>
        </div>

        <div class="pane-content rounded offline" v-if="ready && offline">
          <span>Le tableau de bord n'est pas disponible sans connexion internet</span>
        </div>

        <div class="pane-content large rounded" v-if="ready && !offline">

          <h1>Tableau de bord</h1>

          <div class="dashboard-modes">
            <div class="dashboard-mode"
                 v-bind:class="globalMode ? '' : 'selected'"
                 v-on:click="globalMode = false"
                 >
              Personnel
            </div>
            <div class="dashboard-mode"
                 v-bind:class="globalMode ? 'selected' : ''"
                 v-on:click="globalMode = true"
                 >
              Global
            </div>
          </div>

          <div class="shrinked" v-if="!globalMode">
            <h2><i class="icon-fish" />Mes poissons</h2>
            <DistributionChart :distribution="caughtSpeciesDistribution"></DistributionChart>
          </div>

          <div class="shrinked" v-if="!globalMode">
            <h2><i class="icon-fishing" />Mes captures</h2>
            <div class="not-enough-data" v-if="latestTrips.length == 0">
              <span>Pas assez de données</span>
            </div>
            <div class="average-header" v-if="latestTrips.length > 0">
              <div class="count">{{averageCatchsPerTripRounded}}</div>
              captures en moyenne / sortie
            </div>
            <div class="average">
              <div v-for="(f, index) in latestTrips"
                   v-bind:key="f.tripId"
                   class="average-column"
                   v-on:click="openTrip(f.tripId)">
                <div class="count">
                  {{f.catchsCount}}
                </div>
                <div class="average-row-bar">
                  <div class="average-row-bar-filled"
                        v-if="f.catchsCount > 0"
                        v-bind:class="index % 2 == 0 ? 'even' : 'odd'"
                        v-bind:style="'height: ' + (f.catchsCount * 100 / maxCatchsCount) + '%;'"></div>
                </div>
                <div class="date">
                  <div class="day">
                    {{getDay(f.day)}}
                  </div>
                  <div class="month">
                    {{getMonth(f.day)}}
                  </div>
                  <div class="year">
                    {{getYear(f.day)}}
                  </div>
                </div>
              </div>
              <div v-for="(f, index) in emptylatestTrips"
                   v-bind:key="'empty-' + index"
                   class="average-column">
                <div class="count">
                  -
                </div>
                <div class="average-row-bar">
                </div>
                <div class="date">
                  <div class="day">
                    -
                  </div>
                </div>
              </div>
              <div class="average-threshold"  
                   v-if="latestTrips.length > 0" 
                   v-bind:style="'bottom: ' + (54+(averageCatchsPerTrip * 150 / maxCatchsCount)) + 'px;'"></div>
            </div>
          </div>

          <div class="shrinked" v-if="!globalMode">
            <h2><i class="icon-size" />Historique des tailles (cm)</h2>
            <OptionsList :items="monthlySizesOptions"
                         v-if="monthlySizesOptions.length > 0"
                         v-on:item-selected="onMonthlySizeSelected"></OptionsList>
            <HistogramChart :values="monthlySizes"
                            v-if="monthlySizes"
                            :orderedMonths="orderedMonths"></HistogramChart>
          </div>

          <div v-if="!globalMode">
            <div class="shrinked">
              <h2><i class="icon-size" />Top 5 tailles</h2>
            </div>
            <div class="not-enough-data" v-if="topBySizeOptions.length == 0">
              <span>Pas assez de données</span>
            </div>
            <OptionsList :items="topBySizeOptions"
                         v-if="topBySizeOptions.length > 0"
                         v-on:item-selected="onTopSizeSelected"></OptionsList>
            <div class="dashboard-top-catchs catch-preview-list-scrollable"
                v-if="topBySizeCatchs">
              <CatchPreviewList v-bind:modifiable="false"
                                v-bind:reverse="false"
                                lakeId=""
                                v-bind:catchs="topBySizeCatchs"
                                v-on:openCatchFromId="openCatch($event)"
                                bottomMode="index"/>
            </div>
          </div>

          <div v-if="!globalMode">
            <div class="shrinked">
              <h2><i class="icon-weight" />Top 5 poids</h2>
            </div>
            <div class="not-enough-data" v-if="topByWeightOptions.length == 0">
              <span>Pas assez de données</span>
            </div>
            <OptionsList :items="topByWeightOptions"
                         v-if="topByWeightOptions.length > 0"
                         v-on:item-selected="onTopWeightSelected"></OptionsList>
            <div class="dashboard-top-catchs catch-preview-list-scrollable"
                v-if="topByWeightCatchs">
              <CatchPreviewList v-bind:modifiable="false"
                                v-bind:reverse="false"
                                lakeId=""
                                v-bind:catchs="topByWeightCatchs"
                                v-on:openCatchFromId="openCatch($event)"
                                metaMode="weight"
                                bottomMode="index"/>
            </div>
          </div>

        </div>

      </div>
      <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip"/>
    </div>
    <FisholaFooter shortcuts="logout,dashboard,home"
                   selected="dashboard" />
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue';
import RunningOverlay from '@/components/layout/RunningOverlay.vue';
import FisholaFooter from '@/components/layout/FisholaFooter.vue';

import CatchPreviewList from '@/components/trip/CatchPreviewList.vue';

import OptionsList from '@/components/common/OptionsList.vue';

import DistributionChart from '@/components/charts/DistributionChart.vue';
import HistogramChart from '@/components/charts/HistogramChart.vue';

import DashboardService from '@/services/DashboardService';
import TripsService from '@/services/TripsService';
import {Dashboard, SpeciesWithAlias, DashboardLastTrip, CatchBean, Month} from '@/pojos/BackendPojos';
import DistributionEntry from '@/pojos/DistributionEntry';
import OptionItem from '@/pojos/OptionItem';
import {DashboardAndSpecies} from '@/services/DashboardService';

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../router';

import moment from 'moment';


export class TopEntry {
    constructor (
        public species:SpeciesWithAlias,
        public catchs:CatchBean[]
        ) {
    }
}

@Component({
  components: {
    FisholaHeader,
    RunningOverlay,
    FisholaFooter,
    DistributionChart,
    OptionsList,
    HistogramChart,
    CatchPreviewList
  }
})
export default class DashboardView extends Vue {

  speciesIndex:{ [index: string]: SpeciesWithAlias } = {};
  months = ["Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc"];

  globalMode:boolean = false;

  ready:boolean = false;
  offline:boolean = false;

  caughtSpeciesDistribution:DistributionEntry[] = [];
  averageCatchsPerTripRounded:number = 0;
  averageCatchsPerTrip:number = 0;
  latestTrips:DashboardLastTrip[] = [];
  emptylatestTrips:any[] = [];
  maxCatchsCount:number = 100;
  topBySizeOptions:OptionItem[] = [];
  topBySizeCatchs:CatchBean[] | null = null;
  topByWeightOptions:OptionItem[] = [];
  topByWeightCatchs:CatchBean[] | null = null;
  orderedMonths: Month[] | null = null;
  monthlySizesOptions: OptionItem[] = [];
  monthlySizes: { [P in Month]?: number } | null = null;

  // On a besoin de maintenir un index de capture -> sortie
  catchToTripId:{ [index: string]: string } = {};

  hasRunningTrip:boolean = false;

  constructor() {
    super();
  }

  mounted() {
    DashboardService.loadDashboardOrTimeout()
      .then(this.loaded, this.cannotLoad);
    TripsService.hasRunningTrip()
      .then((result:boolean) => this.hasRunningTrip = result);
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

  loaded(data:DashboardAndSpecies) {
    const speciesAliases = data.dashboard.speciesAliases;
    data.species.forEach((species) => {
      this.speciesIndex[species.id] = species;

      const aliases = speciesAliases[species.id];
      if (aliases) {
        species.alias = aliases.join(' ou ');
      }
    });

    const speciesCount = data.dashboard.caughtSpeciesCount;
    const distribution = data.dashboard.caughtSpeciesDistribution;
    Object.keys(distribution)
      .forEach((speciesId) => {
        const species:SpeciesWithAlias = this.speciesIndex[speciesId];
        const percent:number = Math.round(distribution[speciesId]);
        const count:number = speciesCount[speciesId];
        const entry:DistributionEntry = new DistributionEntry(speciesId, species.name, percent, count, species.alias);
        this.caughtSpeciesDistribution.push(entry);
    });

    this.averageCatchsPerTrip = data.dashboard.averageCatchsPerTrip || 0;
    this.averageCatchsPerTripRounded = Math.round(10 * this.averageCatchsPerTrip) / 10;

    this.maxCatchsCount = 1;
    data.dashboard.latestTripsCatchs.forEach((trip) => {
      this.latestTrips.push(trip);
      if (trip.catchsCount > this.maxCatchsCount) {
        this.maxCatchsCount = trip.catchsCount;
      }
    });
    while ((this.latestTrips.length + this.emptylatestTrips.length) < 9) {
      this.emptylatestTrips.push({});
    }

    const topBySize:TopEntry[] = Vue.lodash.orderBy(this.parseTop(data.dashboard.topBySize), 'species.name');
    topBySize.forEach((top) => {
      this.topBySizeOptions.push({
        id: top.species.id,
        name: top.species.name,
        alias: top.species.alias,
        whatever: top.catchs
      });
    });

    const topByWeight:TopEntry[] = Vue.lodash.orderBy(this.parseTop(data.dashboard.topByWeight), 'species.name');
    topByWeight.forEach((top) => {
      this.topByWeightOptions.push({
        id: top.species.id,
        name: top.species.name,
        alias: top.species.alias,
        whatever: top.catchs
      });
    });

    this.orderedMonths = data.dashboard.orderedMonths;
    const monthlySizesSpecies:string[] = Object.keys(data.dashboard.monthlySizes);
    monthlySizesSpecies.forEach((speciesId) => {
      const species:SpeciesWithAlias = this.speciesIndex[speciesId];
      this.monthlySizesOptions.push({
        id: species.id,
        name: species.name,
        alias: species.alias,
        whatever: data.dashboard.monthlySizes[speciesId]
      });
    });

    this.ready = true;
  }

  parseTop(rawTop:{ [index: string]: CatchBean[] }):TopEntry[] {
    const result:TopEntry[] = [];
    const topSpecies:string[] = Object.keys(rawTop);
    topSpecies.forEach((speciesId) => {
      const species:SpeciesWithAlias = this.speciesIndex[speciesId];
      const catchs:CatchBean[] = [];
      const rawCatchs:any[] = rawTop[speciesId];
      rawCatchs.forEach((rawCatch:any) => {
        this.catchToTripId[rawCatch.id] = rawCatch.tripId;
        const aCatch:CatchBean = TripsService.backendCatchToCatchBean(moment(), rawCatch);
        catchs.push(aCatch);
      })
      const entry:TopEntry = new TopEntry(species, catchs);
      result.push(entry);
    });
    return result;
  }

  onTopSizeSelected(item:OptionItem) {
    this.topBySizeCatchs = item.whatever;
  }

  onTopWeightSelected(item:OptionItem) {
    this.topByWeightCatchs = item.whatever;
  }

  onMonthlySizeSelected(item:OptionItem) {
    this.monthlySizes = item.whatever;
  }

  openCatch(catchId:string) {
    router.push({name:'catch', params: {tripId: this.catchToTripId[catchId], catchId:catchId}});
  }

  openTrip(tripId:string) {
    router.push({name:'trip', params: {id: tripId}});
  }

  getDay(date:number) {
    return new Date(date).getDate();
  }

  getMonth(date:number) {
    const month = new Date(date).getMonth();
    const result = this.months[month];
    return result;
  }

  getYear(date:number) {
    return new Date(date).getFullYear();
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
    height: 100%;

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

    color: @gunmetal;

    &.offline {
      height: 100%;
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

    .dashboard-modes {
      width: 100%;
      display: flex;
      flex-direction: row;
      justify-content: space-evenly;
      margin-bottom: 40px;

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

    .shrinked {
      padding-left: @margin-large;
      padding-right: @margin-large;

      @media(max-width:350px) {
        padding-left: @margin-medium;
        padding-right: @margin-medium;
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
    }

    .average-header {
      height: @average-header-height;
      line-height: @average-header-height;
      font-weight: bold;
      font-size: @fontsize-header-paragraph;
      color: @terra-cotta;

      display: flex;
      flex-direction: row;

      .count {
        width: 30px;
        border-radius: 15px;
        background-color: @terra-cotta;
        color: @white;
        margin-right: @margin-small;
      }
    }

    .average {
      position: relative;
      margin-top: @vertical-margin-small;
      width: 100%;
      display: flex;
      flex-direction: row;
      justify-content: space-between;

      .average-threshold {
        position: absolute;
        width: calc(100% + 60px);
        left: -30px;
        height: 0px;
        border: 1px solid @terra-cotta;
      }

      .average-column {
        display: flex;
        flex-direction: column;
        align-items:center;

        .count {
          font-weight: bold;
          font-size: @fontsize-small-paragraph;
          color: @pelorous;
        }

        .average-row-bar {
          position: relative;
          margin-top: @vertical-margin-xx-small;
          margin-bottom: @vertical-margin-xx-small;
          height: 150px;
          width: 14px;
          border-radius: 7px;
          background: @solitude;

          .average-row-bar-filled {
            position: absolute;
            bottom: 0px;
            width: 14px;
            border-radius: 7px;

            &.even {
              background: @pelorous;
            }

            &.odd {
              background: @summer-sky;
            }

          }
        }

        .date {
          height: 50px;
          font-size: @fontsize-small-paragraph;
          color: @gunmetal;
          .day {
            font-weight: bold;
          }
        }
      }

    }

    .dashboard-top-catchs {

      display: flex;
      flex-direction: row;
      align-items: center;
      overflow-x: auto;
      overflow-y: hidden;
      height: 200px;
      margin-bottom: @vertical-margin-medium;

    }

  }

}

</style>
