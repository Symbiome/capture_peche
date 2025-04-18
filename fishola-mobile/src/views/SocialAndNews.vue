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
  <div class="my-trips page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="page social-and-news-page">
      <div class="pane pane-only">
        <div class="pane-content large rounded no-scroll">
          <h1 class="no-margin-pane">Communauté
            <div class="selects-holder">
              <select placeholder="lake" v-model="selectedLakeUUID">
                <option v-for="lake in lakes" :value="lake.id" :key="lake.uuid">
                  {{ lake.name }}
                </option>
              </select>
            </div>
          </h1>

          <div class="main-tabs">
            <div class="tab" :class="visualizationMode === 'news' ? '' : 'selected'"
              @click="changeVisualizationMode('social')">
              Autour de moi
            </div>
            <div class="tab" :class="visualizationMode === 'news' ? 'selected' : ''" @click="showNewsTab">
              <span> Communications </span>
              <div class="news-badge">
                {{ unreadNewsCount }}
              </div>
            </div>
          </div>
          <div class="padding-content">
            <SocialView v-if="visualizationMode === 'social'" :lakeId="selectedLakeUUID" />
            <NewsView :news="news" v-else />
          </div>
        </div>
      </div>
      <div class="bottom">
        <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip" />
        <FisholaFooter shortcuts="logout,dashboard,home" selected="dashboard" />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import MyTrips from "@/views/MyTrips.vue";
import NewsView from "@/views/News.vue";
import { Component, Prop, Vue } from "vue-property-decorator";
import Helpers from "../services/Helpers";
import DocumentationService from "../services/DocumentationService";
import ProfileService from "../services/ProfileService";
import { Lake, News } from "@/pojos/BackendPojos";
import SocialView from "./Social.vue";
import ReferentialService from "@/services/ReferentialService";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";
import TripsService from "@/services/TripsService";

@Component({
  components: {
    MyTrips,
    FisholaHeader,
    SocialView,
    NewsView,
    FisholaFooter,
    RunningOverlay
  },
})
export default class SocialAndNewsView extends Vue {
  @Prop()
  visualizationMode: string;

  unreadNewsCount = 0;
  hasRunningTrip = false;
  selectedLakeUUID = "";
  news: News[] = [];
  lakes: Lake[] = [];

  created() {
    if (localStorage && localStorage.latestSelectedLakeUUID && localStorage.latestSelectedLakeUUID != "all") {
      this.selectedLakeUUID = localStorage.latestSelectedLakeUUID
    }
    this.loadLakes();
    TripsService.hasRunningTrip().then(
      (result: boolean) => (this.hasRunningTrip = result)
    );
  }

  async loadLakes(): Promise<void> {
    this.lakes = [];
    try {
      const allLakes = await ReferentialService.getLakes();
      this.lakes = this.lakes.concat(allLakes);
    } catch (e) {
      // Silent catch, no more lakes will be added
    }
  }

  mounted() {
    this.updateUnreadNewsCount();
  }

  async updateUnreadNewsCount() {
    try {
      this.news = await DocumentationService.getNews();
      let profile = await ProfileService.getProfile();
      if (profile.lastNewsSeenDate) {
        const lastSeenDate = Helpers.parseLocalDateTime(
          // @ts-ignore
          profile.lastNewsSeenDate
        );

        this.unreadNewsCount = this.news.filter((n) => {
          return (
            // @ts-ignore
            Helpers.parseLocalDateTime(n.datePublicationDebut) > lastSeenDate
          );
        }).length;
      }
    } catch (e) {
      // News section will be left empty
    }
  }

  changeVisualizationMode(newMode: string) {
    if (this.visualizationMode !== newMode) {
      this.$router.push({ params: { visualizationMode: newMode } });
    }
  }

  async showNewsTab() {
    this.changeVisualizationMode('news');
    if (this.unreadNewsCount > 0) {
      try {
        let profile = await ProfileService.getProfile();
        profile.lastNewsSeenDate = new Date();
        ProfileService.saveProfile(profile);
        this.updateUnreadNewsCount();
      } catch (e) {
        // Unread news count won't be updated
      }
    }
  }
}
</script>

<style scope lang="less">
@import "../less/main";

.h1-with-selects {
  @media screen and (max-width: @desktop-min-width) {
    margin-top: -20px !important;
    margin-bottom: 20px !important;
  }
}
.tab {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: @margin-small;

  &.selected {
    font-weight: bold;
  }
}

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

@media screen and (max-width: @desktop-min-width) {
  .trips-and-news-tab {
    padding-top: 20px;
    margin-top: 0px;
  }
        .social-and-news-page {
          .pane {
            h1 {
              height: 80px;
              margin-top: 0px;
            }
          }
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
    
        @media screen and (min-width: @desktop-min-width) {
          .social-and-news-page {
            .pane {
              h1 {
                display: flex;
                flex-direction: row;
                justify-content: flex-start;
                align-items: center;
              }
            }
          }
  
}</style>
