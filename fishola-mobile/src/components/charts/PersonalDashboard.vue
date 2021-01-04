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
  <div>
    <div class="section shrinked">
      <h2><i class="icon-fish" />Mes poissons</h2>
      <DistributionChart :distribution="caughtSpeciesDistribution"></DistributionChart>
    </div>

    <div class="section shrinked">
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

    <div class="section">
      <div class="shrinked">
        <h2><i class="icon-size" />Historique des tailles (cm)</h2>
      </div>
      <div class="not-enough-data" v-if="monthlySizesOptions.length == 0">
        <span>Pas assez de données</span>
      </div>
      <OptionsList :items="monthlySizesOptions"
                    v-if="monthlySizesOptions.length > 0"
                    v-on:item-selected="onMonthlySizeSelected"></OptionsList>
      <div class="shrinked" v-if="monthlySizes">
        <HistogramChart :values="monthlySizes"
                        :orderedMonths="orderedMonths"></HistogramChart>
      </div>
    </div>

    <div class="section">
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

    <div class="section">
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
</template>

<script lang="ts">

import CatchPreviewList from '@/components/trip/CatchPreviewList.vue';

import OptionsList from '@/components/common/OptionsList.vue';

import DistributionChart from '@/components/charts/DistributionChart.vue';
import HistogramChart from '@/components/charts/HistogramChart.vue';

import TripsService from '@/services/TripsService';
import {Dashboard, SpeciesWithAlias, DashboardLastTrip, CatchBean, Month} from '@/pojos/BackendPojos';
import DistributionEntry from '@/pojos/DistributionEntry';
import OptionItem from '@/pojos/OptionItem';
import {DashboardAndSpecies} from '@/services/DashboardService';

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

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
    DistributionChart,
    OptionsList,
    HistogramChart,
    CatchPreviewList
  }
})
export default class PersonalDashboard extends Vue {

  speciesIndex:{ [index: string]: SpeciesWithAlias } = {};
  months = ["Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc"];

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

  @Prop() dashboardData:DashboardAndSpecies;

  constructor() {
    super();
  }

  created() {
    const speciesAliases = this.dashboardData.dashboard.speciesAliases;
    this.dashboardData.species.forEach((species) => {
      this.speciesIndex[species.id] = species;

      const aliases = speciesAliases[species.id];
      if (aliases) {
        species.alias = aliases.join(' ou ');
      }
    });

    const speciesCount = this.dashboardData.dashboard.caughtSpeciesCount;
    const distribution = this.dashboardData.dashboard.caughtSpeciesDistribution;
    const releasedDistribution = this.dashboardData.dashboard.caughtAndReleasedSpeciesDistribution;
    Object.keys(distribution)
      .forEach((speciesId) => {
        const species:SpeciesWithAlias = this.speciesIndex[speciesId];
        const percent:number = Math.round(distribution[speciesId]);
        const releasedPercent:number = Math.round(releasedDistribution[speciesId]) | 0;
        const count:number = speciesCount[speciesId];
        const entry:DistributionEntry = new DistributionEntry(speciesId, species.name, percent, releasedPercent, count, species.alias);
        this.caughtSpeciesDistribution.push(entry);
    });

    this.averageCatchsPerTrip = this.dashboardData.dashboard.averageCatchsPerTrip || 0;
    this.averageCatchsPerTripRounded = Math.round(10 * this.averageCatchsPerTrip) / 10;

    this.maxCatchsCount = 1;
    this.dashboardData.dashboard.latestTripsCatchs.forEach((trip) => {
      this.latestTrips.push(trip);
      if (trip.catchsCount > this.maxCatchsCount) {
        this.maxCatchsCount = trip.catchsCount;
      }
    });
    this.maxCatchsCount = Math.max(this.maxCatchsCount, this.averageCatchsPerTripRounded);
    while ((this.latestTrips.length + this.emptylatestTrips.length) < 9) {
      this.emptylatestTrips.push({});
    }

    const topBySize:TopEntry[] = this.parseTop(this.dashboardData.dashboard.topBySize);
    topBySize.forEach((top) => {
      this.topBySizeOptions.push({
        id: top.species.id,
        name: top.species.name,
        alias: top.species.alias,
        whatever: top.catchs
      });
    });
    this.topBySizeOptions = Vue.lodash.orderBy(this.topBySizeOptions, 'name');

    const topByWeight:TopEntry[] = this.parseTop(this.dashboardData.dashboard.topByWeight);
    topByWeight.forEach((top) => {
      this.topByWeightOptions.push({
        id: top.species.id,
        name: top.species.name,
        alias: top.species.alias,
        whatever: top.catchs
      });
    });
    this.topByWeightOptions = Vue.lodash.orderBy(this.topByWeightOptions, 'name');

    this.orderedMonths = this.dashboardData.dashboard.orderedMonths;
    const monthlySizesSpecies:string[] = Object.keys(this.dashboardData.dashboard.monthlySizes);
    monthlySizesSpecies.forEach((speciesId) => {
      const species:SpeciesWithAlias = this.speciesIndex[speciesId];
      this.monthlySizesOptions.push({
        id: species.id,
        name: species.name,
        alias: species.alias,
        whatever: this.dashboardData.dashboard.monthlySizes[speciesId]
      });
    });
    this.monthlySizesOptions = Vue.lodash.orderBy(this.monthlySizesOptions, 'name');

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

@import "../../less/main";

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
      height: 60px;
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

</style>
