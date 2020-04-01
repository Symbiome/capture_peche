<template>
  <div class="dashboard page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page dashboard-page">
      <div class="pane pane-only">
        <h1>Tableau de bord</h1>

        <div class="spinner-wrapper" v-if="!ready">
          <div class="spinner"></div>
        </div>

        <div class="pane-content-large" v-if="ready">

          <div class="shrinked">
            <h2><i class="icon-fish" />Mes poissons</h2>
            <div class="distribution">
              <div class="not-enough-data" v-if="caughtSpeciesDistribution.length == 0">
                <span>Pas assez de données</span>
              </div>
              <div v-for="(f, index) in orderedCaughtSpeciesDistribution()"
                  v-bind:key="f.species.id"
                  class="distribution-row">
                <div class="distribution-row-data">
                  <div class="species">
                    {{f.species.name}}
                  </div>
                  <div class="percent">
                    {{f.percent}} %
                  </div>
                </div>
                <div class="distribution-row-bar">
                  <div class="distribution-row-bar-filled"
                      v-bind:class="index % 2 == 0 ? 'even' : 'odd'"
                      v-bind:style="'width: ' + f.percent + '%;'"></div>
                </div>
              </div>
            </div>
          </div>

          <div class="shrinked">
            <h2><i class="icon-fishing" />Moyenne des captures</h2>
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

          <div class="shrinked">
            <h2><i class="icon-size" />Top 5 tailles</h2>
          </div>
          <div class="not-enough-data" v-if="topBySize.length == 0">
            <span>Pas assez de données</span>
          </div>
          <div class="scroll">
            <div class="item"
                 v-bind:class="((topSizeSelected && t == topSizeSelected) ? 'selected':'')"
                 v-for="t in topBySize"
                 v-bind:key="'size-' + t.species.id"
                 v-on:click="selectTopSize(t)">
              {{t.species.name}}
            </div>
          </div>
          <div class="dashboard-top-catchs catch-preview-list-scrollable"
               v-if="topSizeSelected">
            <CatchPreviewList v-bind:modifiable="false"
                              v-bind:reverse="false"
                              lakeId=""
                              v-bind:catchs="topSizeSelected.catchs"
                              v-on:openCatchFromId="openCatch($event)"
                              bottomMode="index"/>
          </div>

          <div class="shrinked">
            <h2><i class="icon-weight" />Top 5 poids</h2>
          </div>
          <div class="not-enough-data" v-if="topByWeight.length == 0">
            <span>Pas assez de données</span>
          </div>
          <div class="scroll">
            <div class="item"
                 v-bind:class="((topWeightSelected && t == topWeightSelected) ? 'selected':'')"
                 v-for="t in topByWeight"
                 v-bind:key="'weight-' + t.species.id"
                 v-on:click="selectTopWeight(t)">
              {{t.species.name}}
            </div>
          </div>
          <div class="dashboard-top-catchs catch-preview-list-scrollable"
               v-if="topWeightSelected">
            <CatchPreviewList v-bind:modifiable="false"
                              v-bind:reverse="false"
                              lakeId=""
                              v-bind:catchs="topWeightSelected.catchs"
                              v-on:openCatchFromId="openCatch($event)"
                              metaMode="weight"
                              bottomMode="index"/>
          </div>

        </div>

      </div>
      <RunningOverlay v-if="hasRunningTrip"/>
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

import DashboardService from '@/services/DashboardService';
import TripsService from '@/services/TripsService';
import {Dashboard, SpeciesWithAlias, DashboardLastTrip, CatchBean} from '@/pojos/BackendPojos';
import {DashboardAndSpecies} from '@/services/DashboardService';

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../router';

import moment from 'moment';

export class DistributionEntry {
    constructor (
        public species:SpeciesWithAlias,
        public percent:number
        ) {
    }
}

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
    CatchPreviewList
  }
})
export default class DashboardView extends Vue {

  speciesIndex:{ [index: string]: SpeciesWithAlias } = {};
  months = ["Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc"];

  ready:boolean = false;

  caughtSpeciesDistribution:DistributionEntry[] = [];
  averageCatchsPerTripRounded:number = 0;
  averageCatchsPerTrip:number = 0;
  latestTrips:DashboardLastTrip[] = [];
  emptylatestTrips:any[] = [];
  maxCatchsCount:number = 100;
  topBySize:TopEntry[] = [];
  topSizeSelected:TopEntry | null = null;
  topByWeight:TopEntry[] = [];
  topWeightSelected:TopEntry | null = null;

  // On a besoin de maintenir un index de capture -> sortie
  catchToTripId:{ [index: string]: string } = {};

  hasRunningTrip:boolean = false;

  constructor() {
    super();
  }

  mounted() {
    DashboardService.loadDashboard()
      .then(this.loaded);
    TripsService.hasRunningTrip()
      .then((result:boolean) => this.hasRunningTrip = result);
  }

  loaded(data:DashboardAndSpecies) {
    data.species.forEach((species) => {
      this.speciesIndex[species.id] = species;
    });

    let distribution = data.dashboard.caughtSpeciesDistribution;
    Object.keys(distribution)
      .forEach((speciesId) => {
        let species:SpeciesWithAlias = this.speciesIndex[speciesId];
        let percent:number = Math.round(distribution[speciesId]);
        let entry:DistributionEntry = new DistributionEntry(species, percent);
        this.caughtSpeciesDistribution.push(entry);
    });

    this.averageCatchsPerTrip = data.dashboard.averageCatchsPerTrip || 0;
    this.averageCatchsPerTripRounded = Math.round(10 * this.averageCatchsPerTrip) / 10;

    this.maxCatchsCount = 0;
    data.dashboard.latestTripsCatchs.forEach((trip) => {
      this.latestTrips.push(trip);
      if (trip.catchsCount > this.maxCatchsCount) {
        this.maxCatchsCount = trip.catchsCount;
      }
    });
    while ((this.latestTrips.length + this.emptylatestTrips.length) < 9) {
      this.emptylatestTrips.push({});
    }
    if (this.maxCatchsCount == 0) {
      this.maxCatchsCount = 100;
    }

    this.topBySize = Vue.lodash.orderBy(this.parseTop(data.dashboard.topBySize), 'species.name');
    this.topByWeight = Vue.lodash.orderBy(this.parseTop(data.dashboard.topByWeight), 'species.name');

    if (this.topBySize && this.topBySize.length >= 1) {
      this.selectTopSize(this.topBySize[0]);
    }
    if (this.topByWeight && this.topByWeight.length >= 1) {
      this.selectTopWeight(this.topByWeight[0]);
    }

    this.ready = true;
  }

  parseTop(rawTop:{ [index: string]: CatchBean[] }):TopEntry[] {
    let result:TopEntry[] = [];
    let topSpecies:string[] = Object.keys(rawTop);
    topSpecies.forEach((speciesId) => {
      let species:SpeciesWithAlias = this.speciesIndex[speciesId];
      let catchs:CatchBean[] = [];
      let rawCatchs:any[] = rawTop[speciesId];
      rawCatchs.forEach((rawCatch:any) => {
        this.catchToTripId[rawCatch.id] = rawCatch.tripId;
        let aCatch:CatchBean = TripsService.backendCatchToCatchBean(moment(), rawCatch);
        catchs.push(aCatch);
      })
      let entry:TopEntry = new TopEntry(species, catchs);
      result.push(entry);
    });
    return result;
  }

  selectTopSize(top:TopEntry) {
    this.topSizeSelected = top;
  }

  selectTopWeight(top:TopEntry) {
    this.topWeightSelected = top;
  }

  openCatch(catchId:string) {
    router.push({name:'catch', params: {tripId: this.catchToTripId[catchId], catchId:catchId}});
  }

  orderedCaughtSpeciesDistribution() {
    return Vue.lodash.orderBy(this.caughtSpeciesDistribution, 'species.name');
  }

  openTrip(tripId:string) {
    router.push({name:'trip', params: {id: tripId}});
  }

  getDay(date:number) {
    return new Date(date).getDate();
  }

  getMonth(date:number) {
    let month = new Date(date).getMonth();
    let result = this.months[month];
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
      font-size: 18px;
      line-height: 25px;
      color: @pale-sky;
      text-align: center;
      margin-top: 30px;
    }
  }

  .pane-content-large {

    overflow: auto;

    text-align:center;

    background-color: @white-smoke;
    padding-top: 30px;

    color: @gunmetal;

    .shrinked {
      padding-left: 30px;
      padding-right: 30px;
    }

    h2 {

      i {
        color: @pelorous;
        margin-right: 10px;
      }

      font-style: normal;
      font-weight: normal;
      font-size: 22px;
      line-height: 30px;
      text-align: left;
    }

    .distribution {
      width: 100%;
      display: flex;
      flex-direction: column;

      .distribution-row {

        margin-bottom: 10px;

        .distribution-row-data {
          display: flex;
          flex-direction: row;
          justify-content: space-between;

          font-size: 12px;
          line-height: 16px;
          height: 16px;

          .species {
            color: @gunmetal;
          }

          .percent {
            font-weight: bold;
            color: @pelorous;
          }
        }

        .distribution-row-bar {
          position: relative;
          margin-top: 4px;
          height: 14px;
          border-radius: 7px;
          background: @solitude;

          .distribution-row-bar-filled {
            position: absolute;
            height: 14px;
            border-radius: 7px;

            &.even {
              background: @pelorous;
            }

            &.odd {
              background: @summer-sky;
            }
          }

        }

      }

    }

    .average-header {
      height: 30px;
      line-height: 30px;
      font-weight: bold;
      font-size: 14px;
      color: @terra-cotta;

      display: flex;
      flex-direction: row;

      .count {
        width: 30px;
        border-radius: 15px;
        background-color: @terra-cotta;
        color: @white;
        margin-right: 10px;
      }
    }

    .average {
      position: relative;
      margin-top: 10px;
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
          font-size: 12px;
          color: @pelorous;
        }

        .average-row-bar {
          position: relative;
          margin-top: 5px;
          margin-bottom: 5px;
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
          font-size: 12px;
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
      margin-bottom: 20px;

    }

    .scroll {
      display: flex;
      flex-direction: row;
      justify-content: flex-start;
      margin-bottom: 10px;

      padding-left: 30px;
      padding-right: 30px;

      overflow:auto;

      div.item {
        margin-right: 20px;
        color: @pale-sky;
        white-space: nowrap;
      }
      div.item.selected {
        color: @gunmetal;
        border-bottom: 2px solid @pelorous;
      }
    }
  }

}

</style>
