<template>
  <div class="trips">
    <p>Vous pouvez exporter les sorties.</p>
    <button class="button is-primary" @click="exportToCsv()">Lancer un export CSV</button>
  </div>
</template>

<script lans="ts">

import { Component, Prop, Vue } from 'vue-property-decorator'

@Component
export default class TripsVue extends Vue {
    exportToCsv() {
        this.$buefy.dialog.alert('Not yet implemented!');
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
