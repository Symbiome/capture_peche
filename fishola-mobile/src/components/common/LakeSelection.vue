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
        Plan<span v-if="allowMultipleSelection">s</span> d'eau
      </label>
      <span class="input-wrapper">
        <input
          id="lakes-autocomplete-input"
          type="text"
          v-model="search"
          inputmode="search"
          autocomplete="off"
          :class="{
            'isSearching' : search.toLowerCase() != selectedLabel.toLowerCase(),
            'field-error' : error
          }"
          v-on:keydown="updateSuggestions"
          v-on:input="updateSearch"
          v-on:focusin="showSuggestions"
          v-on:focusout="closeSuggestions"
        />
        <i
          v-if="!allowMultipleSelection && search != '' &&  (search.toLowerCase() == selectedLabel.toLowerCase())"
          class="icon-error input-delete"
          @click="clearSelection"
          title="Retirer la sélection"
        />
        <span class="input-actions">
          <i class="icon-chevron" @click="toggleSuggestionsDisplay" title="Voir les suggestions" />
          <i class="icon-map" @click="toggleMapDisplay" title="Voir les suggestions sur une carte"/>
        </span>
        <ul class="suggestions" v-show="displaySuggestions">
          <li
            v-for="lake in suggestedFavorites"
            class="favorite"
            :class="selectedLakesId.includes(lake.id) ? 'selected' : ''"
            @click="selectLake(lake)"
            v-html="highlightMatchingText(lake.name)"
          />
          <li
            v-for="lake in suggestedLakes"
            :class="selectedLakesId.includes(lake.id) ? 'selected' : ''"
            @click="selectLake(lake)"
            v-html="highlightMatchingText(lake.name)"
          />
        </ul>
      </span>

      <div class="input-error" :class="error ? 'field-error' : ''">
        <span v-if="error">
          {{ error }}
        </span>
      </div>

      <div v-if="allowMultipleSelection" class="selectedLakes">
        <span v-for="l in selectedLakes">
          {{ l.name }} <i class="icon-error" @click="toggleLake(l)" />
        </span>
      </div>

      <LakesMap
        v-show="displayMap"
        :isVisible="displayMap"
        :lakes="allLakesExecptFavorites"
        :favoriteLakes="favoriteLakes"
        :selectedLake="selectedLakes.length == 1 ? selectedLakes[0] : null"
        @selectLake="selectLakeById"
        v-on:close="toggleMapDisplay"
      />
    </div>
</template>

<script lang="ts">

import { Lake } from '@/pojos/BackendPojos';
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import LakesMap from "@/components/common/LakesMap.vue";
import ReferentialService from '@/services/ReferentialService';

@Component({
  components: {
    LakesMap,
  },
})
export default class LakeSelection extends Vue {
  @Prop() selectedLakes: Lake[];
  @Prop() favoriteLakes: Lake[];
  @Prop({default : ""}) error: string;
  @Prop({default : false}) allowMultipleSelection: boolean;

  displayMap: boolean = false;
  displaySuggestions: boolean = false;
  allLakes: Lake[] = [];
  allLakesExecptFavorites: Lake[] = [];
  suggestedLakes: Lake[] = [];
  suggestedFavorites: Lake[] = [];
  search: string = "";
  selectedLabel: string = "";
  selectedLakesId: string[] = [];

  mounted() {
    this.loadLakes();
  }

  async loadLakes() {
      let favoriteLakes = await ReferentialService.getFavoriteLakes();
      const fetchedLakes = await ReferentialService.getLakes();
      this.allLakes = fetchedLakes;
      this.allLakesExecptFavorites = fetchedLakes.filter(el => {
            return !favoriteLakes.find(el2 => {
                return el.id === el2.id
            })
        });
      this.suggestedLakes = this.allLakesExecptFavorites;
      this.suggestedFavorites = favoriteLakes;

      this.$emit('favoriteLakesChanged', favoriteLakes);
  }

  @Watch("favoriteLakes")
  favoriteLakesChanged() {
    let favorites = this.favoriteLakes;
    this.allLakesExecptFavorites = this.allLakes.filter(lake => {
        return !favorites.find(other => {
            return lake.id === other.id
        })
    });
    this.suggestedLakes = this.allLakesExecptFavorites;
    this.updateSuggestedLakes();
  }

  @Watch("selectedLakes")
  updateSelectedLakeLabel() {
    this.selectedLakesId = this.selectedLakes.map(lake => { return lake.id });
    if (this.selectedLakesId.length == 1 && this.allLakesExecptFavorites) {
      const selectedLake = this.selectedLakesId[0];
      let filteredItem = this.allLakes.filter((l) => {
        return l.id === selectedLake;
      });
      this.selectedLabel = filteredItem.length == 1 ? filteredItem[0].name : '';
      this.updateSuggestedLakes();
    }
  }

  @Watch("search")
  updateSuggestedLakes() {
    this.suggestedLakes = this.search == "" || this.selectedLabel.toLowerCase() == this.search.toLowerCase() ?
      this.suggestedLakes :
      this.allLakesExecptFavorites.filter((lake) => {
        return lake.name
            .toString()
            .toLowerCase()
            .indexOf(this.search.toLowerCase()) >= 0;
    });
    this.suggestedFavorites = this.search == "" || this.selectedLabel.toLowerCase() == this.search.toLowerCase() ?
      this.favoriteLakes :
      this.favoriteLakes.filter((lake) => {
        return lake.name
            .toString()
            .toLowerCase()
            .indexOf(this.search.toLowerCase()) >= 0;
    });
  }

  updateSearch(event: any) {
    /* Because v-model is not updated with mobile keyboard */
    if (event.isComposing) {
      this.search = event.data;
    }
  }

  selectLakeById(id : string) {
    let filteredItem = this.allLakes.filter((l) => {
      return l.id === id;
    });
    if (filteredItem.length == 1 ? filteredItem[0].name : '') {
      this.selectLake(filteredItem[0]);
      if (!this.allowMultipleSelection) {
        this.displayMap = false;
      }
    }
  }

  selectLake(selected: Lake) {
    if (!this.allowMultipleSelection) {
      this.search = selected.name;
    } else {
      this.clearSelection()
    }
    this.displaySuggestions = false;
    this.$emit("updated", selected);
  }

  clearSelection() {
    this.search = "";
    this.$emit("updated", null);
  }

  toggleLake(lake: Lake) {
    this.$emit("updated", lake);
  }

  highlightMatchingText(text) {
    if (this.search != this.selectedLabel) {
      const regexp = new RegExp(RegExp.escape(this.search), 'ig');
      return text.replace(regexp, `<span class="highlight">${this.search}</span>`)
    }
    return text;
  }

  showSuggestions() {
    console.log("showSuggestions")
    if (this.search && (this.search != this.selectedLabel)) {
      this.displaySuggestions = true;
    }
  }

  closeSuggestions(event:Event) {
    // Hide suggestions when leaving the input field, except when clicking on one of the suggestions
    // Timeout to be sure that another event is not dismissed, specially the click on a suggestion
    setTimeout(() => {
      this.displaySuggestions = false;
    }, "500");
  }

  updateSuggestions(event:Event) {
    // Hide suggestions when the Esc or Enter key is pressed
    if (event.keyCode == 27 || event.keyCode == 13) {
      this.displaySuggestions = false;
      if (this.allowMultipleSelection) {
        this.clearSelection();
      }
      // Hide suggestions and select the matching lake, it there is only one, when the Enter key is pressed
      if (event.keyCode == 13 && (this.suggestedLakes.length + this.suggestedFavorites.length) == 1) {
        const selectedLake = this.suggestedLakes.length ? this.suggestedLakes[0] : this.suggestedFavorites[0]
        this.$emit("updated", selectedLake);
        if (!this.allowMultipleSelection) {
          this.search = selectedLake.name;
        }
      }
    } else {
      // Display suggestions when search term is inputted
      this.displaySuggestions = true;
      if (!this.allowMultipleSelection) {
        this.selectedLabel = "";
        this.$emit("updated", null);
      }
    }
  }

  toggleSuggestionsDisplay() {
    this.displayMap = false;
    this.displaySuggestions = !this.displaySuggestions;
  }
  toggleMapDisplay() {
    this.displaySuggestions = false;
    this.displayMap = !this.displayMap;
  }
}
</script>

<style scoped lang="less">
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
      &:hover {
        color: @terra-cotta;
      }
    }
    i.icon-chevron {
      font-size: 12px;
      margin-top: 7px;
    }
  }
  .input-delete {
    position: absolute;
    top:  calc(@vertical-margin-xx-small + 1px);
    right: 80px;
    height: 38px;
    display: flex;
    align-items: center;
    cursor: pointer;
    &:hover {
      color: @terra-cotta;
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
    padding: 0;
    list-style: none;
    background-color: white;
    box-shadow: 0 0 5px #0002;
    z-index: 100;

    & > li {
      padding: 6px 10px 6px 40px;

      &.favorite:before {
        font-family: "Fishola-Icons";
        content: '\f114';
        position: absolute;
        left: 15px;
        padding-top: 2px;
        color: @pelorous;
        font-weight: bold;
      }
      &:hover {
        cursor: pointer;
        background-color: #0001;
      }
      &.selected {
        background-color: #1e9bc422;
      }
    }
  }

  .selectedLakes {
    height: auto;
    display: flex;
    flex: 1;
    flex-wrap: wrap;
    gap: 10px 15px;

    & > span {
      display: flex;
      align-items: center;
      border-radius: 20px;
      border: 1px solid @pelorous;
      padding: 5px 10px;
      i {
        margin-left: 5px;
        cursor: pointer;
        &:hover {
          opacity: 0.5;
        }
      }
    }
  }

  div.input-error {
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
.lake-selection .suggestions .highlight {
  font-weight: bold;
}
</style>