<template>
  <div class="dashboard page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page dashboard-page">
      <div class="pane pane-only">
        <h1>Tableau de bord</h1>

        <div class="pane-content">
          <h2><i class="icon-fish" />Mes poissons</h2>
          <div class="placeholder">
            Mes poissons
          </div>

          <h2><i class="icon-fishing" />Moyenne des captures</h2>
          <span>12 captures en moyenne / sortie</span>
          <div class="placeholder">
            Moyenne des captures
          </div>

          <h2><i class="icon-size" />Top 5 tailles</h2>
          <div class="scroll">
            <div class="item selected">Perche</div>
            <div class="item">Brochet</div>
            <div class="item">Truite</div>
            <div class="item">Corégone</div>
            <div class="item">Omble_chevalier</div>
            <div class="item">Silure</div>
          </div>
          <div class="placeholder">
            Top 5 tailles
          </div>

          <h2><i class="icon-weight" />Top 5 poids</h2>
          <div class="scroll">
            <div class="item selected">Perche</div>
            <div class="item">Brochet</div>
            <div class="item">Truite</div>
            <div class="item">Corégone</div>
            <div class="item">Omble_chevalier</div>
            <div class="item">Silure</div>
          </div>
          <div class="placeholder">
            Top 5 poids
          </div>
        </div>

      </div>
    </div>
    <FisholaFooter shortcuts="logout,dashboard,home"
                   selected="dashboard" />
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
export default class Dashboard extends Vue {
  
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

.dashboard-page {

  display: flex;
  flex-direction: column;
  justify-content: space-between;

  text-align:center;

  .pane-content {

    overflow: auto;

    text-align:center;

    background-color: @white-smoke;
    padding-left: 30px;
    padding-right: 30px;
    padding-top: 30px;

    color: @gunmetal;

    h2 {

      i {
        color: @pelorous;
        margin-right: 10px;
      }

      font-style: normal;
      font-weight: normal;
      font-size: 22px;
      line-height: 30px;
      text-align: left;
    }

    .placeholder {

      display: flex;
      flex-direction: column;
      justify-content: center;

      color: @white;
      background-color: @cyprus;
      opacity: 0.8;
      border-radius: 8px;
      
      height: 200px;
    }

    .scroll {
      display: flex;
      flex-direction: row;
      justify-content: flex-start;
      margin-bottom: 10px;

      overflow:auto;

      div.item {
        margin-right: 20px;
        color: @pale-sky;
      }
      div.item.selected {
        color: @gunmetal;
        border-bottom: 3px solid @pelorous;
      }
    }
  }

}

</style>
