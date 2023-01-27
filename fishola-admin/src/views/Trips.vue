<!--
  #%L
  Fishola :: Admin
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
  <div class="trips">
    <p>Vous pouvez exporter les sorties.</p>
    <a @click="exportCsv">
      <button class="button is-primary">Lancer un export CSV</button></a
    >
  </div>
</template>

<script lans="ts">
import { Component, Prop, Vue } from "vue-property-decorator";
import BackendService from "@/services/BackendService";
import Constants from "@/services/Constants";

@Component
export default class TripsVue extends Vue {
  async exportCsv() {
    let csvContent = await BackendService.backendGetRaw("/v1/trips/export");
    var hiddenElement = document.createElement("a");
    hiddenElement.href = "data:text/csv;charset=utf-8," + encodeURI(csvContent);
    hiddenElement.target = "_blank";
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
    hiddenElement.download = "Fishola_Export_" + "_" + dateString + ".csv";
    hiddenElement.click();
  }
}
</script>

<style scoped lang="less">
@import "../less/main";

.trips {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  font-size: 22px;

  p {
    margin: 5px;
  }
}
</style>
