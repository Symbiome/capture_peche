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
  <div class="faq page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="page faq-page">
      <div class="pane pane-only">
        <div class="pane-content large rounded">
          <h1>FAQ</h1>

          <div class="faq-rows" v-html="faqRows"></div>

          <div class="bottom-page-spacer"></div>
        </div>
      </div>
      <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip" />
    </div>
    <FisholaFooter shortcuts="back,credits,documentation" />
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

  faqRows: string = '';

  hasRunningTrip: boolean = false;

  constructor() {
    super();
  }

  mounted() {
    DocumentationService.getFaq()
      .then(this.faqLoaded);
    TripsService.hasRunningTrip()
      .then((result: boolean) => this.hasRunningTrip = result);
  }

  faqLoaded(editorial: Editorial) {
    this.faqRows = editorial.content;
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.faq-page {

  p.faq-p {
    font-size: @fontsize-small-paragraph;
    line-height: calc(@fontsize-small-paragraph + @line-height-padding-medium);
    color: @pale-sky;
    text-align: left;
  }

  .faq-rows {
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

}
</style>
