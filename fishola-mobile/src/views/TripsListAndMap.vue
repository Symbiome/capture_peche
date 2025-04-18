<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2022 INRAE - UMR CARRTEL
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
  <div class="my-trips page-with-header shifted-background">
    <FisholaHeader />
    <div class="page my-trips-page">
      <div class="pane pane-only">
        <div class="pane-content large rounded no-scroll">
          <h1 class="no-margin-pane">Mes Sorties</h1>
          <div class="main-tabs">
            <div class="tab" :class="visualizationMode === 'list' ? 'selected' : ''"
              @click="changeVisualizationMode('list')">
              Liste
            </div>
            <div class="tab" :class="visualizationMode !== 'list' ? 'selected' : ''"
              @click="changeVisualizationMode('map')">
              Carte
            </div>
          </div>
          <div class="padding-content">
            <MyTrips v-if="visualizationMode === 'list'" :hasRunningTrip="hasRunningTrip" />
            <MyTripsMapView :visible="visualizationMode == 'map'" />
          </div>
          <div class="bottom">
            <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip && visualizationMode === 'list'" />
            <FisholaFooter shortcuts="logout,dashboard,home" selected="dashboard" v-if="visualizationMode === 'list'" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";
import MyTrips from "@/views/MyTrips.vue";
import NewsView from "@/views/News.vue";
import { Component, Prop, Vue } from "vue-property-decorator";
import MyTripsMapView from "@/components/my-trips/MyTripsMap.vue";
import TripsService from "@/services/TripsService";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";

@Component({
  components: {
    FisholaHeader,
    MyTrips,
    FisholaFooter,
    MyTripsMapView,
    NewsView,
    RunningOverlay
  },
})
export default class TripsListAndMapView extends Vue {
  @Prop({ default: "list" })
  visualizationMode: string;
  hasRunningTrip = false;

  mounted() {
    TripsService.hasRunningTrip().then(
      (result: boolean) => (this.hasRunningTrip = result)
    );
  }

  changeVisualizationMode(newMode: string) {
    if (this.visualizationMode !== newMode) {
      this.$router.push({ params: { visualizationMode: newMode } });
    }
  }

}
</script>

<style scope lang="less">
@import "../less/main";

.news-badge {
  width: 30px;
  height: 25px;
  font-size: 16px;
  border-radius: 15px;
  background-color: @terra-cotta;
  color: @white;
  margin-right: @margin-small;
}
</style>
