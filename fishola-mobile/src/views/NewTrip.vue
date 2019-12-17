<template>
  <div class="new-trip page-with-header">
    <FisholaHeader />
    <div class="page new-trip-page">
      <SomeTripHeader/>
      <div class="new-trip-options">
        <div class="new-trip-option">
          <div class="item">
            <div class="left">
              <img src="img/illustration_live.svg"/>
            </div>
            <div class="right">
              <div class="title">En direct</div>
              <div class="detail">C’est parti, démarrez une sortie et renseignez vos captures en direct&nbsp;!</div>
              <div class="action"><button><i class="icon-arrow" /></button></div>
            </div>
          </div>
        </div>
        <div class="new-trip-option">
          <div class="item">
            <div class="left">
              <img src="img/illustration_house.svg"/>
            </div>
            <div class="right">
              <div class="title">À la maison</div>
              <div class="detail">Vous rentrez d’une sortie de pêche&nbsp;? Renseignez vos captures à posteriori</div>
              <div class="action"><button><i class="icon-arrow" /></button></div>
            </div>
          </div>
        </div>
      </div>
      <FisholaFooter buttons="back,giveup"
                     selected="home"/>
    </div>
  </div>
</template>

<script lang="ts">
import Trip from '@/pojos/Trip';

import FisholaHeader from '@/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
  
@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    FisholaFooter
  }
})
export default class NewTrip extends Vue {
  
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

.new-trip-page {

  display: flex;
  flex-direction: column;
  justify-content: space-between;

  .new-trip-options {
    flex:auto;

    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;

    background-color: @white-smoke;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    padding-top: 30px;
    padding-bottom: 30px;

    .new-trip-option {
      height: 50%;

      padding-left: 30px;
      padding-right: 30px;

      display: flex;
      flex-direction: row;
      justify-content: center;
      align-items: center;

      .item {

        display: flex;
        flex-direction: row;
        justify-content: center;
        align-items: flex-start;

        .left {
          margin-left: 5px;
          margin-right: 5px;

          img {
            width: 60px;
            height: 60px;
            color: @pelorous;
          }
        }

        .right {
          margin-left: 15px;

          display: flex;
          flex-direction: column;
          align-items: flex-start;

          * {
            margin-bottom: 10px;
          }

          .title {
            font-weight: bold;
            font-size: 18px;
            line-height: 25px;
            color: @pelorous;
          }

          .detail {
            font-size: 14px;
            line-height: 19px;
            color: @gunmetal;
          }

          .action {

            width: 100%;
            text-align: right;

            button {
              font-size: 24px;
              border: 0px;
              height: 40px;
              border-radius: 50px;
              padding-left: 20px;
              padding-right: 20px;
              color: @white;
              background-color: @terra-cotta;
            }
          }
        }
      }


    }

    div.new-trip-option:first-child {
      border-bottom: 1px solid @gainsboro;
    }
  }
}

</style>
