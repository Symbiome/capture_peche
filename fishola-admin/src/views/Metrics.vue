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
            @click="exportAsCSV(activeUsersColumns, metrics.activeUsersPerYear)"
            >Exporter en csv</b-button
          >
        </h2>
        <b-table
          :data="metrics.activeUsersPerYear"
          :columns="activeUsersColumns"
          :default-sort="['lac', 'asc']"
        />

        <h2 class="metrics-title">
          Nombre d'inscriptions par an
          <b-button
            type="is-primary"
            @click="
              exportAsCSV(
                userRegistrationsColumns,
                metrics.userRegistrationsPerYear
              )
            "
            >Exporter en csv</b-button
          >
        </h2>
        <b-table
          :data="metrics.userRegistrationsPerYear"
          :columns="userRegistrationsColumns"
          :default-sort="['annee', 'asc']"
        />

        <h2 class="metrics-title">
          Nombre de sorties par lac et par an
          <b-button
            type="is-primary"
            @click="exportAsCSV(tripsPerLakeColumns, metrics.tripsPerLake)"
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
            @click="exportAsCSV(catchesPerLakeColumns, metrics.catchesPerLake)"
            >Exporter en csv</b-button
          >
        </h2>
        <b-table
          :data="metrics.catchesPerLake"
          :columns="catchesPerLakeColumns"
          :default-sort="['lac', 'asc']"
        />
        <h2 class="metrics-title">
          Nombre de mesures automatiques par lac et par an
          <b-button
            type="is-primary"
            @click="
              exportAsCSV(
                automaticMeasuresPerLakeColumns,
                metrics.automaticMeasuresPerLake
              )
            "
            >Exporter en csv</b-button
          >
        </h2>
        <b-table
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
  metrics = {
    activeUsersPerYear: [],
    userRegistrationsPerYear: [],
    tripsPerLake: [],
    catchesPerLake: [],
    automaticMeasuresPerLake: []
  };
  activeUsersColumns = [
    { field: "annee", label: "Année", sortable: true },
    { field: "lac", label: "Lac", sortable: true },
    { field: "total", label: "Utilisateurs actifs", sortable: true }
  ];
  userRegistrationsColumns = [
    { field: "annee", label: "Année", sortable: true },
    { field: "total", label: "Inscriptions", sortable: true }
  ];
  tripsPerLakeColumns = [
    { field: "annee", label: "Année", sortable: true },
    { field: "lac", label: "Lac", sortable: true },
    { field: "total", label: "Nombre de sorties", sortable: true }
  ];
  catchesPerLakeColumns = [
    { field: "annee", label: "Année", sortable: true },
    { field: "lac", label: "Lac", sortable: true },
    { field: "total", label: "Nombres de prises", sortable: true }
  ];
  automaticMeasuresPerLakeColumns = [
    { field: "annee", label: "Année", sortable: true },
    { field: "lac", label: "Lac", sortable: true },
    { field: "total", label: "Mesures automatiques", sortable: true }
  ];

  created() {
    BackendService.backendGet(this.url).then(res => {
      this.metrics = res;
    });
  }

  exportAllAsCSV() {
    let csvContent = "data:text/csv;charset=utf-8,";

    const columns = [
      { field: "annee", label: "Année", sortable: true },
      { field: "lac", label: "Lac", sortable: true },
      { field: "total", label: "Indicateur", sortable: true }
    ];
    for (var i = 0; i < columns.length; i++) {
      if (i > 0) {
        csvContent += ",";
      }
      csvContent += ((columns[i] as unknown) as Column).label;
    }
    csvContent += this.getCSVRows(
      columns,
      this.metrics.activeUsersPerYear,
      "Nombre d'utilisateurs actifs (au moins une sortie dans l'année)"
    );
    csvContent += this.getCSVRows(
      columns,
      this.metrics.userRegistrationsPerYear,
      "Nombre d'inscriptions par an"
    );
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
    csvContent += this.getCSVRows(
      columns,
      this.metrics.automaticMeasuresPerLake,
      "Nombre de mesures automatiques par lac et par an"
    );
    var encodedUri = encodeURI(csvContent);
    window.open(encodedUri);
  }

  exportAsCSV(columns: Array<string>, array: Array<any>) {
    let csvContent = "data:text/csv;charset=utf-8,";
    for (var i = 0; i < columns.length; i++) {
      if (i > 0) {
        csvContent += ",";
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
            csvRow += ",";
          }
          csvRow += row[((columns[i] as unknown) as Column).field];
        }
        return csvRow;
      })
      .join("\n");
    var encodedUri = encodeURI(csvContent);
    window.open(encodedUri);
  }

  getCSVRows(columns: Array<Column>, array: any, prefixLine: string) {
    let csvContent = "\n" + prefixLine + "," + ",\n";
    csvContent += (array as Array<any>)
      .map(row => {
        var csvRow = "";
        for (var i = 0; i < columns.length; i++) {
          if (i > 0) {
            csvRow += ",";
          }
          csvRow += row[((columns[i] as unknown) as Column).field];
        }
        return csvRow;
      })
      .join("\n");
    return csvContent;
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
