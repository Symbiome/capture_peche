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
  <div class="my-trips-header secondary-header hiddenWhenKeyboardShows">
    <div>
      <span>Mes sorties</span>
    </div>
    <div class="header-icons">
      <div class="header-icons-group" v-on:click="$emit('reverseSortOrder')">
        <i class="icon-calendar"></i>
        <i class="icon-chevron" v-if="sortDown"></i>
        <i class="icon-chevron icon-chevron-up" v-if="!sortDown"></i>
      </div>
      <div class="header-icons-group">
        <span v-if="offline">?</span>
        <span v-if="!offline">{{count}}</span>
        <i class="icon-fishing"></i>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class MyTripsHeader extends Vue {
  @Prop() offline!:boolean;
  @Prop() count!:number;
  @Prop() sortDown!:boolean;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.my-trips-header {

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
      margin-left: 20px;
      margin-right: 0px;
      align-items: center;

      * {
        margin-left: 5px;
      }

      .icon-chevron {
        margin-top: calc(@fontsize-small-chevron - 1px);
        font-size:@fontsize-small-chevron;
      }
    }
  }
}
</style>
