<template>
  <div class="dashboard page-with-header">
    <FisholaHeader/>
    <div class="page dashboard-page">
      <div class="dashboard-header">
        <div class="bars">Bar chart</div>
        <div>X captures en moyenne par sortie</div>
      </div>
      <div class="dashboard-content">
        <h1>Tableau de bord</h1>
        <h2><i class="icon-fish" />Mes poissons</h2>
        <p>Quelques statistiques ...</p>
      </div>
      <FisholaFooter buttons="logout,dashboard,home"
                     selected="dashboard" />
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

  overflow: auto;

  .dashboard-header {
    height: 300px;

    display: flex;
    flex-direction: column;
    justify-content: space-between;

    text-align:center;

    padding: 30px;

    .bars {
      height: 200px;
      background-color: rgba(255, 255, 255, 0.7);
      border-radius:8px;

      display: flex;
      flex-direction: column;
      justify-content: center;
      text-align:center;

    }
  }

  .dashboard-content {

    flex:auto;

    display: flex;
    flex-direction: column;
    justify-content: flex-start;

    text-align:center;

    background-color: @white-smoke;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    margin-top: 50px;
    padding-left: 30px;
    padding-right: 30px;
    padding-top: 30px;

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

    h2 {

      i {
        color: @pelorous;
        margin-right: 10px;
      }

      color: @gunmetal;
      font-style: normal;
      font-weight: normal;
      font-size: 22px;
      line-height: 30px;
      text-align: left;
    }

    p { 
      color: @gunmetal;
      font-style: normal;
      font-weight: normal;
      text-align: left;
    }

  }

}

</style>
