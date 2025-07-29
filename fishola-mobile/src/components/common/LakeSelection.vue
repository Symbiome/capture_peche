<!--
  #%L
  Fishola :: Mobile
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
    <div class="lake-selection">
      <label for="field-lakes">
        Lac
      </label>
      <span class="input-wrapper">
        <input
          id="field-lakes"
          type="text"
          v-model="search"
          :class="{
            'isSearching' : search != getSelectedLabel(),
            'field-error' : error
          }"
          v-on:keydown="displaySuggestions = false"
        />
        <span class="input-actions">
          <i class="icon-chevron" @click="toggleSuggestions()" />
          <i class="icon-lake" />
        </span>
        <ul class="suggestions" v-if="displaySuggestions || search != '' && search != getSelectedLabel()">
          <li
          v-for="(result, i) in getOptions()"
          v-if="i < 11000000"
          :class="result.id == selectedId ? 'selected' : ''"
          @click="selectOption(result)"
          v-html="highlightText(result.name)"
          >
        </li>
      </ul>
    </span>

      <div :class="error ? 'field-error' : ''">
        <span v-if="error">
          {{ error }}
        </span>
      </div>
    </div>
</template>

<script lang="ts">

import { Lake } from '@/pojos/BackendPojos';
import TripMeta from "@/pojos/TripMeta";
import FormInput from "@/components/common/FormInput.vue";
import FormSelect from "@/components/common/FormSelect.vue";
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';

@Component({
  components: {
    FormInput,
    FormSelect,
  },
})
export default class LakeSelection extends Vue {
  @Prop() lakes: Lake[];
  @Prop() selectedId: string;
  @Prop() error: string = "";
  search: string = "";
  displaySuggestions: boolean = false;

  mounted() {
  }

  @Watch("search")
  getOptions() {
    return this.search == "" ? 
      this.lakes :
      this.lakes.filter((lake) => {
        return lake.name
            .toString()
            .toLowerCase()
            .indexOf(this.search.toLowerCase()) >= 0;
    });
  }

  // @Watch("selectedId")
  getSelectedLabel() {
    console.log("getSelectedLabel")
    let filteredItem = this.lakes.filter((option) => {
      return option.id === this.selectedId;
    });
    return filteredItem.length == 1 && filteredItem[0].name;
  }

  selectOption(selected: Lake) {
    this.search = selected.name;
    this.displaySuggestions = false;
    this.$emit("updated", selected.id);
  }

  highlightText(text) {
    const regexp = new RegExp(this.search, 'ig');
    return text.replace(regexp, `<span class="highlight">${this.search}</span>`)
  }

  toggleSuggestions() {
    if (!this.displaySuggestions) {
      this.search = "";
    }
    this.displaySuggestions = !this.displaySuggestions;
  }

}
</script>

<style scoped lang="less">
@import "../../less/main";

.lake-selection {
  position: relative;
  margin-top: @vertical-margin-x-small;

  font-size: @fontsize-form-input;
  line-height: calc(@fontsize-form-input + @line-height-padding-medium);

  display: flex;
  flex-direction: column;
  align-items: flex-start;

  label {
    font-weight: 300;
    color: @black;
  }

  .input-wrapper {
    width: 100%;
    position: relative;
  }

  .input-actions {
    margin-top: @vertical-margin-xx-small;
    position: absolute;
    top: 0;
    right: 0;
    display: flex;
    align-items: center;
    justify-content: space-around;
    background: #eee;
    border-radius: 0 4px 4px 0;
    border: 1px solid @pale-sky;
    width: 70px;
    height: 38px;
    padding: 0 10px;
    gap: 10px;
  }

  input {
    padding-left: @margin-small;
    padding-right: 70px;
    margin-top: @vertical-margin-xx-small;
    width: 100%;
    height: 38px;
    border-radius: 4px;

    background: transparent;
    border: 1px solid @pale-sky;

    color: @gunmetal;
    font-size: @fontsize-form-input;
    font-family: 'Open Sans', sans-serif;

    &.isSearching {
      font-style: italic;
      font-weight: normal;
      font-size: @fontsize-form-input;
      color: @pale-sky;
    }
    &.field-error {
      border: 1px solid @cardinal !important;
    }
  }

  .suggestions {
    position: absolute;
    top: 100%;
    left: 0;
    min-width: 100%;
    max-height: 250px;
    overflow-y: auto;
    margin: 0;
    padding: 10px 20px;
    list-style: none;
    background-color: white;
    box-shadow: 0 0 5px #0002;

    & > li {
      padding: 5px;

      &:hover {
        cursor: pointer;
        background-color: #0001;
      }
    }
  }
  div {
    height: calc(@fontsize-form-error + @line-height-padding-medium);
  }
  div.field-error {
    background-color: transparent;
    color: @cardinal;
    font-size: @fontsize-form-error;
    line-height: calc(@fontsize-form-error + @line-height-padding-medium);
  }
  @media screen and (min-width: @desktop-min-width) {
    font-size: @fontsize-form-input-desktop;
    line-height: calc(@fontsize-form-input-desktop + @line-height-padding-medium);

    input {
      font-size: @fontsize-form-input-desktop;
      height: 42px;

      &::placeholder {
        font-size: @fontsize-form-input-desktop;
      }

    }
    .input-actions {
      height: 42px;
    }
  }
}

</style>
<style  lang="less">
.highlight {
  font-weight: bold;
}
.selected {
  color: red;
}
</style>