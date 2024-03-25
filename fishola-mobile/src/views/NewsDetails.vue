<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
  <div class="news-details page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="page news-page">
      <div class="pane pane-only">
        <div class="pane-content rounded">
          <h1 class="news-details-title">
            <i
              class="icon-news-back icon-arrow hide-on-mobile"
              @click="goBack"
            />
            {{ news.name }}
          </h1>

          <span class="publication-date-details">
            <i class="icon-send" /><span
              >Publié le
              {{ formatPublicationDate(news.datePublicationDebut) }}</span
            >
          </span>
          <div class="miniature-pic">
            <img :src="getMiniatureURl(news)" alt="image de l'actualité"/>
          </div>

          <div class="news-details-content" v-html="news.content" />
          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter shortcuts="back" />
  </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import documentationService from "@/services/DocumentationService";
import { News } from "@/pojos/BackendPojos";

import { Component, Prop, Vue } from "vue-property-decorator";
import Constants from "../services/Constants";
import Helpers from "../services/Helpers";
import router from "@/router";

@Component({
  components: {
    FisholaHeader,
    FisholaFooter,
  },
})
export default class NewsDetailsView extends Vue {
  @Prop() newsId: string;
  news: News = {
    id: "",
    name: "",
    content: "",
    datePublicationDebut: new Date(),
    datePublicationFin: new Date(),
    dateNotificationSent: new Date(),
    miniatureId: "",
  };

  mounted() {
    this.loadSingleNews();
  }

  async loadSingleNews(): Promise<void> {
    try {
      this.news = await documentationService.getSingleNews(this.newsId);
    } catch (e) {
      // news will be left empty
    }
  }
  getMiniatureURl(news: News) {
    if (news.miniatureId) {
      return Constants.apiUrl("/v1/news-picture/" + news.miniatureId);
    } else {
      return "img/logo-small.svg";
    }
  }

  formatPublicationDate(date: number[]) {
    if (date) {
      const toDate = Helpers.parseLocalDate(date);
      if (toDate) {
        return Helpers.formatToDate(toDate);
      }
    }
    return Helpers.formatToDate(new Date());
  }

  treatHTMLContent(htmlContent: string) {
    return htmlContent.replace(/<[^>]+>/g, "");
  }

  goBack() {
    router.go(-1);
  }
}
</script>

<style lang="less">
@import "../less/main";

.news-details-title {
  display: flex;
  align-items: center;
  gap: 25px;
  .icon-news-back {
    cursor: pointer;
    display: inline-block;
    font-size: 22px;
    transform: rotate(180deg);
  }
}
.publication-date-details {
  margin-top: -20px;
  display: flex;
  align-items: center;
  gap: 15px;
  color: @pale-sky;
  text-transform: uppercase;
}

.miniature-pic {
  padding-top: 30px;
  img {
    max-height: 30vh;
    max-width: 60vw;
    border-radius: 20px;
    object-fit: fill;
  }
}

.news-details-content {
  h1 {
    font-size: 30px !important;
  }
  img {
    max-height: 30vh;
    max-width: 60vw;
    border-radius: 20px;
    object-fit: fill;
  }
}

@media screen and (min-width: 760px) and (max-width: 1200px) {
  .news-details-title {
    font-size: 20px !important;
  }
  .news-details-content {
    h1 {
      font-size: 16px !important;
    }
  }
}

@media screen and (max-width: @desktop-min-width) {
  .news-details-content {
    text-align: center;
    h1 {
      font-size: 20px !important;
    }
  }
  .news-page {
    text-align: center;
  }

  .news-details-title {
    justify-content: center;
  }
  .publication-date-details {
    justify-content: center;
  }
}
</style>
