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
          <img :src="getMiniatureURl(doc)" class="news-pic" />
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
      <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip" />

      <FisholaFooter
        shortcuts="back,credits,documentation"
        selected="documentation"
      />
    </div>
  </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import TripsService from "@/services/TripsService";
import { DocumentationLight } from "@/pojos/BackendPojos";

import { Component, Prop, Vue } from "vue-property-decorator";
import Constants from "../services/Constants";
import Helpers from "../services/Helpers";
import router from "../router";

@Component({
  components: {
    FisholaHeader,
    RunningOverlay,
    FisholaFooter,
  },
})
export default class NewsView extends Vue {
  @Prop() news: DocumentationLight[];

  hasRunningTrip: boolean = false;

  mounted() {
    TripsService.hasRunningTrip().then(
      (result: boolean) => (this.hasRunningTrip = result)
    );
  }

  getMiniatureURl(news: DocumentationLight) {
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
    router.push("/news/" + newsId);
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
  .news-holder {
    border-bottom: 1px solid @gainsboro;
    margin-bottom: 40px;
    .news-row {
      display: flex;
      padding-left: @margin-x-large;
      padding-right: @margin-x-large;
      .news-pic {
        width: 20vw;
        height: 20vw;
        max-width: 20vh;
        max-height: 20vh;
        object-fit: cover;
      }
      .right-content {
        width: 100%;
        padding-left: 30px;
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
