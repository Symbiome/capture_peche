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
      backend-sorting
      pagination-simple
      @page-change="onPageChange"
      @sort="onSort"
      per-page="15"
      :current-page.sync="offset"
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
          sortable
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
  offset = 1;
  total = 0;
  catches = [];
  selection = { item: null };
  speciesIdMap = new Map<string, string>();
  sortField = "size";
  sortOrder = "desc";
  columns: any[] = [
    {
      field: "id",
      label: "Identifiant"
    },
    {
      field: "speciesId",
      label: "Espèce"
    },
    {
      field: "editedSpeciesId",
      label: "Espèce corrigée"
    },
    {
      field: "size",
      label: "Taille"
    },
    {
      field: "editedSize",
      label: "Taille corrigée"
    },
    {
      field: "weight",
      label: "Poids"
    },
    {
      field: "editedWeight",
      label: "Poids corrigé"
    },
    {
      field: "excludeFromExport",
      label: "Exclure des exports",
      isABoolean: true
    }
  ];

  mounted() {
    this.onSort("size", "desc");
  }

  @Watch("offset")
  async loadData() {
    while (this.catches && this.catches.length) {
      this.catches.pop();
    }
    try {
      if (this.speciesIdMap.size == 0) {
        let species = await BackendService.backendGet(
          "/v1/referential/raw-species"
        );
        species.forEach((specie: { id: string; name: string }) => {
          this.speciesIdMap.set(specie.id, specie.name);
        });
      }
      let res = await BackendService.backendGet(
        "/v1/referential/catches/" +
          this.offset +
          "/" +
          this.sortField +
          "/" +
          this.sortOrder
      );
      this.catches = res.elements;
      this.total = res.total;
    } catch (e) {
      console.error(e);
    }
  }

  formatDate(puet: number[]): string {
    return UtilityServices.formatDate(puet);
  }

  onPageChange(page: number) {
    this.offset = page;
    this.loadData();
  }

  onSort(field: string, order: string) {
    this.sortField = UtilityServices.camelCaseToUnderscore(field);
    this.sortOrder = order;
    this.loadData();
  }
}
</script>

<style scoped lang="less">
@import "../../less/main";
</style>
