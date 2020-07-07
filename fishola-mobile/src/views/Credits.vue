<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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
  <div class="credits page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page credits-page">
      <div class="pane pane-only">
        <div class="pane-content rounded">
          <h1>Infos</h1>

          <div class="credits-row">
            <p v-for="p in paragraphs"
               v-bind:key="p"
               class="credits-p">
              {{p}}
            </p>
          </div>

          <div v-if="link" class="credits-link">
            <a v-bind:href="link" target="_blank">
              Plus d'informations
              <button>
                <i class="icon-arrow"/>
              </button>
            </a>
          </div>

          <div class="credits-logos">
            <a href="https://www.inrae.fr" target="_blank"><img src='/img/credits/inrae.svg'/></a>
            <a href="https://ofb.gouv.fr/" target="_blank"><img src='/img/credits/ofb.png'/></a>
            <a href="https://www.codelutin.com" target="_blank"><img src='/img/credits/code-lutin.png'/></a>
          </div>

          <div class="bottom-page-spacer"></div>
        </div>
      </div>
      <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip"/>
    </div>
    <FisholaFooter shortcuts="back,credits,documentation"
                   selected="credits" />
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue';
import RunningOverlay from '@/components/layout/RunningOverlay.vue';
import FisholaFooter from '@/components/layout/FisholaFooter.vue';

import {Editorial} from '@/pojos/BackendPojos';
import DocumentationService from '@/services/DocumentationService';
import TripsService from '@/services/TripsService';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    RunningOverlay,
    FisholaFooter
  }
})
export default class CreditsView extends Vue {

  paragraphs:string[] = [];
  link = '';

  hasRunningTrip:boolean = false;

  constructor() {
    super();
  }

  mounted() {
    DocumentationService.getCredits()
      .then(this.creditsLoaded);
    TripsService.hasRunningTrip()
      .then((result:boolean) => this.hasRunningTrip = result);
  }

  creditsLoaded(editorial:Editorial) {
    editorial.content
      .split("<br/>")
      .forEach((p:string) => {
        this.paragraphs.push(p);
      });
    this.link = editorial.link;
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../less/main";

.credits-page {

  p.credits-p {
    font-size: @fontsize-small-paragraph;
    line-height: calc(@fontsize-small-paragraph + 4px);
    color: @pale-sky;
    text-align: left;
  }

  .credits-link {
    width: 100%;
    text-align: right;
    font-weight: bold;
    font-size: @fontsize-header-paragraph;
    line-height: calc(@fontsize-header-paragraph + 5px);

    a, a:visited {
      text-decoration: none;
      color: @summer-sky;
    }

    button {
      width: 32px;
      height: 20px;
      background-color: @summer-sky;
      color: @white;
      border: 0px;
      border-radius: 50px;
      margin-left: 10px;
    }
  }

  .credits-logos {
    margin-top: 30px;
    text-align: center;
    img {
      width: 200px;
      margin-bottom: 10px;
    }
  }
}

</style>
