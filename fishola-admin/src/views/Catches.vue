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
    <h1>Sorties</h1>
    <b-message type="is-info">
      Liste de toutes les prises enregistrées sur Fishola (hors prises de
      l'équipe Fishola exclues d'office). <br />Vous pouvez filtrer et trier
      selon chaque champ. Seules les lignes non exclues de l'export seront
      exportées dans le fichier CSV et incluses dans les tableaux de bords
      globaux.
    </b-message>
    <a @click="exportCsv">
      <button class="button is-primary">Lancer un export CSV</button></a>
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
      <template v-slot:default="props">
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
          @click="showLink($event, props.row[col.field])"
          :key="col.name"
          sortable
        >
          <span v-if="col.isUrl">
            {{ props.row[col.field] }}
          </span>
          <button class="button is-small is-info">
            <b-icon
              icon="eye"
              size="is-small"
            ></b-icon>
          </button>
        </b-table-column>
      </template>
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
const sortField = ref("size");
const sortOrder = ref("desc");
const columns: any[] = [
  {
    field: "catchId",
    label: "Identifiant",
    searchable: true,
    sortable: true
  },
  {
    field: "nomDeLaPlateforme",
    label: "Plateforme",
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
    field: "typeDePeche",
    label: "Type de pêche",
    searchable: true,
    sortable: true
  },
  {
    field: "especeCapturee",
    label: "Espèce Capturée",
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
  },
  {
    field: "longueurTotaleDuPoissonCalculee",
    label: "Mesure automatique",
    searchable: true,
    sortable: true
  },
  {
    field: "aExclure",
    label: "Exclure de l'export",
    searchable: true,
    sortable: true
  }
];

onMounted(() => {
  onSort("date_de_la_sortie", "desc");
});

async function loadData() {
  if (!loading.value) {
    loading.value = true;
    while (catches.value && catches.value.length) {
      catches.value.pop();
    }
    try {
      // Load catches with current pagination, sort and filters
      let url =
        "/v1/trips/export/" +
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

function formatDate(puet: number[]): string {
  return UtilityServices.formatDate(puet);
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

function onFiltersChange(filters: any) {
  filters.value = filters;
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

/**
 * Returns a query parameters url corresponding to the given filter object
 * e.g. {
 *   'weight': '42',
 *   'speciesId': 'Perche'
 * }
 * will return '?weight=42&speciesId='
 */
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

async function exportCsv() {
  const csvContent = await BackendService.backendGetRaw("/v1/trips/export");
  const hiddenElement = document.createElement("a");
  hiddenElement.href = "data:text/csv;charset=utf-8," + encodeURI(csvContent);
  hiddenElement.target = "_blank";
  const d = new Date();
  const mm = d.getMonth() + 1;
  const dd = d.getDate();
  const dateString =
    d.getFullYear() +
    "-" +
    (mm > 9 ? "" : "0") +
    mm +
    "-" +
    (dd > 9 ? "" : "0") +
    dd;
  hiddenElement.download = "Fishola_Export_" + "_" + dateString + ".csv";
  hiddenElement.click();
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
