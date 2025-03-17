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
  <div class="documentation page-with-header-and-footer shifted-background">
    <FisholaHeader />

    <div class="page documentation-page">
      <div class="pane pane-only">
        <div class="pane-content large rounded">
          <h1 class="no-margin-pane">Documentation</h1>
          <div class="main-tabs">
            <div class="tab" :class="{ selected: activeTab == 'doc' }" @click="goDoc">
              Documents à télécharger
            </div>
            <div class="tab" :class="{ selected: activeTab == 'faq' }" @click="goFaq">
              <span> FAQ </span>
            </div>
          </div>
          <div v-if="activeTab === 'doc'">
            <div class="documentation-row" v-for="doc in elements" v-bind:key="doc.id">
              <span>{{ doc.name }}</span>
              <a v-bind:href="doc.url" title="Télécharger" target="_blank">
                <i class="icon-download" />
              </a>
            </div>
          </div>

          <div v-else>
            <div class="faq-rows" v-html="faqRows"></div>
            <div class="credits-row">
              <h1>Information et crédit</h1>
              <p v-for="p in creditsParagraphs" v-bind:key="p" class="credits-p">
                {{ p }}
              </p>
              <div v-if="creditsLink" class="credits-link">
                <a v-bind:href="creditsLink" target="_blank">
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
                <a href="https://www.codelutin.com" target="_blank" rel="noopener"><img
                    src='/img/credits/code-lutin.svg' alt="Code Lutin" /></a>
              </div>
            </div>


            <div class="bottom-page-spacer"></div>
          </div>

          <div class="bottom-page-spacer"></div>
        </div>
      </div>
      <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip" />
    </div>
    <FisholaFooter shortcuts="back,credits,documentation" selected="documentation" />
  </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import DocumentationService from "@/services/DocumentationService";
import TripsService from "@/services/TripsService";
import { DocumentationLight, Editorial } from "@/pojos/BackendPojos";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import router from "@/router";
import { RouterUtils } from "@/router/RouterUtils";

@Component({
  components: {
    FisholaHeader,
    RunningOverlay,
    FisholaFooter,
  },
})
export default class DocumentationView extends Vue {
  @Prop() tab: string;
  activeTab = "doc";
  elements: DocumentationLight[] = [];
  faqRows: string = "";
  creditsParagraphs: string[] = [];
  creditsLink = '';

  hasRunningTrip: boolean = false;

  mounted() {
    if (this.tab) {
      this.activeTab = this.tab;
    }
    DocumentationService.getDocumentations().then(this.documentationsLoaded);
    DocumentationService.getFaq().then(this.faqLoaded);
    DocumentationService.getCredits().then(this.creditsLoaded);
    TripsService.hasRunningTrip().then(
      (result: boolean) => (this.hasRunningTrip = result)
    );
  }

  @Watch("tab")
  activeTabChanged() {
    this.activeTab = this.tab;
  }

  documentationsLoaded(docs: DocumentationLight[]) {
    const sortedDocs = Vue.lodash.orderBy(docs, "name");
    sortedDocs.forEach((doc) => this.elements.push(doc));
  }

  faqLoaded(editorial: Editorial) {
    this.faqRows = editorial.content;
  }

  creditsLoaded(editorial: Editorial) {
    editorial.content
      .split("<br/>")
      .forEach((p: string) => {
        this.creditsParagraphs.push(p);
      });
    this.creditsLink = editorial.link;
  }

  goDoc() {
    RouterUtils.pushRouteNoDuplicate(router, "/documentation/doc");
  }

  goFaq() {
    RouterUtils.pushRouteNoDuplicate(router, "/documentation/faq");
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../less/main";

.documentation-page {
  .pane .pane-content {
    padding-left: 0px;
    padding-right: 0px;
  }

  .documentation-row {
    padding-left: @margin-x-large;
    padding-right: @margin-x-large;
    height: 56px;

    @media (max-height: 579px) {
      height: 46px;
    }

    @media (max-width: 370px) {
      padding-left: @margin-large;
      padding-right: @margin-large;
    }

    border-bottom: 1px solid @solitude;

    display: flex;
    justify-content: space-between;
    align-items: center;

    span {
      font-size: @fontsize-small-paragraph;
      line-height: calc(@fontsize-small-paragraph + @line-height-padding-medium );
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

  .faq-rows {
    padding-left: @margin-x-large;
    padding-right: @margin-x-large;

    @media (max-width: 370px) {
      padding-left: @margin-large;
      padding-right: @margin-large;
    }

    display: flex;
    flex-direction: column;

    /deep/ .faq {
      margin-top: 10px;
      margin-bottom: 10px;

      h4 {
        margin-top: 0px;
        margin-bottom: 0px;
        font-size: 18px;
        font-weight: normal;
      }

      p {
        margin-top: 5px;
        margin-bottom: 5px;
        font-size: 16px;
        color: @pale-sky;
      }
    }
  }

  .credits-row {
    padding-left: @margin-x-large;
    padding-right: @margin-x-large;

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
}
</style>
