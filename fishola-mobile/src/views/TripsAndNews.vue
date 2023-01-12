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
          <h1 class="hide-on-mobile">Accueil</h1>
          <div class="trips-and-news-tab">
            <div
              class="trips-or-news"
              :class="showNews ? '' : 'selected'"
              @click="showNews = false"
            >
              Mes Sorties
            </div>
            <div
              class="trips-or-news"
              :class="showNews ? 'selected' : ''"
              @click="showNewsTab"
            >
              <span> Communications </span>
              <div class="news-badge" v-if="unreadNewsCount > 0">
                {{ unreadNewsCount }}
              </div>
            </div>
          </div>
          <MyTrips v-if="!showNews" />
          <NewsView :news="news" v-else />
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
import { Component, Vue } from "vue-property-decorator";
import Helpers from "../services/Helpers";
import DocumentationService from "../services/DocumentationService";
import ProfileService from "../services/ProfileService";
import { News } from "@/pojos/BackendPojos";

@Component({
  components: {
    FisholaHeader,
    MyTrips,
    FisholaFooter,
    NewsView,
  },
})
export default class TripsAndNews extends Vue {
  unreadNewsCount = 0;
  showNews = false;
  news: News[] = [];

  mounted() {
    this.updateUnreadNewsCount();
  }

  async updateUnreadNewsCount() {
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
  }

  async showNewsTab() {
    this.showNews = true;
    if (this.unreadNewsCount > 0) {
      let profile = await ProfileService.getProfile();
      profile.lastNewsSeenDate = new Date();
      ProfileService.saveProfile(profile);
      this.updateUnreadNewsCount();
    }
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
