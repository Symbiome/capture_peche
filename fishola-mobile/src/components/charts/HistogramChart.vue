<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
  <div class="histogram">
    <div class="values">
      <div class="value" v-for="m in orderedMonths" v-bind:key="'value-' + m">
        {{values[m] ? Math.round(values[m]) : ''}}
      </div>
    </div>
    <div class="bars">
      <div class="bar" v-for="(m, index) in orderedMonths" v-bind:key="'bar-' + m">
        <div class="bar-filled"
              v-if="values[m]"
              v-bind:class="index % 2 == 0 ? 'even' : 'odd'"
              v-bind:style="'height: ' + (values[m] * 100 / maxValue) + '%;'"></div>
      </div>
    </div>
    <div class="labels">
      <div class="label-short"
           v-for="m in orderedMonths"
           v-bind:key="'label-short-' + m">
        {{m.substring(0,1)}}
      </div>
      <div class="label-mid"
           v-for="m in orderedMonths"
           v-bind:key="'label-mid-' + m">
        {{midMonth(m)}}
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import Constants from '@/services/Constants';

import { Component, Prop, Vue, Watch } from 'vue-property-decorator';

import { Month } from '@/pojos/BackendPojos';

@Component
export default class HistogramChart extends Vue {

  @Prop() orderedMonths:Month[];
  @Prop() values:{ [P in Month]?: number };

  maxValue:number = 50;

  mounted() {
    this.maxValue = this.findMaxValue(this.values);
  }

  midMonth(m:Month) {
    let index = this.orderedMonths.indexOf(m);
    let result = Constants.MONTHS[index];
    return result;
  }

  @Watch('values')
  onValuesChanged(newValue:{ [P in Month]?: number }, oldValue:{ [P in Month]?: number }) {
    this.maxValue = this.findMaxValue(newValue);
  }

  findMaxValue(someValues:{ [P in Month]?: number }):number {
    const numbers:(number|undefined)[] = Object.values(someValues);
    let newMax:number = 0;
    numbers.forEach((n) => {
      if (n && n > newMax) {
        newMax = n;
      }
    });
    let result = Math.max(newMax, 50);
    return result;
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.histogram {
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 250px;

  .values {
    width: 100%;
    height: 18px;
    display: flex;
    flex-direction: row;
    justify-content: space-evenly;

    .value {
      width: 8%;
      font-weight: bold;
      font-size: @fontsize-smallest-paragraph;
      color: @pelorous;
      text-align: center;
    }
  }
  .bars {
    width: 100%;
    height: calc(100% - 18px - 25px);
    display: flex;
    flex-direction: row;
    justify-content: space-evenly;

    .bar {
      width: 6%;
      margin-left: 1%;
      margin-right: 1%;
      height: 100%;
      background-color: @solitude;
      border-radius: 2px;

      position: relative;


      .bar-filled {
        position: absolute;
        bottom: 0px;
        width: 100%;
        border-radius: 2px;

        &.even {
          background: @pelorous;
        }

        &.odd {
          background: @summer-sky;
        }

      }
    }
  }

  .labels {
    width: 100%;
    height: 25px;
    display: flex;
    flex-direction: row;
    justify-content: space-evenly;

    .label-short,
    .label-mid {
      width: 8%;
      color: @gunmetal;
      text-align: center;
    }

    @media screen and (max-width: 900px) {
      .label-mid {
        display: none;
      }
    }

    @media screen and (min-width: 901px) {
      .label-short {
        display: none;
      }
    }

  }
}

</style>
