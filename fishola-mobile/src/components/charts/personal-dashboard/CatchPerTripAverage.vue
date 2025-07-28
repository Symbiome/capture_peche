<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
  <div class="section shrinked">
    <h2><i class="icon-fishing" />Mes captures</h2>

    <div class="not-enough-data" v-if="latestTrips.length === 0">
      <span>Pas assez de données</span>
    </div>

    <div class="average-header" v-if="latestTrips.length > 0">
      <div class="count">{{ averageCatchsPerTripRounded }}</div>
      captures en moyenne / sortie
    </div>

    <div class="average">
      <div
        v-for="(f, index) in latestTrips"
        :key="f.tripId"
        class="average-column"
        @click="openTrip(f.tripId)"
      >
        <div class="count">{{ f.catchsCount }}</div>
        <div class="average-row-bar">
          <div
            v-if="f.catchsCount > 0"
            class="average-row-bar-filled"
            :class="index % 2 === 0 ? 'even' : 'odd'"
            :style="{ height: (f.catchsCount * 100) / maxCatchsCount + '%' }"
          ></div>
        </div>
        <div class="date">
          <div class="day">{{ getDay(f.day) }}</div>
          <div class="month">{{ getMonth(f.day) }}</div>
          <div class="year">{{ getYear(f.day) }}</div>
        </div>
      </div>

      <div
        v-for="(f, index) in emptylatestTrips"
        :key="'empty-' + index"
        class="average-column"
      >
        <div class="count">-</div>
        <div class="average-row-bar"></div>
        <div class="date"><div class="day">-</div></div>
      </div>

      <div
        class="average-threshold"
        v-if="latestTrips.length > 0"
        :style="{
          bottom: 54 + (averageCatchsPerTrip * 150) / maxCatchsCount + 'px',
        }"
      ></div>
    </div>
  </div>
</template>

<script lang="ts">
import { DashboardLastTrip } from '@/pojos/BackendPojos';
import { RouterUtils } from '@/router/RouterUtils';
import { DashboardAndSpecies } from '@/services/DashboardService';
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';

@Component
export default class AverageCatchChart extends Vue {
  @Prop() dashboardData: DashboardAndSpecies;

  latestTrips: DashboardLastTrip[] = [];
  emptylatestTrips: any[] = [];
  maxCatchsCount: number = 100;
  averageCatchsPerTripRounded: number = 0;
  averageCatchsPerTrip: number = 0;
  
  @Watch("dashboardData")
  dashboardDataChanged(): void {
    this.averageCatchsPerTripRounded = 0;
    this.averageCatchsPerTrip = 0;
    this.latestTrips = [];
    this.emptylatestTrips = [];
    this.maxCatchsCount = 0;
    this.averageCatchsPerTrip =
      this.dashboardData.dashboard.averageCatchsPerTrip || 0;
    this.averageCatchsPerTripRounded =
      Math.round(10 * this.averageCatchsPerTrip) / 10;

    this.maxCatchsCount = 1;
    this.dashboardData.dashboard.latestTripsCatchs.forEach((trip) => {
      this.latestTrips.push(trip);
      if (trip.catchsCount > this.maxCatchsCount) {
        this.maxCatchsCount = trip.catchsCount;
      }
    });
    this.maxCatchsCount = Math.max(
      this.maxCatchsCount,
      this.averageCatchsPerTripRounded
    );
    while (this.latestTrips.length + this.emptylatestTrips.length < 9) {
      this.emptylatestTrips.push({});
    }
  }


  openTrip(tripId: string) {
    RouterUtils.pushRouteNoDuplicate(this.$router, {
      name: "trip",
      params: { id: tripId },
    });
  }

  getDay(dateStr: string) {
    return new Date(dateStr).getDate();
  }

  getMonth(dateStr: string) {
    return new Date(dateStr).toLocaleString('fr-FR', { month: 'short' });
  }

  getYear(dateStr: string) {
    return new Date(dateStr).getFullYear();
  }
}
</script>

<style scoped lang="less">
@import "../../less/main";

</style>
