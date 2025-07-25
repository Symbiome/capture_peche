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
  <div class="some-trip-header secondary-header hiddenWhenKeyboardShows">
    <div>
      <span v-if="trip">{{trip.name}}</span>
      <span v-if="!trip">Nouvelle sortie</span>
    </div>
    <div class="header-icons">
      <div class="header-icons-group">
        <span v-if="trip">{{catchsCount()}}</span>
        <span v-if="!trip">0</span>
        <i class="icon-fish"></i>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import TripMeta from '@/pojos/TripMeta';
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class SomeTripHeader extends Vue {
  @Prop() trip:TripMeta;

  getCatchs(object:any):number {
    if (object && object.catchs) {
      return object.catchs.length;
    }
    return 0;
  }

  catchsCount():number {
    const result:number = this.getCatchs(this.trip);
    return result;
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.some-trip-header {

  display: flex;
  justify-content: space-between;


  font-weight: bold;
  font-size: @fontsize-header-paragraph;

  .header-icons {
    display: flex;
    flex-direction: row;
    align-items: center;

    .header-icons-group {
      display: flex;
      margin-left: @margin-medium;
      margin-right: 0px;
      align-items: center;

      * {
        margin-left: @margin-x-small;
      }

      .icon-chevron {
        margin-top: calc(@fontsize-small-chevron - 1px);
        font-size:@fontsize-small-chevron;
      }
    }
  }
}
</style>
