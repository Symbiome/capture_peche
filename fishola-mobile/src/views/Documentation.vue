<template>
  <div class="documentation page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page documentation-page">
      <div class="pane pane-only">
        <h1>Documentation</h1>
        <div class="pane-content">

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
      <RunningOverlay v-if="hasRunningTrip"/>
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
export default class Documentation extends Vue {
  
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
    docs.forEach((doc) => this.elements.push(doc));
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
    padding-left: 40px;
    padding-right: 40px;
    height: 56px;
    border-bottom: 1px solid @solitude;

    display: flex;
    justify-content: space-between;
    align-items: center;

    span {
      font-size: 12px;
      line-height: 16px;
      color: @gunmetal;
    }

    a {
      color: @pelorous;
    }
  }
}

</style>
