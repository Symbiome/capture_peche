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
            <LakeAndYearSelection 
                :showYears="false"
                @lake="selectedLakeUUID = $event"
              />
          </h1>

          <div class="main-tabs">
            <div class="tab" :class="visualizationMode === 'news' ? '' : 'selected'"
              @click="changeVisualizationMode('social')">
              Autour de moi
            </div>
            <div class="tab" :class="visualizationMode === 'news' ? 'selected' : ''" @click="showNewsTab">
              <span> Communications </span>
              <div class="news-badge" v-if="unreadNewsCountForCurrentLake > 0">
                {{ unreadNewsCountForCurrentLake }}
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
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import Helpers from "../services/Helpers";
import DocumentationService from "../services/DocumentationService";
import ProfileService from "../services/ProfileService";
import { NewsBean } from "@/pojos/BackendPojos";
import SocialView from "./Social.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";
import TripsService from "@/services/TripsService";
import LakeAndYearSelection from "@/components/common/LakeAndYearSelection.vue";
@Component({
  components: {
    MyTrips,
    FisholaHeader,
    SocialView,
    NewsView,
    FisholaFooter,
    RunningOverlay,
    LakeAndYearSelection
  },
})
export default class SocialAndNewsView extends Vue {
  @Prop()
  visualizationMode: string;

  unreadNewsCountForCurrentLake = 0;
  unreadNewsCountPerLake: Map<string, number> = new Map();
  hasRunningTrip = false;
  selectedLakeUUID = "";
  news: NewsBean[] = [];

  created() {
    TripsService.hasRunningTrip().then(
      (result: boolean) => (this.hasRunningTrip = result)
    );
  }


  @Watch("selectedLakeUUID")
  async updateUnreadNewsCount() {
    try {
      this.unreadNewsCountForCurrentLake = 0;
      this.news = await DocumentationService.getNews(this.selectedLakeUUID);

      // Get last news seen date from local storage and profile
      let lastNewsSeenDateForLake = undefined;
      const lastLocalStorageDateString = localStorage.getItem("last_news_seen_" + this.selectedLakeUUID);
      if (lastLocalStorageDateString) {
        lastNewsSeenDateForLake = new Date(lastLocalStorageDateString);
      }
      let lastProfileSeenDate = undefined;
      let profile = await ProfileService.getProfile();
      if (profile.lastNewsSeenDate) {
        lastProfileSeenDate = Helpers.parseLocalDateTime(
          // @ts-ignore
          profile.lastNewsSeenDate
        );
        if (!lastNewsSeenDateForLake) {
          lastNewsSeenDateForLake = lastProfileSeenDate;
        }
      }

      // Compute unreadNewsCount
      this.unreadNewsCountForCurrentLake = this.news.filter((n) => {
        // @ts-ignore
        const newsDate =  Helpers.parseLocalDateTime(n.datePublicationDebut);

        // Make sure a national news is not marked unread once read (not mattering the lake)
        if (n.isNational) {
          return (lastNewsSeenDateForLake ? newsDate > lastNewsSeenDateForLake : true) 
          && (lastProfileSeenDate ? newsDate > lastProfileSeenDate : true)
        }
        return (lastNewsSeenDateForLake ? newsDate > lastNewsSeenDateForLake : true);
      }).length;
      this.unreadNewsCountPerLake.set(this.selectedLakeUUID, this.unreadNewsCountForCurrentLake);
      
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
    localStorage.setItem("last_news_seen_" + this.selectedLakeUUID, new Date().toISOString());
    if (this.unreadNewsCountPerLake.get(this.selectedLakeUUID) ?? 0 > 0) {
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
