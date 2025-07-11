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
        <b-button type="is-primary" @click="exportAllAsCSV()"
          >Tout exporter en csv</b-button
        >
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
            >Exporter en csv</b-button
          >
        </h2>
        <b-table
          :data="metrics.activeUsersPerYear"
          :columns="activeUsersColumns"
          :default-sort="['lac', 'asc']"
        />

        <h2 class="metrics-title" v-if="loggedAdmin.isNationalAdmin">
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
            >Exporter en csv</b-button
          >
        </h2>
        <b-table v-if="loggedAdmin.isNationalAdmin"
          :data="metrics.userRegistrationsPerYear"
          :columns="userRegistrationsColumns"
          :default-sort="['annee', 'asc']"
        />

        <h2 class="metrics-title">
          Nombre de sorties par lac et par an
          <b-button
            type="is-primary"
            @click="
              exportAsCSV('sorties', tripsPerLakeColumns, metrics.tripsPerLake)
            "
            >Exporter en csv</b-button
          >
        </h2>
        <b-table
          :data="metrics.tripsPerLake"
          :columns="tripsPerLakeColumns"
          :default-sort="['lac', 'asc']"
        />

        <h2 class="metrics-title">
          Nombre de captures par lac et par an
          <b-button
            type="is-primary"
            @click="
              exportAsCSV(
                'captures',
                catchesPerLakeColumns,
                metrics.catchesPerLake
              )
            "
            >Exporter en csv</b-button
          >
        </h2>
        <b-table
          :data="metrics.catchesPerLake"
          :columns="catchesPerLakeColumns"
          :default-sort="['lac', 'asc']"
        />
        <h2 class="metrics-title" v-if="loggedAdmin.isNationalAdmin">
          Nombre de mesures automatiques par lac et par an
          <b-button
            type="is-primary"
            @click="
              exportAsCSV(
                'mesures',
                automaticMeasuresPerLakeColumns,
                metrics.automaticMeasuresPerLake
              )
            "
            >Exporter en csv</b-button
          >
        </h2>
        <b-table v-if="loggedAdmin.isNationalAdmin"
          :data="metrics.automaticMeasuresPerLake"
          :columns="automaticMeasuresPerLakeColumns"
          :default-sort="['lac', 'asc']"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import BackendService from "@/services/BackendService";
import { Component, Vue } from "vue-property-decorator";

@Component({
  components: {}
})
export default class Metrics extends Vue {
  url = "/v1/metrics";
  loggedAdmin = { isNationalAdmin: false}
  metrics = {
    activeUsersPerYear: [],
    userRegistrationsPerYear: [],
    tripsPerLake: [],
    catchesPerLake: [],
    automaticMeasuresPerLake: []
  };
  activeUsersColumns = [
    { field: "annee", label: "Année", sortable: true, searchable: true },
    { field: "lac", label: "Lac", sortable: true, searchable: true },
    { field: "total", label: "Utilisateurs actifs", sortable: true }
  ];
  userRegistrationsColumns = [
    { field: "annee", label: "Année", sortable: true, searchable: true },
    { field: "total", label: "Inscriptions", sortable: true }
  ];
  tripsPerLakeColumns = [
    { field: "annee", label: "Année", sortable: true, searchable: true },
    { field: "lac", label: "Lac", sortable: true, searchable: true },
    { field: "total", label: "Nombre de sorties", sortable: true }
  ];
  catchesPerLakeColumns = [
    { field: "annee", label: "Année", sortable: true, searchable: true },
    { field: "lac", label: "Lac", sortable: true, searchable: true },
    { field: "total", label: "Nombres de prises", sortable: true }
  ];
  automaticMeasuresPerLakeColumns = [
    { field: "annee", label: "Année", sortable: true, searchable: true },
    { field: "lac", label: "Lac", sortable: true, searchable: true },
    { field: "total", label: "Mesures automatiques", sortable: true }
  ];

  created() {
    BackendService.backendGet("/v1/admin/check").then(
      (admin) => {
        this.loggedAdmin = admin
      }
    );
    BackendService.backendGet(this.url).then(res => {
      this.metrics = res;
    });
  }

  exportAllAsCSV() {
    let csvContent = "";

    const columns = [
      { field: "annee", label: "Année", sortable: true },
      { field: "lac", label: "Lac", sortable: true },
      { field: "indic_type", label: "Indicateur", sortable: true },
      { field: "total", label: "Valeur", sortable: true }
    ];
    for (var i = 0; i < columns.length; i++) {
      if (i > 0) {
        csvContent += ";";
      }
      csvContent += ((columns[i] as unknown) as Column).label;
    }
    csvContent += this.getCSVRows(
      columns,
      this.metrics.activeUsersPerYear,
      "Nombre d'utilisateurs actifs (au moins une sortie dans l'année)"
    );
    if (this.loggedAdmin.isNationalAdmin) {
      csvContent += this.getCSVRows(
        columns,
        this.metrics.userRegistrationsPerYear,
        "Nombre d'inscriptions par an"
      );
    }
    csvContent += this.getCSVRows(
      columns,
      this.metrics.tripsPerLake,
      "Nombre de sorties par lac et par an"
    );
    csvContent += this.getCSVRows(
      columns,
      this.metrics.catchesPerLake,
      "Nombre de captures par lac et par an "
    );
    if (this.loggedAdmin.isNationalAdmin) {
      csvContent += this.getCSVRows(
        columns,
        this.metrics.automaticMeasuresPerLake,
        "Nombre de mesures automatiques par lac et par an"
      );
    }
    this.downloadCSV("ensemble_indicateurs", csvContent);
  }

  exportAsCSV(fileName: string, columns: Array<string>, array: Array<any>) {
    let csvContent = "";
    for (var i = 0; i < columns.length; i++) {
      if (i > 0) {
        csvContent += ";";
      }
      csvContent += ((columns[i] as unknown) as Column).label;
    }
    csvContent += "\n";
    // Add content rows
    csvContent += array
      .map(row => {
        var csvRow = "";
        for (var i = 0; i < columns.length; i++) {
          if (i > 0) {
            csvRow += ";";
          }
          csvRow += row[((columns[i] as unknown) as Column).field];
        }
        return csvRow;
      })
      .join("\n");
    this.downloadCSV(fileName, csvContent);
  }

  getCSVRows(columns: Array<Column>, array: any, prefixLine: string) {
    let csvContent = "\n";
    csvContent += (array as Array<any>)
      .map(row => {
        var csvRow = "";
        for (var i = 0; i < columns.length; i++) {
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

  downloadCSV(fileName: string, csvContent: string) {
    var hiddenElement = document.createElement("a");
    hiddenElement.href = "data:text/csv;charset=utf-8," + encodeURI(csvContent);
    hiddenElement.target = "_blank";

    //provide the name for the CSV file to be downloaded
    const d = new Date();
    var mm = d.getMonth() + 1;
    var dd = d.getDate();
    let dateString =
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
}

class Column {
  field: string;
  label: string;
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
