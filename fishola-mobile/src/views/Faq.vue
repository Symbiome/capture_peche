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
  <div class="faq page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page faq-page">
      <div class="pane pane-only">
        <div class="pane-content rounded">
          <h1>FAQ</h1>

          <div class="faq-row">
            <p v-for="p in paragraphs"
               v-bind:key="p"
               class="faq-p">
              {{p}}
            </p>
          </div>

          <div class="bottom-page-spacer"></div>
        </div>
      </div>
      <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip"/>
    </div>
    <FisholaFooter shortcuts="back,credits,documentation"/>
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

  hasRunningTrip:boolean = false;

  constructor() {
    super();
  }

  mounted() {
    DocumentationService.getFaq()
      .then(this.faqLoaded);
    TripsService.hasRunningTrip()
      .then((result:boolean) => this.hasRunningTrip = result);
  }

  faqLoaded(editorial:Editorial) {
    editorial.content
      .split("<br/>")
      .forEach((p:string) => {
        this.paragraphs.push(p);
      });
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../less/main";

.faq-page {

  p.faq-p {
    font-size: @fontsize-small-paragraph;
    line-height: calc(@fontsize-small-paragraph + @line-height-padding-medium);
    color: @pale-sky;
    text-align: left;
  }

}

</style>
