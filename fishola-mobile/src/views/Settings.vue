<template>
  <div class="settings page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page settings-page">
      <div class="pane">
        <h1>Paramètres</h1>
        <div class="pane-content">
          <div class="warning">Work in progress</div>
        </div>
      </div>
    </div>
    <FisholaFooter shortcuts="back,settings,profile"
                   selected="settings" />
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue'

import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    FisholaFooter
  }
})
export default class Settings extends Vue {
  
  constructor() {
    super();
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../less/main";

.settings-page {

  .warning {
    width: 100%;
    text-align: center;
    color: @carrot-orange;
  }
}

</style>
