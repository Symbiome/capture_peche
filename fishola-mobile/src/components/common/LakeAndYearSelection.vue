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
    <div class="selects-holder">
        <select placeholder="lake" v-model="selectedLakeUUID">
            <option v-for="lake in lakes" :value="lake.id" :key="lake.uuid">
                {{ lake.name }}
            </option>
            </select>
            <select placeholder="Année" v-model="selectedYear" v-if="showYears">
            <option v-for="year in years" :value="year" :key="'year_' + year">
                {{ year }}
            </option>
        </select>

    <span class="show-all-lakes" @click="showAllLakes = !showAllLakes">
        <span v-if="showAllLakes">Seulement mes lacs favoris</span>
        <span v-else>Voir tous les lacs</span>
    </span>
    </div>
</template>

<script lang="ts">

import { Lake } from '@/pojos/BackendPojos';
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
  selectedLakeUUID = "";
  lakes: Lake[] = [];
  showAllLakes = false;
  doneLoading = false;

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
    if (localStorage && localStorage.showAllLakes) {
      this.showAllLakes = localStorage.showAllLakes === "true";
    }
    if (query.lakeId) {
      this.selectedLakeUUID = query.lakeId as string;
    }
    this.loadLakes();
  }

  
  @Watch("showAllLakes")
  async loadLakes(): Promise<void> {
    try {
        if (this.showAllLakes)  {
            this.lakes = await ReferentialService.getLakes();
        } else {
            this.lakes = await ReferentialService.getFavoriteLakes();
        }
    } catch (e) {
        // Silent catch
        console.error(e);
    }
    const query = this.$route.query;
    if (query.lakeId) {
      this.selectedLakeUUID = query.lakeId as string;
    } else {
      if (localStorage && localStorage.latestSelectedLakeUUID && localStorage.latestSelectedLakeUUID != "all") {
        if (this.lakes.some(l => l.id === localStorage.latestSelectedLakeUUID)) {
          this.selectedLakeUUID = localStorage.latestSelectedLakeUUID
        } else {
          this.selectedLakeUUID = this.lakes[0].id;
        }
      } else if (this.lakes && this.lakes.length > 0) {
          this.selectedLakeUUID = this.lakes[0].id;
      }
    }
    this.$emit("lake-and-year", {lake: this.selectedLakeUUID, year: this.selectedYear});
    this.doneLoading = true; 
  }

  @Watch("selectedLakeUUID")
  selectedLakeUUIDChanged() {
    if (this.selectedLakeUUID) {
      localStorage.latestSelectedLakeUUID = this.selectedLakeUUID
    } else {
      localStorage.latestSelectedLakeUUID = "all"
    }
    if (this.selectedLakeUUID !== this.$route.query.lakeId) {
      this.$router.replace({
        query: {
          ...this.$route.query,
          lakeId: this.selectedLakeUUID,
        }
      });
    }
    if (this.doneLoading) {
      this.$emit("lake", this.selectedLakeUUID);
    }
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

  @Watch("showAllLakes")
  showAllLakesChanged() {
    localStorage.showAllLakes = this.showAllLakes;
  }

}
</script>

<style scoped lang="less">
@import "../../less/main";

.selects-holder {
  select {
    background-color: white;
    padding: 10px;
    height: 40px;
    border: 1px solid @pale-sky;
    border-radius: 3px;
    margin-left: 10px;
    max-width: 150px;
  }
}

@media screen and (min-width: @desktop-min-width) {
    .selects-holder {
        margin-left: 40px;
        margin-top: -10px;
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
