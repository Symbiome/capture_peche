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
  <div class="news">
    <div v-if="!news || news.length == 0" class="empty">
      Pas de communications autour de Fishola pour le moment, revenez consultez
      cette page prochainement !
    </div>

    <div
      class="news-holder"
      v-for="doc in news"
      v-bind:key="doc.id"
      @click="showNewsDetails(doc.id)"
    >
      <div class="news-row">
        <div class="left-pic">
          <img :src="getMiniatureURl(doc)" class="news-pic" alt="image de l'actualité"/>
        </div>
        <div class="right-content">
          <strong>{{ doc.name }}</strong> <br />
          <span class="publication-date">
            <i class="icon-send" />Publié le
            {{ formatPublicationDate(doc.datePublicationDebut) }}
          </span>
          <div class="news-content only-on-big-screen">
            <i class="icon-files" />
            {{ treatHTMLContent(doc.content) }}
          </div>
          <div class="only-on-big-screen">
            <div class="read-more">Lire la suite <i class="icon-arrow" /></div>
          </div>
        </div>
      </div>
      <div class="news-content only-on-small-screen">
        <i class="icon-files" />
        {{ treatHTMLContent(doc.content) }}
      </div>

      <div class="only-on-small-screen">
        <div class="read-more">Lire la suite <i class="icon-arrow" /></div>
      </div>
    </div>
    <div class="bottom">
      <FisholaFooter
        shortcuts="logout,dashboard,home"
        selected="documentation"
      />
    </div>
  </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";
import { NewsBean } from "@/pojos/BackendPojos";

import { Component, Prop, Vue } from "vue-property-decorator";
import Constants from "../services/Constants";
import Helpers from "../services/Helpers";
import { RouterUtils } from "@/router/RouterUtils";

@Component({
  components: {
    FisholaHeader,
    FisholaFooter,
  },
})
export default class NewsView extends Vue {
  @Prop() news: NewsBean[];

  getMiniatureURl(news: NewsBean) {
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

  showNewsDetails(newsId: string) {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/news/" + newsId);
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../less/main";
.empty {
  padding-top: 20px;
}
.news {
  cursor: pointer;
  padding-top: 20px;

  overflow-y: scroll;
  height: calc(100vh - 40px - env(safe-area-inset-top) - 20px - 22px - 8px - 76px - env(safe-area-inset-bottom) );
  padding-bottom: @margin-large;

  .news-holder {
    border-bottom: 1px solid @gainsboro;
    margin-bottom: 40px;
    .news-row {
      display: flex;
      padding-left: @margin-x-large;
      padding-right: @margin-x-large;
      @media (max-width: 768px) {
        padding-left: @margin-x-small;
        padding-right: @margin-x-small;
        flex-direction: column;
        align-items: center;
      }

      .news-pic {
        @media (min-width: 768px) {
          width: 20vw;
          height: 20vw;
        }
        max-width: 20vh;
        max-height: 20vh;
        object-fit: cover;
      }
      .right-content {
        width: 100%;
        padding-left: 30px;
        @media (max-width: 768px) {
          padding-left: 0;
        }
        .publication-date {
          padding-top: 10px;
          display: flex;
          gap: 10px;
          color: @pale-sky;
          text-transform: uppercase;
        }
      }
    }
  }
  .news-content {
    padding-top: 10px;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
    @media (max-width: 768px) {
        padding-left: @margin-x-small;
        padding-right: @margin-x-small;
    }
  }

  .read-more {
    display: flex;
    justify-content: end;
    gap: 10px;
    width: 100%;
    color: @pelorous;
    font-weight: bold;
    padding: 10px;
    i {
      background-color: @pelorous;
      color: white;
      padding-left: 15px;
      padding-right: 15px;
      border-radius: 10px;
    }
  }

  .only-on-small-screen {
    display: none;
  }
  @media (max-width: 1200px) {
    .only-on-small-screen {
      max-height: 80px;
      display: block;
    }
    .only-on-big-screen {
      display: none;
    }
  }
}
</style>
