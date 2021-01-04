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
  <div class="new-trip page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="page new-trip-page">
      <SomeTripHeader/>
      <div class="pane">
        <div class="new-trip-option">
          <div class="item">
            <div class="left">
              <img src="img/illustration_live.svg"/>
            </div>
            <div class="right">
              <div class="title">En direct</div>
              <div class="detail">C’est parti, démarrez une sortie et renseignez vos captures en direct&nbsp;!</div>
              <div class="action"><button v-on:click="newLiveTrip"><i class="icon-arrow" /></button></div>
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
              <div class="action"><button v-on:click="newAfterwardsTrip"><i class="icon-arrow" /></button></div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <FisholaFooter shortcuts="back,spacer,giveup"/>
  </div>
</template>

<script lang="ts">
import TripsService from '@/services/TripsService';

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';
  
@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    FisholaFooter
  }
})
export default class NewTripView extends Vue {
  
  constructor() {
    super();
  }

  newLiveTrip() {
    TripsService.newLiveTrip().then((id:string) => {
      router.push({name:'trip-meta', params: {id: id}});
    });
  }

  newAfterwardsTrip() {
    TripsService.newAfterwardsTrip().then((id:string) => {
      router.push({name:'trip-meta', params: {id: id}});
    });
  }
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

.new-trip-page {

  .pane {
    flex:auto;

    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;

    padding-top: @vertical-margin-large;
    padding-bottom: @vertical-margin-large;

    .new-trip-option {
      height: 50%;

      padding-left: @margin-large;
      padding-right: @margin-large;

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
          margin-left: @margin-x-small;
          margin-right: @margin-x-small;

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

          .title {
            font-weight: bold;
            font-size: @fontsize-span-big;
            line-height: calc(@fontsize-span-big + @line-height-padding-x-large);
            color: @pelorous;
            margin-bottom: @vertical-margin-small;
          }

          .detail {
            font-size: @fontsize-header-paragraph;
            line-height: calc(@fontsize-header-paragraph + @line-height-padding-large);
            color: @gunmetal;
            margin-bottom: @vertical-margin-small;
          }

          .action {

            width: 100%;
            text-align: right;

            button {
              font-size: @fontsize-button-verry-big;
              border: 0px;
              height: 40px;
              border-radius: 50px;
              padding-left: @margin-medium;
              padding-right: @margin-medium;
              color: @white;
              background-color: @terra-cotta;
            }
          }
        }

        @media(max-height:579px) {

          .left {
            img {
              width: 50px;
              height: 50px;
            }
          }

          .right {
            .title {
              font-size: @fontsize-paragraph;
              line-height: calc(@fontsize-paragraph + @line-height-padding-x-large);
              margin-bottom: @vertical-margin-x-small;
            }

            .detail {
              margin-bottom: @vertical-margin-x-small;
            }

            .action {

              button {
                font-size: @fontsize-button-big;
                height: 30px;
                padding-left: @margin-small;
                padding-right: @margin-small;
              }
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
