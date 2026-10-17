<!--
  #%L
  Carnet de pêche :: Mobile
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
  <div class="page-with-header shifted-background">
    <CarnetHeader />
    <div class="page">
      <div class="pane pane-only">
        <div class="pane-content rounded">
          <h1 class="hide-on-mobile">Accueil</h1>
          <div class="trips-and-news-tab">
            <div
              class="trips-or-news"
              :class="showNews ? '' : 'selected'"
              @click="goPresentation"
            >
              Présentation
            </div>
            <div
              class="trips-or-news"
              :class="showNews ? 'selected' : ''"
              @click="goNews"
            >
              <span> Communications </span>
            </div>
          </div>
          <div class="about-container" v-if="!showNews">
            <About :wrappedInTab="true" />
          </div>
          <NewsView :news="news" v-else />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import CarnetHeader from "@/components/layout/CarnetHeader.vue";
import CarnetFooter from "@/components/layout/CarnetFooter.vue";
import MyTrips from "@/views/MyTrips.vue";
import About from "@/views/About.vue";
import NewsView from "@/views/News.vue";
import { Component, Prop, Vue, Watch } from "vue-facing-decorator";
import DocumentationService from "../services/DocumentationService";
import { NewsBean } from "@/pojos/BackendPojos";
import { RouterUtils } from "@/router/RouterUtils";

@Component({
  components: {
    CarnetHeader,
    MyTrips,
    CarnetFooter,
    NewsView,
    About,
  },
})
export default class OfflineHome extends Vue {
  @Prop({ default: "presentation" }) defaultTab: string;
  showNews = false;
  news: NewsBean[] = [];
  mounted(): void {
    Prop;
    this.loadNews();
  }

  @Watch("defaultTab")
  async loadNews() {
    try {
      this.news = await DocumentationService.getNews();
      this.showNews = this.defaultTab == "news";
    } catch (e) {
      // News section will be left empty
    }
  }

  async showNewsTab() {
    this.showNews = true;
  }

  goNews() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/offline-home/news");
  }

  goPresentation() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/offline-home/presentation");
  }
}
</script>

<style scoped lang="less">
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
    padding-left: 20px;
    padding-right: 20px;
    cursor: pointer;

    &.selected {
      color: @gunmetal;
      border-bottom: 2px solid @accent-text;
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
  border-radius: 15px;
  background-color: @terra-cotta;
  color: @white;
  margin-right: @margin-small;
}
</style>
