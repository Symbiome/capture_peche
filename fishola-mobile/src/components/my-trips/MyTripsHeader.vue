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
  <div class="my-trips-header secondary-header hiddenWhenKeyboardShows">
    <slot></slot>
    <div class="header-icons">
      <div
        class="header-icons-group clickable"
        v-on:click="$emit('reverseSortOrder')"
        title="Inverser le tri"
      >
        <i class="icon-calendar"></i>
        <i class="icon-chevron" v-if="sortDown"></i>
        <i class="icon-chevron icon-chevron-up" v-if="!sortDown"></i>
      </div>
      <div class="header-icons-group">
        <span v-if="offline">?</span>
        <span v-if="!offline">{{ count }}</span>
        <i class="icon-fishing"></i>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";

@Component
export default class MyTripsHeader extends Vue {
  @Prop() offline!: boolean;
  @Prop() count!: number;
  @Prop() sortDown!: boolean;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../../less/main";

.my-trips-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
        font-size: @fontsize-small-chevron;
      }
      &.clickable {
        cursor: pointer;
      }
    }
  }

  @media screen and (min-width: @desktop-min-width) {
    color: @gunmetal;
    font-size: @fontsize-paragraph-desktop;
    .header-icons {
      flex-direction: row-reverse;
    }
  }

  @media screen and (max-width: 1000px) and (min-width: 770px) {
    .header-icons {
      margin-right: -180px;
    }
  }
}

@media screen and (max-width: 770px) {
  .my-trips-header {
    padding-top: 20px;
    margin-left: -40px;
  }
}
</style>
