<template>
  <div class="trips">
    <p>Vous pouvez exporter les sorties.</p>
    <a :href="exportCsvURL"> <button class="button is-primary">Lancer un export CSV</button></a>
  </div>
</template>

<script lans="ts">

import { Component, Prop, Vue } from 'vue-property-decorator';
import Constants from '@/services/Constants';

@Component
export default class TripsVue extends Vue {
  exportCsvURL = Constants.apiUrl("/v1/trips/export");
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
