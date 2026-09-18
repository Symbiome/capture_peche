<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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
<!--
  Sélecteur plan d'eau + année d'un tableau de bord (#175). Le plan d'eau se
  choisit par favoris + recherche serveur (comme LakeSelection.vue) : le
  référentiel national complet (~181k entités, cf. #126/#128) n'est JAMAIS
  chargé ni rendu ici, sous peine de RangeError (pile de rendu Vue dépassée).
  -->
<template>
    <div class="selects-holder">
        <span class="lake-select-wrapper">
            <input
              id="lake-and-year-search-input"
              type="text"
              class="lake-search-input"
              v-model="search"
              inputmode="search"
              autocomplete="off"
              placeholder="Plan d'eau"
              v-on:input="updateSearchInput"
              v-on:focusin="showSuggestions"
              v-on:focusout="closeSuggestions"
            />
            <ul class="lake-suggestions" v-show="displaySuggestions">
                <li
                  v-for="lake in suggestedFavorites"
                  :key="'fav-' + lake.id"
                  class="favorite"
                  :class="lake.id === selectedLakeId ? 'selected' : ''"
                  @click="selectLake(lake)"
                >
                    {{ lake.name }}
                </li>
                <li
                  v-for="lake in suggestedLakes"
                  :key="lake.id"
                  :class="lake.id === selectedLakeId ? 'selected' : ''"
                  @click="selectLake(lake)"
                >
                    {{ lake.name }}
                </li>
            </ul>
        </span>
        <select placeholder="Année" v-model="selectedYear" v-if="showYears">
            <option v-for="year in years" :value="year" :key="'year_' + year">
                {{ year }}
            </option>
        </select>
    </div>
</template>

<script lang="ts">

import { WaterEntity as Lake } from '@/pojos/BackendPojos';
import ReferentialService from '@/services/ReferentialService';
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';

@Component({
  components: {
  }
})
export default class LakeAndYearSelection extends Vue {
  @Prop() years: number[];
  @Prop() showYears: boolean;

  selectedYear = 0;
  selectedLakeId = "";
  favoriteLakes: Lake[] = [];
  suggestedFavorites: Lake[] = [];
  suggestedLakes: Lake[] = [];
  search: string = "";
  selectedLabel: string = "";
  displaySuggestions = false;
  doneLoading = false;

  private searchSeq: number = 0;
  private searchTimer: any = null;

  mounted() {
    const query = this.$route.query;
    if (this.years && this.years.length > 0) {
      const yearFromURL = parseInt(query.currentYear as string);
      if (yearFromURL && this.years.indexOf(yearFromURL) > -1) {
        this.selectedYear = yearFromURL;
      } else {
        this.selectedYear = this.years[0];
      }
    }
    this.loadFavorites();
  }

  beforeDestroy() {
    // Évite qu'un debounce en vol ne déclenche une requête et ne mute l'état
    // d'un composant détruit.
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
  }

  async loadFavorites(): Promise<void> {
    try {
      this.favoriteLakes = await ReferentialService.getFavoriteLakes();
    } catch (e) {
      // Silent catch : les favoris sont un confort, pas un blocant.
      console.error(e);
    }
    this.suggestedFavorites = this.favoriteLakes;

    const query = this.$route.query;
    let initialId = "";
    if (query.lakeId) {
      initialId = query.lakeId as string;
    } else if (localStorage && localStorage.latestSelectedLakeUUID && localStorage.latestSelectedLakeUUID !== "all") {
      initialId = localStorage.latestSelectedLakeUUID;
    } else if (this.favoriteLakes.length > 0) {
      initialId = this.favoriteLakes[0].id;
    }

    if (initialId) {
      await this.resolveAndApplySelection(initialId, false);
    }

    this.$emit("lake-and-year", { lake: this.selectedLakeId, year: this.selectedYear });
    this.doneLoading = true;
  }

  // Retrouve le libellé d'un plan d'eau connu par avance (URL, historique
  // local) sans jamais charger le référentiel national complet (#175) : on
  // cherche d'abord parmi les favoris déjà en mémoire, sinon on résout par id.
  private async resolveAndApplySelection(lakeId: string, persistAndEmit: boolean): Promise<void> {
    const known = this.favoriteLakes.find((l) => l.id === lakeId);
    if (known) {
      this.applySelection(known, persistAndEmit);
      return;
    }
    try {
      const resolved = await ReferentialService.getWaterEntityById(lakeId);
      if (resolved) {
        this.applySelection(resolved, persistAndEmit);
        return;
      }
    } catch (e) {
      // Silent catch : on retombe sur l'id seul ci-dessous.
    }
    this.selectedLakeId = lakeId;
    if (persistAndEmit) {
      this.persistSelection();
    }
  }

  private applySelection(lake: Lake, persistAndEmit: boolean) {
    this.selectedLakeId = lake.id;
    this.search = lake.name;
    this.selectedLabel = lake.name;
    if (persistAndEmit) {
      this.persistSelection();
    }
  }

  private persistSelection() {
    localStorage.latestSelectedLakeUUID = this.selectedLakeId ? this.selectedLakeId : "all";
    if (this.selectedLakeId !== this.$route.query.lakeId) {
      this.$router.replace({
        query: {
          ...this.$route.query,
          lakeId: this.selectedLakeId,
        }
      });
    }
    if (this.doneLoading) {
      this.$emit("lake", this.selectedLakeId);
    }
  }

  selectLake(lake: Lake) {
    this.applySelection(lake, true);
    this.displaySuggestions = false;
  }

  @Watch("$route")
  routeUpdated() {
    const query = this.$route.query;
    if (query.lakeId && query.lakeId !== this.selectedLakeId) {
      this.resolveAndApplySelection(query.lakeId as string, false);
    }
  }

  @Watch("search")
  updateSuggestedLakes() {
    const term = this.search;
    const isSelectedLabel = this.selectedLabel.toLowerCase() === term.toLowerCase();

    // Pas de recherche active (champ vide ou = libellé déjà sélectionné) : on
    // ne propose que les favoris. La liste nationale complète (~181k
    // entités, cf. #126) ne doit jamais être rendue en une fois dans le
    // <li v-for>, sous peine de RangeError ; la recherche serveur ci-dessous
    // prend le relais dès 2 caractères.
    if (term === "" || isSelectedLabel) {
      this.suggestedLakes = [];
      this.suggestedFavorites = this.favoriteLakes;
      return;
    }

    const lowered = term.toLowerCase();
    this.suggestedFavorites = this.favoriteLakes.filter((lake) =>
      lake.name.toString().toLowerCase().indexOf(lowered) >= 0);

    if (term.trim().length < 2) {
      this.suggestedLakes = [];
      return;
    }

    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    this.searchTimer = setTimeout(() => {
      const seq = ++this.searchSeq;
      ReferentialService.searchWaterEntities(term.trim())
        .then((results) => {
          if (seq === this.searchSeq) {
            this.suggestedLakes = results;
          }
        })
        .catch(() => { /* recherche en échec : on conserve l'état courant */ });
    }, 250);
  }

  updateSearchInput(event: any) {
    /* Because v-model is not updated with mobile keyboard */
    if (event.isComposing) {
      this.search = event.data;
    }
  }

  showSuggestions() {
    this.displaySuggestions = true;
  }

  closeSuggestions() {
    // Cache les suggestions en quittant le champ, sauf clic sur une suggestion
    // (timeout pour laisser cet évènement passer avant).
    setTimeout(() => {
      this.displaySuggestions = false;
    }, 500);
  }

  @Watch("selectedYear")
  yearChanged() {
    if (""+ this.selectedYear !== this.$route.query.currentYear) {
      this.$router.replace({
        query: {
          ...this.$route.query,
          currentYear: ""+this.selectedYear,
        }
      });
    }
    if (this.doneLoading) {
      this.$emit("year", this.selectedYear);
    }
  }
}
</script>

<style scoped lang="less">
.selects-holder {
  display: flex;
  align-items: center;

  select {
    background: transparent;
    padding: 0 10px;
    min-height: 35px;
    border: 1px solid @pelorous;
    border-radius: 20px;
    margin-left: 10px;
    max-width: 150px;
    font-weight: bold;
    font-size: 16px;
    font-family: inherit;
    color: @pelorous;
    cursor: pointer;

    option {
      color: black;
    }

    &:hover {
      background-color: white;
    }
  }

  .lake-select-wrapper {
    position: relative;
    display: inline-block;
  }

  .lake-search-input {
    background: transparent;
    padding: 0 10px;
    min-height: 35px;
    border: 1px solid @pelorous;
    border-radius: 20px;
    margin-left: 10px;
    width: 150px;
    font-weight: bold;
    font-size: 16px;
    font-family: inherit;
    color: @pelorous;

    &:hover {
      background-color: white;
    }
  }

  .lake-suggestions {
    position: absolute;
    top: 100%;
    left: 10px;
    min-width: 220px;
    max-height: 250px;
    overflow-y: auto;
    margin: 4px 0 0;
    padding: 0;
    list-style: none;
    background-color: white;
    box-shadow: 0 0 5px #0002;
    z-index: 500;
    text-align: left;

    & > li {
      padding: 6px 10px;
      color: @gunmetal;
      font-weight: normal;

      &.favorite:before {
        font-family: "Fishola-Icons";
        content: '\f114';
        margin-right: 6px;
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
}

@media screen and (min-width: @desktop-min-width) {
    .selects-holder {
        margin-left: 20px;
        // margin-top: -10px;
    }
}

@media screen and (max-width: 1180px) {
    .selects-holder {
        margin-left: 0px;
        margin-top: 0px;
        margin-bottom: 20px;
    }
}

.show-all-lakes {
    font-size: 14px;
    padding-left: 10px;
}

</style>
