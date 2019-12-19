<template>
  <div class="dashboard page-with-header shifted-background">
    <FisholaHeader/>
    <div class="page dashboard-page">
      <div class="dashboard-header">
        <h1>Tableau de bord</h1>
      </div>
      <div class="dashboard-content">

        <h2><i class="icon-fish" />Mes poissons</h2>
        <div class="placeholder">
          Mes poissons
        </div>

        <h2><i class="icon-fish" />Moyenne des captures</h2>
        <span>12 captures en moyenne / sortie</span>
        <div class="placeholder">
          Moyenne des captures
        </div>

        <h2><i class="icon-fish" />Top 5 tailles</h2>
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

        <h2><i class="icon-fish" />Top 5 poids</h2>
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
      <div>
        <FisholaFooter buttons="logout,dashboard,home"
                      selected="dashboard" />
      </div>
    </div>
  </div>
</template>

<script>
import Trip from '@/pojos/Trip';

import FisholaHeader from '@/layout/FisholaHeader.vue'

import FisholaFooter from '@/layout/FisholaFooter.vue'

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

  .dashboard-header {

    background-color: @white-smoke;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    margin-top: 20px;
    padding-left: 30px;
    padding-right: 30px;
    padding-top: 30px;

    color: @gunmetal;

    h1 {
      margin-top: 0px;
      margin-bottom: 30px;
      height: 30px;
      font-style: normal;
      font-weight: normal;
      font-size: 22px;
      line-height: 30px;
      color: @pelorous;
      text-align: center;
    }

  }

  .dashboard-content {

    overflow: auto;

    // flex:auto;

    display: flex;
    flex-direction: column;
    justify-content: flex-start;

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
      background-color: @pelorous;
      opacity: 40%;
      border-radius: 10px;
      
      height: 200px;
    }

    .scroll {
      display: flex;
      flex-direction: row;
      justify-content: flex-start;

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
