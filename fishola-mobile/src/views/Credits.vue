<template>
  <div class="credits page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page credits-page">
      <div class="pane">
        <h1>Infos</h1>
        <div class="pane-content">

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
              <a href="https://www.inrae.fr" target="_blank"><img src='/img/credits/inrae.png'/></a>
              <a href="https://ofb.gouv.fr/" target="_blank"><img src='/img/credits/ofb.png'/></a>
              <a href="https://www.codelutin.com" target="_blank"><img src='/img/credits/code-lutin.png'/></a>
            </div>

        </div>
      </div>
    </div>
    <FisholaFooter shortcuts="back,credits,documentation"
                   selected="credits" />
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue'

import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import {Editorial} from '@/pojos/BackendPojos';
import DocumentationService from '@/services/DocumentationService';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    FisholaFooter
  }
})
export default class Credits extends Vue {

  paragraphs:string[] = [];
  link = '';

  constructor() {
    super();
  }

  mounted() {
    DocumentationService.getCredits()
      .then(this.creditsLoaded);
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
    font-size: 12px;
    line-height: 16px;
    color: @pale-sky;
    text-align: left;
  }

  .credits-link {
    width: 100%;
    text-align: right;
    font-weight: bold;
    font-size: 14px;
    line-height: 19px;

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
      max-width: 200px;
      max-height: 70px;
    }
  }
}

</style>
