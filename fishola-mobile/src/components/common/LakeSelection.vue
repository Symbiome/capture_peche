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
      <label for="lakes-autocomplete-input">
        Lac
      </label>
      <span class="input-wrapper">
        <input
          id="lakes-autocomplete-input"
          type="text"
          v-model="search"
          :class="{
            'isSearching' : search.toLowerCase() != selectedLabel.toLowerCase(),
            'field-error' : error
          }"
          v-on:keydown="(event) => updateSuggestions(event)"
          v-on:focusout="(event) => closeSuggestions(event)"
        />
        <span class="input-actions">
          <i class="icon-chevron" @click="toggleSuggestions()" />
          <i class="icon-lake" @click="displayMap = !displayMap"/>
        </span>
        <ul class="suggestions" v-show="displaySuggestions">
          <li
            v-for="option in options"
            :class="option.id == selectedId ? 'selected' : ''"
            @click="selectOption(option)"
            v-html="highlightMatchingText(option.name)"
          />
        </ul>
      </span>

      <div :class="error ? 'field-error' : ''">
        <span v-if="error">
          {{ error }}
        </span>
      </div>
      <LakesMap
        v-if="displayMap"
        :lakes="lakes"
        class="modal"
        style="width: 100%; height: 500px"
        @selectLake="selectLakeById"
      />
    </div>
</template>

<script lang="ts">

import { Lake } from '@/pojos/BackendPojos';
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import LakesMap from "@/components/common/LakesMap.vue";

@Component({
  components: {
    LakesMap,
  },
})
export default class LakeSelection extends Vue {
  @Prop() lakes: Lake[];
  @Prop() selectedId: string;
  @Prop({default : ""}) error: string;

  displayMap: boolean = false;
  displaySuggestions: boolean = false;
  options: Lake[] = [];
  search: string = "";
  selectedLabel: string = "";

  mounted() {
    this.options = this.lakes;
  }

  @Watch("selectedId")
  updateSelectedLakeLabel() {
    let filteredItem = this.lakes.filter((option) => {
      return option.id === this.selectedId;
    });
    this.selectedLabel = filteredItem.length == 1 ? filteredItem[0].name : '';
    this.updateOptions();
  }

  @Watch("search")
  updateOptions() {
    this.options = this.search == "" || this.selectedLabel.toLowerCase() == this.search.toLowerCase() ?
      this.lakes :
      this.lakes.filter((lake) => {
        return lake.name
            .toString()
            .toLowerCase()
            .indexOf(this.search.toLowerCase()) >= 0;
    });
  }

  selectLakeById(id : string) {
    console.log("selectLakeById", id);
    let filteredItem = this.lakes.filter((option) => {
      return option.id === id;
    });
    if (filteredItem.length == 1 ? filteredItem[0].name : '') {
      this.selectOption(filteredItem[0]);
      this.displayMap = false;
    }
  }

  selectOption(selected: Lake) {
    this.search = selected.name;
    this.displaySuggestions = false;
    this.$emit("updated", selected.id);
  }

  highlightMatchingText(text) {
    if (this.search != this.selectedLabel) {
      const regexp = new RegExp(RegExp.escape(this.search), 'ig');
      return text.replace(regexp, `<span class="highlight">${this.search}</span>`)
    }
    return text;
  }

  closeSuggestions(event:Event) {
    // Hide suggestions when leaving the input field, except when clicking on one of the suggestions
    if (event.type === 'focusout' && !event.target.parentElement.contains(event.rangeParent)) {
      this.displaySuggestions = false;
    }
  }

  updateSuggestions(event:Event) {
    // Hide suggestions when the Esc or Enter key is pressed
    if (event.keyCode == 27 || event.keyCode == 13) {
      this.displaySuggestions = false;
      // Hide suggestions and select the matching lake when the Enter key is pressed
      if (event.keyCode == 13 && this.options.length == 1) {
        this.$emit("updated", this.options[0].id);
        this.search = this.options[0].name;
      }
    } else {
      // Display suggestions when search term is inputted
      this.displaySuggestions = true;
      if (this.selectedId) {
        this.$emit("updated", null);
      }
    }
  }

  toggleSuggestions() {
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

    i {
      cursor: pointer;
    }
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
.modal {
  position: absolute;
  z-index: 1500;
  top: 100%;
  left: 0;
  width: 100%;
  height: 500px !important;
  background-color: @black-alpha-90;
  transition: opacity 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
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