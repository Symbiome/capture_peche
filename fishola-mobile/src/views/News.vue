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
  <div class="news page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page news-page">
      <div class="pane pane-only">
        <div class="pane-content rounded">
          <h1 class="no-margin-pane">On en parle</h1>

          <div v-if="!elements || elements.length == 0" class="empty">
            Il n'y a pas encore d'actualités ...
          </div>

          <div class="news-row"
               v-for="doc in elements"
               v-bind:key="doc.id">
            <span>{{doc.name}}</span>
            <a v-bind:href="doc.url"
               title="Télécharger"
               target="_blank">
              <i class="icon-download"/>
            </a>
          </div>

          <div class="bottom-page-spacer"></div>
        </div>
      </div>
      <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip"/>
    </div>
    <FisholaFooter shortcuts="back,credits,documentation"
                   selected="documentation" />
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue';
import RunningOverlay from '@/components/layout/RunningOverlay.vue';
import FisholaFooter from '@/components/layout/FisholaFooter.vue';

import DocumentationService from '@/services/DocumentationService';
import TripsService from '@/services/TripsService';
import {DocumentationLight} from '@/pojos/BackendPojos';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    RunningOverlay,
    FisholaFooter
  }
})
export default class NewsView extends Vue {
  
  elements:DocumentationLight[] = [];

  hasRunningTrip:boolean = false;

  constructor() {
    super();
  }

  mounted() {
    DocumentationService.getNews()
      .then(this.documentationsLoaded);
    TripsService.hasRunningTrip()
      .then((result:boolean) => this.hasRunningTrip = result);
  }

  documentationsLoaded(docs:DocumentationLight[]) {
    const sortedDocs = Vue.lodash.orderBy(docs, 'name');
    sortedDocs.forEach((doc) => this.elements.push(doc));
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../less/main";

.news-page {

  .pane .pane-content {
    padding-left: 0px;
    padding-right: 0px;
  }

  .empty {
    color: @pale-sky;
    text-align: center;
    font-style: italic;
    font-size: @fontsize-button;
  }

  .news-row {
    padding-left: @margin-x-large;
    padding-right: @margin-x-large;
    height: 56px;

    @media(max-height:579px) {
      height: 46px;
    }

    @media(max-width:370px) {
      padding-left: @margin-large;
      padding-right: @margin-large;
    }

    border-bottom: 1px solid @solitude;

    display: flex;
    justify-content: space-between;
    align-items: center;

    span {
      font-size: @fontsize-small-paragraph;
      line-height: calc(@fontsize-small-paragraph + @line-height-padding-medium);
      color: @gunmetal;
    }

    a {
      color: @pelorous;
      font-size: @fontsize-paragraph;
    }

    @media screen and (min-width: @desktop-min-width) {
      span {
        font-size: @fontsize-paragraph;
        line-height: calc(@fontsize-paragraph + @line-height-padding-medium);
      }
      a {
        font-size: @fontsize-paragraph-desktop;
      }
    }

    @media screen and (min-width: 800px) {
      padding-left: @margin-large-desktop;
      padding-right: @margin-large-desktop;
    }

  }
}

</style>
