<template>
  <div class="documentation page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page documentation-page">
      <div class="pane">
        <h1>Documentation</h1>
        <div class="pane-content">

            <div class="documentation-row">
                Ah !
            </div>

        </div>
      </div>
    </div>
    <FisholaFooter shortcuts="logout,credits,documentation"
                   selected="documentation" />
  </div>
</template>

<script>

import FisholaHeader from '@/components/layout/FisholaHeader.vue'

import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    FisholaFooter
  }
})
export default class Documentation extends Vue {
  
  constructor() {
    super();
  }

  mounted() {
  }
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../less/main";

.documentation-page {

  .pane-content {
    padding-left: 0px;
    padding-right: 0px;
  }

}

</style>
