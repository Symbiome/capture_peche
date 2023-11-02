<!--
  #%L
  Fishola :: Admin
  %%
  Copyright (C) 2019 - 2023 INRAE - UMR CARRTEL
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
  <div class="catches">
    <b-table
      :data="catches"
      paginated
      backend-pagination
      backend-filtering
      backend-sorting
      pagination-simple
      @page-change="onPageChange"
      @filters-change="onFiltersChange"
      @sort="onSort"
      per-page="15"
      :current-page.sync="page"
      :striped="true"
      :default-sort="[sortField, sortOrder]"
      :loading="!catches"
      :total="total"
    >
      <template slot-scope="props">
        <b-table-column
          v-for="col in columns.filter(
            col =>
              col.visible !== false &&
              !col.isUrl &&
              !col.isFile &&
              !col.isHTML &&
              !col.isPicture
          )"
          :field="col.field"
          :label="col.label"
          :key="col.name"
          :sortable="col.sortable"
          :searchable="col.searchable"
        >
          <span v-if="col.isABoolean && props.row[col.field]">
            Oui
          </span>
          <span v-else-if="col.isABoolean && !props.row[col.field]">
            Non
          </span>
          <span v-else-if="col.field.indexOf('peciesId') > -1">
            {{ speciesIdMap.get(props.row[col.field]) }}
          </span>
          <span v-else-if="col.isADate && props.row[col.field]">
            {{ formatDate(props.row[col.field]) }}
          </span>
          <span v-else> {{ props.row[col.field] }} </span>
        </b-table-column>
        <b-table-column
          v-for="col in columns.filter(
            col =>
              col.visible !== false &&
              (col.isUrl || col.isFile || col.isPicture)
          )"
          :field="col.field"
          :label="col.label"
          @click.native="showLink($event, props.row[col.field])"
          :key="col.name"
          sortable
        >
          <span v-if="col.isUrl">
            {{ props.row[col.field] }}
          </span>
          <button class="button is-small is-info">
            <b-icon icon="eye" size="is-small"></b-icon>
          </button>
        </b-table-column>
      </template>
    </b-table>
  </div>
</template>

<script lang="ts">
import Referential from "@/components/Referential.vue";

import { Component, Vue, Watch } from "vue-property-decorator";
import BackendService from "@/services/BackendService";
import UtilityServices from "@/services/UtilityServices";

@Component({
  components: {
    Referential
  }
})
export default class LakesVue extends Vue {
  page = 1;
  total = 0;
  loading = false;
  lastTimerId: number;
  catches = [];
  filters: any = {};
  selection = { item: null };
  speciesIdMap = new Map<string, string>();
  sortField = "size";
  sortOrder = "desc";
  columns: any[] = [
    {
      field: "id",
      label: "Identifiant",
      searchable: true,
      sortable: true
    },
    {
      field: "speciesId",
      label: "Espèce",
      searchable: true
    },
    {
      field: "editedSpeciesId",
      label: "Espèce corrigée",
      searchable: true
    },
    {
      field: "size",
      label: "Taille",
      searchable: true,
      sortable: true
    },
    {
      field: "editedSize",
      label: "Taille corrigée",
      searchable: true,
      sortable: true
    },
    {
      field: "weight",
      label: "Poids",
      searchable: true,
      sortable: true
    },
    {
      field: "editedWeight",
      label: "Poids corrigé",
      searchable: true,
      sortable: true
    },
    {
      field: "excludeFromExport",
      label: "Exclure des exports",
      isABoolean: true,
      searchable: true,
      sortable: true
    }
  ];

  mounted() {
    this.onSort("size", "desc");
  }

  async loadData() {
    if (!this.loading) {
      this.loading = true;
      while (this.catches && this.catches.length) {
        this.catches.pop();
      }
      try {
        // Step 1 :load species referential if not already loaded
        if (this.speciesIdMap.size == 0) {
          let species = await BackendService.backendGet(
            "/v1/referential/raw-species"
          );
          species.forEach((specie: { id: string; name: string }) => {
            this.speciesIdMap.set(specie.id, specie.name);
          });
        }
        // Step 2 : load catches with current pagination, sort and filters
        let url =
          "/v1/referential/catches/" +
          (this.page - 1) +
          "/" +
          this.sortField +
          "/" +
          this.sortOrder;
        url += this.computeFiltersQueryParameters(this.filters);
        let res = await BackendService.backendGet(url);
        this.catches = res.elements;
        this.total = res.total;
      } catch (e) {
        console.error(e);
      }

      this.loading = false;
    }
  }

  formatDate(puet: number[]): string {
    return UtilityServices.formatDate(puet);
  }

  onPageChange(page: number) {
    this.page = page;
    this.loadData();
  }

  onSort(field: string, order: string) {
    this.sortField = UtilityServices.camelCaseToUnderscore(field);
    this.sortOrder = order;
    this.page = 1;
    this.loadData();
  }

  onFiltersChange(filters: any) {
    this.filters = filters;
    this.page = 1;
    this.loadDataDebounced();
  }

  /**
   * Waits 500ms calling loadData. If during this delay another call is made, cancels the first call and schedules the second.
   */
  loadDataDebounced() {
    clearTimeout(this.lastTimerId);
    this.lastTimerId = setTimeout(this.loadData, 450);
  }

  /**
   * Returns a query parameters url corresponding to the given filter object
   * e.g. {
   *   'weight': '42',
   *   'speciesId': 'Perche'
   * }
   * will return '?weight=42&speciesId='
   */
  computeFiltersQueryParameters(filterObject: any) {
    let url = "";
    let firstKey = true;
    Object.keys(filterObject).forEach(filter => {
      let filteredValue = filterObject[filter].trim();
      if (filteredValue) {
        let column = this.columns.find(c => {
          return c.field == filter;
        });
        // Boolean column : replace "oui" by 'true', otherwise false
        if (column?.isABoolean) {
          if (filteredValue.toLowerCase() == "oui") {
            filteredValue = "true";
          } else if (filteredValue.toLowerCase() == "non") {
            filteredValue = "false";
          }
        }
        // Species column : get the id matching the specie
        if (column?.field.indexOf("peciesId") > -1) {
          for (let [specieId, specieName] of this.speciesIdMap.entries()) {
            console.error(
              `${specieName.toLowerCase()}.indexOf(${filteredValue.toLowerCase()}) = specieName.toLowerCase().indexOf(filteredValue.toLowerCase()`
            );
            if (
              specieName.toLowerCase().indexOf(filteredValue.toLowerCase()) == 0
            ) {
              filteredValue = specieId;
              break;
            }
          }
        }
        if (filteredValue) {
          if (firstKey) {
            url += "?";
            firstKey = false;
          } else {
            url += "&";
          }
          url +=
            UtilityServices.camelCaseToUnderscore(filter) + "=" + filteredValue;
        }
      }
    });
    return url;
  }
}
</script>

<style scoped lang="less">
@import "../../less/main";
</style>
