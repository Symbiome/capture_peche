<template>
  <div class="lakes">
    <Referential
      name="Lacs" 
      url="http://localhost:8080/api/v1/referential/lakes"
      :columns="lakeColumns"></Referential>
  </div>
</template>

<script lang="ts">
import Referential from '@/components/Referential.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    Referential
  }
})
export default class LakesVue extends Vue {

  lakeColumns:any[] = [
    {
      field: 'id',
      label: 'Identifiant'
    },
    {
      field: 'name',
      label: 'Nom'
    },
    {
      field: 'exportAs',
      label: 'Nom d\'export'
    },
    {
      field: 'latitude',
      label: 'Latitude'
    },
    {
      field: 'longitude',
      label: 'Longitude'
    }
  ];
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
