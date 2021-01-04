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
  <div class="distribution">
    <div class="not-enough-data" v-if="distribution.length == 0">
      <span>Pas assez de données</span>
    </div>
    <div v-for="(f, index) in orderedCaughtSpeciesDistribution()"
        v-bind:key="f.id"
        class="distribution-row">
      <div class="distribution-row-data">
        <div class="species">
          {{f.name}}
          <span class="alias" v-if="f.alias">({{f.alias}})</span>
        </div>
        <div class="percent">
          {{f.count}}
        </div>
      </div>
      <div class="distribution-row-bar">
        <div class="distribution-row-bar-filled"
             v-bind:class="index % 2 == 0 ? 'even' : 'odd'"
             v-bind:style="'width: ' + f.percent + '%;'"></div>
        <div v-if="f.greenPercent"
             class="distribution-row-bar-filled green"
             v-bind:style="'width: ' + f.greenPercent + '%;'"></div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import DistributionEntry from '@/pojos/DistributionEntry';

import { Component, Prop, Vue, Watch } from 'vue-property-decorator';

@Component
export default class DistributionChart extends Vue {

  @Prop() distribution:DistributionEntry[];

  orderedCaughtSpeciesDistribution() {
    return Vue.lodash.orderBy(this.distribution, 'name');
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.distribution {
  width: 100%;
  display: flex;
  flex-direction: column;

  .distribution-row {

    margin-bottom: @vertical-margin-small;

    .distribution-row-data {
      display: flex;
      flex-direction: row;
      justify-content: space-between;

      font-size: @fontsize-small-paragraph;
      line-height: calc(@fontsize-small-paragraph + @line-height-padding-medium);
      height: calc(@fontsize-small-paragraph + @line-height-padding-medium);

      .species {
        color: @gunmetal;
      }

      .percent {
        font-weight: bold;
        color: @pelorous;
      }
    }

    .distribution-row-bar {
      position: relative;
      margin-top: 4px;
      height: 14px;
      border-radius: 7px;
      background: @solitude;

      .distribution-row-bar-filled {
        position: absolute;
        height: 14px;
        border-radius: 7px;

        &.even {
          background: @pelorous;
        }

        &.odd {
          background: @summer-sky;
        }

        &.green {
          background: @lime-green;
        }
      }

    }

  }

}

</style>
