<!--
  #%L
  Fishola :: Admin
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
<template>
  <div class="catches-to-validate">
    <h1>Prises à valider</h1>
    <b-message type="is-info">
      Prises dont le pêcheur a indiqué une certitude d'identification
      « Probable » ou « Incertain », et qui n'ont pas encore été revues par un
      opérateur. Cliquez sur une ligne pour corriger l'espèce si besoin et la
      marquer comme validée.
    </b-message>
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
      @click="rowClicked"
      per-page="15"
      v-model:current-page="page"
      :striped="true"
      :default-sort="[sortField, sortOrder]"
      :loading="!catches"
      :total="total"
      class="clickable"
    >
      <b-table-column
        v-for="col in columns"
        :field="col.field"
        :label="col.label"
        :key="col.name"
        :sortable="col.sortable"
        :searchable="col.searchable"
        v-slot="props"
      >
        {{ props.row[col.field] }}
      </b-table-column>
    </b-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, Ref } from "vue";

import BackendService from "@/services/BackendService";
import UtilityServices from "@/services/UtilityServices";

import router from "@/router";

const page = ref(1);
const total = ref(0);
const loading = ref(false);
const lastTimerId: Ref<number | undefined> = ref();
const catches = ref([]);
const filters: Ref<any> = ref({});
const sortField = ref("date_de_la_sortie");
const sortOrder = ref("desc");
const columns: any[] = [
  {
    field: "catchId",
    label: "Identifiant",
    searchable: true,
    sortable: true
  },
  {
    field: "dateDeLaSortie",
    label: "Date Sortie",
    searchable: true,
    sortable: true
  },
  {
    field: "especeCapturee",
    label: "Espèce déclarée",
    searchable: true,
    sortable: true
  },
  {
    field: "certitude",
    label: "Certitude",
    searchable: true,
    sortable: true
  },
  {
    field: "poidsDuPoisson",
    label: "Poids du poisson",
    searchable: true,
    sortable: true
  },
  {
    field: "longueurTotaleDuPoisson",
    label: "Taille du poisson",
    searchable: true,
    sortable: true
  }
];

onMounted(() => {
  onSort(sortField.value, sortOrder.value);
});

async function loadData() {
  if (!loading.value) {
    loading.value = true;
    while (catches.value && catches.value.length) {
      catches.value.pop();
    }
    try {
      let url =
        "/v1/trips/catches/pending-validation/" +
        (page.value - 1) +
        "/" +
        sortField.value +
        "/" +
        sortOrder.value;
      url += computeFiltersQueryParameters(filters.value);
      const res = await BackendService.backendGet(url);
      catches.value = res.elements;
      total.value = res.total;
    } catch (e) {
      console.error(e);
    }

    loading.value = false;
  }
}

function onPageChange(p: number) {
  page.value = p;
  loadData();
}

function onSort(field: string, order: string) {
  sortField.value = UtilityServices.camelCaseToUnderscore(field);
  sortOrder.value = order;
  page.value = 1;
  loadData();
}

function onFiltersChange(newFilters: any) {
  filters.value = newFilters;
  page.value = 1;
  loadDataDebounced();
}

/**
 * Waits 500ms calling loadData. If during this delay another call is made, cancels the first call and schedules the second.
 */
function loadDataDebounced() {
  clearTimeout(lastTimerId.value);
  lastTimerId.value = setTimeout(loadData, 450);
}

function computeFiltersQueryParameters(filterObject: any) {
  let url = "";
  let firstKey = true;
  Object.keys(filterObject).forEach(filter => {
    const filteredValue = filterObject[filter].trim();
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
  });
  return url;
}

function rowClicked(row: any) {
  router.push("/catch/" + row.catchId);
}
</script>

<style scoped lang="less">
.clickable {
  cursor: pointer;
}
</style>
