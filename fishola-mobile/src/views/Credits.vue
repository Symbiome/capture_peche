<template>
  <div class="credits page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page credits-page">
      <div class="pane">
        <h1>Infos</h1>
        <div class="pane-content">

            <div class="credits-row warning">
                Work in progress
            </div>

        </div>
      </div>
    </div>
    <FisholaFooter shortcuts="logout,credits,documentation"
                   selected="credits" />
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
export default class Credits extends Vue {
  
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

.credits-page {
  .warning {
    width: 100%;
    text-align: center;
    color: @carrot-orange;
  }
}

</style>
