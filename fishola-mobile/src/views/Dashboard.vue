<template>
  <div class="dashboard page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page dashboard-page">
      <div class="pane pane-only">
        <h1>Tableau de bord</h1>

        <div class="pane-content">
          <h2><i class="icon-fish" />Mes poissons</h2>
          <div class="distribution">
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

          <h2><i class="icon-fishing" />Moyenne des captures</h2>
          <div class="average-header">
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
            <div class="average-threshold" v-bind:style="'bottom: ' + (54+(averageCatchsPerTrip * 150 / maxCatchsCount)) + 'px;'"></div>
          </div>

          <h2><i class="icon-size" />Top 5 tailles</h2>
          <div class="scroll">
            <div class="item selected">Perche</div>
            <div class="item">Brochet</div>
            <div class="item">Truite</div>
            <div class="item">Corégone</div>
            <div class="item">Omble_chevalier</div>
            <div class="item">Silure</div>
          </div>
          <div class="placeholder">
            Top 5 tailles
          </div>

          <h2><i class="icon-weight" />Top 5 poids</h2>
          <div class="scroll">
            <div class="item selected">Perche</div>
            <div class="item">Brochet</div>
            <div class="item">Truite</div>
            <div class="item">Corégone</div>
            <div class="item">Omble_chevalier</div>
            <div class="item">Silure</div>
          </div>
          <div class="placeholder">
            Top 5 poids
          </div>
        </div>

      </div>
    </div>
    <FisholaFooter shortcuts="logout,dashboard,home"
                   selected="dashboard" />
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue'

import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import DashboardService from '@/services/DashboardService';
import {Dashboard, SpeciesWithAlias, DashboardLastTrip} from '@/pojos/BackendPojos';
import {DashboardAndSpecies} from '@/services/DashboardService';

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../router';

export class DistributionEntry {
    constructor (
        public species:SpeciesWithAlias,
        public percent:number
        ) {
    }
}

@Component({
  components: {
    FisholaHeader,
    FisholaFooter
  }
})
export default class DashboardVue extends Vue {

  speciesIndex:{ [index: string]: SpeciesWithAlias } = {};
  months = ["Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc"];

  caughtSpeciesDistribution:DistributionEntry[] = [];
  averageCatchsPerTripRounded:number = 0;
  averageCatchsPerTrip:number = 0;
  latestTrips:DashboardLastTrip[] = [];
  maxCatchsCount:number = 100;

  constructor() {
    super();
  }

  mounted() {
    DashboardService.loadDashboard()
      .then(this.loaded);
  }

  loaded(data:DashboardAndSpecies) {
    console.log("LOADED!", data);
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
    if (this.maxCatchsCount == 0) {
      this.maxCatchsCount = 100;
    }
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

  text-align:center;

  .pane-content {

    overflow: auto;

    text-align:center;

    background-color: @white-smoke;
    padding-left: 30px;
    padding-right: 30px;
    padding-top: 30px;

    color: @gunmetal;

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
        // background-color: @terra-cotta;
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

    .placeholder {

      display: flex;
      flex-direction: column;
      justify-content: center;

      color: @white;
      background-color: @cyprus;
      opacity: 0.8;
      border-radius: 8px;
      
      height: 200px;
    }

    .scroll {
      display: flex;
      flex-direction: row;
      justify-content: flex-start;
      margin-bottom: 10px;

      overflow:auto;

      div.item {
        margin-right: 20px;
        color: @pale-sky;
      }
      div.item.selected {
        color: @gunmetal;
        border-bottom: 3px solid @pelorous;
      }
    }
  }

}

</style>
