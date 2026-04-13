<!--
  #%L
  Fishola :: Admin
  %%
  Copyright (C) 2019 - 2022 INRAE - UMR CARRTEL
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
    <div class="referential metrics-container">
      <h1 class="metrics-super-title">
        Chiffres clés
        <b-button
          type="is-primary"
          @click="exportAllAsCSV()"
        >Tout exporter en csv</b-button>
      </h1>

      <div class="metrics-container">
        <h2 class="metrics-title is-primary">
          Nombre d'utilisateurs actifs (au moins une sortie dans l'année)
          <b-button
            type="is-primary"
            @click="
              exportAsCSV(
                'utilisateurs',
                activeUsersColumns,
                metrics.activeUsersPerYear
              )
              "
          >Exporter en csv</b-button>
        </h2>
        <b-table
          :data="metrics.activeUsersPerYear"
          :columns="activeUsersColumns"
          :default-sort="['lac', 'asc']"
        />

        <h2
          class="metrics-title"
          v-if="loggedAdmin.isNationalAdmin"
        >
          Nombre d'inscriptions par an
          <b-button
            type="is-primary"
            @click="
              exportAsCSV(
                'inscriptions',
                userRegistrationsColumns,
                metrics.userRegistrationsPerYear
              )
              "
          >Exporter en csv</b-button>
        </h2>
        <b-table
          v-if="loggedAdmin.isNationalAdmin"
          :data="metrics.userRegistrationsPerYear"
          :columns="userRegistrationsColumns"
          :default-sort="['annee', 'asc']"
        />

        <h2 class="metrics-title">
          Nombre de sorties par plan d'eau et par an
          <b-button
            type="is-primary"
            @click="
              exportAsCSV('sorties', tripsPerLakeColumns, metrics.tripsPerLake)
              "
          >Exporter en csv</b-button>
        </h2>
        <b-table
          :data="metrics.tripsPerLake"
          :columns="tripsPerLakeColumns"
          :default-sort="['lac', 'asc']"
        />

        <h2 class="metrics-title">
          Nombre de captures par plan d'eau et par an
          <b-button
            type="is-primary"
            @click="
              exportAsCSV(
                'captures',
                catchesPerLakeColumns,
                metrics.catchesPerLake
              )
              "
          >Exporter en csv</b-button>
        </h2>
        <b-table
          :data="metrics.catchesPerLake"
          :columns="catchesPerLakeColumns"
          :default-sort="['lac', 'asc']"
        />
        <h2
          class="metrics-title"
          v-if="loggedAdmin.isNationalAdmin"
        >
          Nombre de mesures automatiques par plan d'eau et par an
          <b-button
            type="is-primary"
            @click="
              exportAsCSV(
                'mesures',
                automaticMeasuresPerLakeColumns,
                metrics.automaticMeasuresPerLake
              )
              "
          >Exporter en csv</b-button>
        </h2>
        <b-table
          v-if="loggedAdmin.isNationalAdmin"
          :data="metrics.automaticMeasuresPerLake"
          :columns="automaticMeasuresPerLakeColumns"
          :default-sort="['lac', 'asc']"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

import BackendService from "@/services/BackendService";

class Column {
  field: string;
  label: string;
}

const url = "/v1/metrics";
const loggedAdmin = ref({ isNationalAdmin: false });
const metrics = ref({
  activeUsersPerYear: [],
  userRegistrationsPerYear: [],
  tripsPerLake: [],
  catchesPerLake: [],
  automaticMeasuresPerLake: []
});
const activeUsersColumns = [
  { field: "annee", label: "Année", sortable: true, searchable: true },
  { field: "lac", label: "Plan d'eau", sortable: true, searchable: true },
  { field: "total", label: "Utilisateurs actifs", sortable: true }
];
const userRegistrationsColumns = [
  { field: "annee", label: "Année", sortable: true, searchable: true },
  { field: "total", label: "Inscriptions", sortable: true }
];
const tripsPerLakeColumns = [
  { field: "annee", label: "Année", sortable: true, searchable: true },
  { field: "lac", label: "Plan d'eau", sortable: true, searchable: true },
  { field: "total", label: "Nombre de sorties", sortable: true }
];
const catchesPerLakeColumns = [
  { field: "annee", label: "Année", sortable: true, searchable: true },
  { field: "lac", label: "Plan d'eau", sortable: true, searchable: true },
  { field: "total", label: "Nombres de prises", sortable: true }
];
const automaticMeasuresPerLakeColumns = [
  { field: "annee", label: "Année", sortable: true, searchable: true },
  { field: "lac", label: "Plan d'eau", sortable: true, searchable: true },
  { field: "total", label: "Mesures automatiques", sortable: true }
];

BackendService.backendGet("/v1/admin/check").then(
  (admin) => {
    loggedAdmin.value = admin;
  }
);
BackendService.backendGet(url).then(res => {
  metrics.value = res;
});


function exportAllAsCSV() {
  let csvContent = "";

  const columns = [
    { field: "annee", label: "Année", sortable: true },
    { field: "lac", label: "Plan d'eau", sortable: true },
    { field: "indic_type", label: "Indicateur", sortable: true },
    { field: "total", label: "Valeur", sortable: true }
  ];
  for (let i = 0; i < columns.length; i++) {
    if (i > 0) {
      csvContent += ";";
    }
    csvContent += ((columns[i] as unknown) as Column).label;
  }
  csvContent += getCSVRows(
    columns,
    metrics.value.activeUsersPerYear,
    "Nombre d'utilisateurs actifs (au moins une sortie dans l'année)"
  );
  if (loggedAdmin.value.isNationalAdmin) {
    csvContent += getCSVRows(
      columns,
    
      metrics.value.userRegistrationsPerYear,
      "Nombre d'inscriptions par an"
    );
  }
  csvContent += getCSVRows(
    columns,
    metrics.value.tripsPerLake,
    "Nombre de sorties par plan d'eau et par an"
  );
  csvContent += getCSVRows(
    columns,
    metrics.value.catchesPerLake,
    "Nombre de captures par plan d'eau et par an "
  );
  if (loggedAdmin.value.isNationalAdmin) {
    csvContent += getCSVRows(
      columns,
      metrics.value.automaticMeasuresPerLake,
      "Nombre de mesures automatiques par plan d'eau et par an"
    );
  }
  downloadCSV("ensemble_indicateurs", csvContent);
}

function exportAsCSV(fileName: string, columns: Array<string>, array: Array<any>) {
  let csvContent = "";
  for (let i = 0; i < columns.length; i++) {
    if (i > 0) {
      csvContent += ";";
    }
    csvContent += ((columns[i] as unknown) as Column).label;
  }
  csvContent += "\n";
  // Add content rows
  csvContent += array
    .map(row => {
      let csvRow = "";
      for (let i = 0; i < columns.length; i++) {
        if (i > 0) {
          csvRow += ";";
        }
        csvRow += row[((columns[i] as unknown) as Column).field];
      }
      return csvRow;
    })
    .join("\n");
  downloadCSV(fileName, csvContent);
}

function getCSVRows(columns: Array<Column>, array: any, prefixLine: string) {
  let csvContent = "\n";
  csvContent += (array as Array<any>)
    .map(row => {
      let csvRow = "";
      for (let i = 0; i < columns.length; i++) {
        if (i > 0) {
          csvRow += ";";
        }
        const columnName = ((columns[i] as unknown) as Column).field;
        if (columnName == "indic_type") {
          csvRow += prefixLine;
        } else {
          csvRow += row[columnName];
        }
      }
      return csvRow;
    })
    .join("\n");
  return csvContent;
}

function downloadCSV(fileName: string, csvContent: string) {
  const hiddenElement = document.createElement("a");
  hiddenElement.href = "data:text/csv;charset=utf-8," + encodeURI(csvContent);
  hiddenElement.target = "_blank";

  //provide the name for the CSV file to be downloaded
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
  hiddenElement.download = "Fishola_" + fileName + "_" + dateString + ".csv";
  hiddenElement.click();
}

</script>

<style lang="less">
.metrics-container {
  padding-left: 0px;

  .metrics-super-title {
    max-width: 900px;
    display: flex;
    justify-content: space-between;
  }

  .metrics-title {
    max-width: 900px;
    display: flex;
    justify-content: space-between;
    padding-top: 60px;
    font-size: 20px;
  }

  .b-table {
    max-width: 900px;

    .table {
      tr {
        td {
          width: 300px;
        }
      }
    }
  }
}
</style>
