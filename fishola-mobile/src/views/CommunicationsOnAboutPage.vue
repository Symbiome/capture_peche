<template>
  <div class="news-about">
    <div class="news-holder no-news" v-if="news.length == 0">
      Pas de communications autour de Fishola pour le moment, revenez consultez
      cette page prochainement !
    </div>
    <div
      v-else
      class="news-holder"
      v-for="doc in news"
      v-bind:key="doc.id"
      @click="showNewsDetails(doc.id)"
    >
      <div class="news-row">
        <div class="news-title">
          <div class="left-pic">
            <img :src="getMiniatureURl(doc)" class="news-pic" />
          </div>
          <div class="right-content">
            <strong>{{ doc.name }}</strong> <br />
          </div>
        </div>
        <span class="publication-date">
          <i class="icon-send" />Publié le
          {{ formatPublicationDate(doc.datePublicationDebut) }}
        </span>
        <div class="news-content">
          {{ treatHTMLContent(doc.content) }}
        </div>
        <div>
          <div class="read-more">Lire la suite <i class="icon-arrow" /></div>
        </div>
      </div>
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
export default class CommunicationsOnAboutPage extends Vue {
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
.no-news {
  color: @pale-sky;
  padding: 20px;
}
.news-about {
  cursor: pointer;
  width: 100%;
  max-width: 1200px;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: center;
  .news-holder {
    .news-row {
      padding-left: @margin-x-large;
      padding-right: @margin-x-large;
      height: fit-content;
      width: 350px;
      margin: 15px;
      padding: 30px;
      border-radius: 15px;
      background-color: @white;
      box-shadow: 5px 5px 5px @gunmetal;
      .news-title {
        display: flex;
      }
      .news-pic {
        width: 15vw;
        height: 15vw;
        max-width: 15vh;
        max-height: 15vh;
        object-fit: cover;
      }
      .right-content {
        width: 100%;
        padding-left: 30px;
        color: black;
      }
    }
    .publication-date {
      padding-top: 10px;
      display: flex;
      gap: 10px;
      color: @pale-sky;
      text-transform: uppercase;
    }
  }
  .news-content {
    padding-top: 10px;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
    color: black;
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

  @media (max-width: 1200px) {
    .news-about {
      max-width: 100vw;
      .news-holder {
        .news-row {
          width: 100%;
        }
      }
    }
  }
}
</style>
