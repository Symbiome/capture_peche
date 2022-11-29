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
  <div class="options-list">
    <div
      class="item"
      v-bind:class="itemSelected && i == itemSelected ? 'selected' : ''"
      v-for="i in items"
      v-bind:key="'item-' + i.id"
      v-on:click="selectItem(i)"
    >
      {{ i.name }}
      <span class="alias" v-if="i.alias">({{ i.alias }})</span>
    </div>
  </div>
</template>

<script lang="ts">
import OptionItem from "@/pojos/OptionItem";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";

@Component
export default class OptionsList extends Vue {
  @Prop() items: OptionItem[];
  itemSelected: OptionItem | null = null;

  mounted() {
    this.selectFirstItem();
  }

  @Watch("items")
  selectFirstItem() {
    if (this.items) {
      this.selectItem(this.items[0]);
    }
  }

  selectItem(item: OptionItem) {
    this.itemSelected = item;
    this.$emit("item-selected", item);
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../../less/main";

.options-list {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  margin-bottom: @vertical-margin-small;

  padding-left: @margin-large;
  padding-right: @margin-large;

  overflow: auto;

  div.item {
    font-size: @fontsize-small-paragraph;

    margin-right: @margin-medium;
    color: @pale-sky;
    white-space: nowrap;
    cursor: pointer;
    &.selected {
      color: @gunmetal;
      border-bottom: 2px solid @pelorous;
    }
  }

  @media screen and (min-width: @desktop-min-width) {
    padding-left: @margin-large-desktop;
    padding-right: @margin-large-desktop;

    div.item {
      font-size: @fontsize-paragraph;
    }
  }
}
</style>
