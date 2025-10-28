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
  <div class="credits page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="page credits-page">
      <div class="pane pane-only">
        <div class="pane-content rounded">
          <h1>Information et crédits</h1>

          <div class="credits-row">
            <p v-for="p in paragraphs" v-bind:key="p" class="credits-p">
              {{ p }}
            </p>
          </div>

          <div v-if="link" class="credits-link">
            <a v-bind:href="link" target="_blank">
              Plus d'informations
              <button>
                <i class="icon-arrow" />
              </button>
            </a>
          </div>

          <div class="credits-logos">
            <a href="https://www.inrae.fr" target="_blank" rel="noopener"><img src='/img/credits/inrae.svg'
                alt="INRAE" /></a>
            <a href="https://ofb.gouv.fr/" target="_blank" rel="noopener"><img src='/img/credits/ofb.png'
                alt="OFB" /></a>
            <a href="https://www.codelutin.com" target="_blank" rel="noopener"><img src='/img/credits/code-lutin.svg'
                alt="Code Lutin" /></a>
          </div>

          <div class="bottom-page-spacer"></div>
        </div>
      </div>
      <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip" />
    </div>
    <FisholaFooter shortcuts="back,credits,documentation" selected="credits" />
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue';
import RunningOverlay from '@/components/layout/RunningOverlay.vue';
import FisholaFooter from '@/components/layout/FisholaFooter.vue';

import { Editorial } from '@/pojos/BackendPojos';
import DocumentationService from '@/services/DocumentationService';
import TripsService from '@/services/TripsService';

import { Component, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    RunningOverlay,
    FisholaFooter
  }
})
export default class CreditsView extends Vue {

  paragraphs: string[] = [];
  link = '';

  hasRunningTrip: boolean = false;

  constructor() {
    super();
  }

  mounted() {
    DocumentationService.getCredits()
      .then(this.creditsLoaded);
    TripsService.hasRunningTrip()
      .then((result: boolean) => this.hasRunningTrip = result);
  }

  creditsLoaded(editorial: Editorial) {
    editorial.content
      .split("<br/>")
      .forEach((p: string) => {
        this.paragraphs.push(p);
      });
    this.link = editorial.link;
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
.credits-page {

  p.credits-p {
    font-size: @fontsize-small-paragraph;
    line-height: calc(@fontsize-small-paragraph + @line-height-padding-medium);
    color: @pale-sky;
    text-align: left;
  }

  .credits-link {
    width: 100%;
    text-align: right;
    font-weight: bold;
    font-size: @fontsize-header-paragraph;
    line-height: calc(@fontsize-header-paragraph + @line-height-padding-large);

    a,
    a:visited {
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
      margin-left: @margin-small;
    }
  }

  .credits-logos {
    margin-top: @vertical-margin-large;
    text-align: center;
    display: flex;
    flex-direction: column;

    img {
      width: 200px;
      margin-bottom: @vertical-margin-small;
    }
  }

  @media screen and (min-width: @desktop-min-width) {

    p.credits-p {
      font-size: @fontsize-paragraph;
      line-height: calc(@fontsize-paragraph + @line-height-padding-medium);
    }

    .credits-link {

      font-size: @fontsize-paragraph;
      line-height: calc(@fontsize-paragraph + @line-height-padding-large);

      button {
        width: 38px;
        height: 24px;
        font-size: @fontsize-paragraph;
        line-height: calc(@fontsize-paragraph + @line-height-padding-large);
      }
    }

    .credits-logos {
      margin-top: @vertical-margin-xx-large;

      img {
        width: 240px;
      }
    }
  }

}
</style>
