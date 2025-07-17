<!--
#%L
Fishola :: Admin
%%
Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
  <div>
    <b-autocomplete
      v-model="search"
      :data="getOptions()"
      field="label"
      :placeholder="placeholder"
      rounded
      clearable
      clear-on-select
      icon="magnify"
      @select="(option) => (selectOption(option))"
    >
      <template #empty>Aucun résultat</template>
    </b-autocomplete>

    <div class="selection">
      Sélection :
      <span v-for="selected in selectedIds" class="selected">
          {{getItemLabel(selected)}}
          <b-icon
            icon="close"
            size="is-small"
            @click.native="unselectedOption(selected)"
            title="Retirer de la sélection">
          </b-icon>
      </span>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";

@Component
export default class MultipleAutoComplete extends Vue {
  @Prop({default : []}) data: any[];
  @Prop({default : []}) defaultSelection: string[];
  @Prop() placeholder: string;
  search: string = "";
  selectedIds: string[] = [];

  created() {
    this.selectedIds = this.defaultSelection;
    this.$emit("updated", this.selectedIds);
  }

  selectOption(option: any) {
    this.selectedIds.push(option.id);
    this.$emit("updated", this.selectedIds)
  }

  unselectedOption(optionId: string) {
    this.selectedIds = this.selectedIds.filter(item => item !== optionId)
    this.$emit("updated", this.selectedIds)
  }

  getOptions() {
    return this.data.filter((option) => {
      return (
        option.label
          .toString()
          .toLowerCase()
          .indexOf(this.search.toLowerCase()) >= 0
        && !this.selectedIds.includes(option.id)
      );
    });
  }

  getItemLabel(id: string) {
    let filteredItem = this.data.filter((option) => {
      return option.id === id;
    });
    return filteredItem.length == 1 && filteredItem[0].label;
  }
}
</script>
<style lang="less">
.selection {
  min-height: 40px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}
.selected {
  background-color: fade(@pelorous, 30%);
  border-radius: 20px;
  padding: 7px 15px;
  display: inline-block;

  .icon {
    padding: 10px;
    border-radius: 50%;
    cursor: pointer;
    &:hover {
      background: white;
    }
  }
}
</style>
