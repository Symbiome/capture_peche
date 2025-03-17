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
        <div class="pane-content rounded">
          <h1 class="hide-on-mobile">Mes Sorties</h1>
          <div class="trips-and-news-tab">
            <div class="trips-or-news" :class="visualizationMode === 'list' ? 'selected' : ''"
              @click="changeVisualizationMode('list')">
              Liste
            </div>
            <div class="trips-or-news" :class="visualizationMode !== 'list' ? 'selected' : ''"
              @click="changeVisualizationMode('map')">
              Carte
            </div>
          </div>
          <MyTrips v-if="visualizationMode === 'list'" />
          <MyTripsMapView :visible="visualizationMode == 'map'" />
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

@Component({
  components: {
    FisholaHeader,
    MyTrips,
    FisholaFooter,
    MyTripsMapView,
    NewsView,
  },
})
export default class TripsListAndMapView extends Vue {
  @Prop({ default: "list" })
  visualizationMode: string;

  changeVisualizationMode(newMode: string) {
    this.$router.push({ params: { visualizationMode: newMode } });
  }
}
</script>

<style scope lang="less">
@import "../less/main";

.trips-and-news-tab {
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: space-evenly;
  margin-top: -30px;

  .trips-or-news {
    width: 100%;
    display: flex;
    justify-content: center;
    text-align: center;
    gap: 5px;
    color: @pale-sky;
    padding-bottom: 5px;
    padding-left: 5px;
    padding-right: 5px;
    cursor: pointer;

    &.selected {
      color: @gunmetal;
      border-bottom: 2px solid @pelorous;
    }
  }
}

@media screen and (max-width: 760px) {
  .trips-and-news-tab {
    padding-top: 20px;
    margin-top: 0px;
  }
}

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
