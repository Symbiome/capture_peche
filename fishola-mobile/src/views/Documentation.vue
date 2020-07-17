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
  <div class="documentation page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page documentation-page">
      <div class="pane pane-only">
        <div class="pane-content rounded">
          <h1>Documentation</h1>

          <div class="documentation-row"
               v-for="doc in elements"
               v-bind:key="doc.id">
            <span>{{doc.name}}</span>
            <a v-bind:href="doc.url">
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
export default class DocumentationView extends Vue {
  
  elements:DocumentationLight[] = [];

  hasRunningTrip:boolean = false;

  constructor() {
    super();
  }

  mounted() {
    DocumentationService.getDocumentations()
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

.documentation-page {

  .pane .pane-content {
    padding-left: 0px;
    padding-right: 0px;
  }

  .documentation-row {
    padding-left: @margin-x-large;
    padding-right: @margin-x-large;
    height: 56px;
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
    }
  }
}

</style>
