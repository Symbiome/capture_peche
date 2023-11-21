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
    <div class="bars">
      <div
        class="bar"
        v-for="(m, index) in orderedMonths"
        v-bind:key="'bar-' + m"
      >
        <div v-if="values[m]">
          <div
            v-for="sizeType in ['MAILLEE', 'NON_MAILLEE', 'NON_DEFINI']"
            :key="m + sizeType"
          >
            <div
              v-if="values[m][sizeType]"
              class="value"
              :class="{
                maillee: sizeType != 'NON_MAILLEE',
                'non-maillee': sizeType == 'NON_MAILLEE',
                even: index % 2 == 0,
                odd: index % 2 != 0,
              }"
              :style="
                ('background-color:red',
                'height: ' +
                  Math.min(
                    108,
                    ((values[m][sizeType] +
                      (sizeType != 'NON_MAILLEE'
                        ? values[m][sizeType] > 65
                          ? 10
                          : 6
                        : -1)) *
                      100) /
                      maxValue
                  ) +
                  '%')
              "
            >
              {{
                values[m] && values[m][sizeType]
                  ? Math.round(values[m][sizeType])
                  : ""
              }}
            </div>
            <div
              class="bar-filled"
              :class="{
                maillee: sizeType != 'NON_MAILLEE',
                'non-maillee': sizeType == 'NON_MAILLEE',
                even: index % 2 == 0,
                odd: index % 2 != 0,
              }"
              v-if="values[m][sizeType]"
              v-bind:style="
                'height: ' + (values[m][sizeType] * 100) / maxValue + '%;'
              "
            ></div>
          </div>
        </div>
      </div>
    </div>
    <div class="labels">
      <div
        class="label-short"
        v-for="m in orderedMonths"
        v-bind:key="'label-short-' + m"
      >
        {{ m.substring(0, 1) }}
      </div>
      <div
        class="label-mid"
        v-for="m in orderedMonths"
        v-bind:key="'label-mid-' + m"
      >
        {{ midMonth(m) }}
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Constants from "@/services/Constants";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";

import { Month, Maillage } from "@/pojos/BackendPojos";

@Component
export default class HistogramChart extends Vue {
  @Prop() orderedMonths: Month[];
  @Prop() values: { [P in Month]?: { [S in Maillage]: number } };

  maxValue: number = 50;

  mounted() {
    this.maxValue = this.findMaxValue(this.values);
  }

  midMonth(m: Month) {
    let index = this.orderedMonths.indexOf(m);
    let result = Constants.MONTHS[index];
    return result;
  }

  @Watch("values")
  onValuesChanged(
    newValue: { [P in Month]?: {} },
    _oldValue: { [P in Month]?: {} }
  ) {
    this.maxValue = this.findMaxValue(newValue);
  }

  findMaxValue(someValues: {
    [P in Month]?: {};
  }): number {
    let newMax: number = 0;
    Object.keys(someValues).forEach((month) => {
      // @ts-ignore
      Object.keys(someValues[month]).forEach((st) => {
        // @ts-ignore
        const n = someValues[month][st];
        if (n && n > newMax) {
          newMax = n;
        }
      });
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

  .bars {
    padding-top: 10px;
    width: 100%;
    height: calc(100% - 18px - 25px);
    display: flex;
    flex-direction: row;
    justify-content: space-evenly;

    .value {
      position: absolute;
      bottom: 0px;
      width: 100%;
      z-index: 1;
      font-weight: bold;
      font-size: @fontsize-smallest-paragraph;
      text-align: center;

      &.maillee {
        color: @pelorous;
      }

      &.non-maillee {
        color: white;
      }
    }

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

        &.maillee {
          &.even {
            background: @pelorous;
          }

          &.odd {
            background: @summer-sky;
          }
        }

        &.non-maillee {
          &.even {
            background: @orange-even;
          }

          &.odd {
            background: @orange-odd;
          }
        }
      }
    }
  }

  .labels {
    margin-top: 10px;
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
