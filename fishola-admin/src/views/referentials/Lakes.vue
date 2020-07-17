<template>
  <div class="lakes">
    <Referential
      name="Lacs" 
      url="/v1/referential/lakes"
      :columns="lakeColumns"
      :createElement=createLake
      ></Referential>
  </div>
</template>

<script lang="ts">
import Referential from '@/components/Referential.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import BackendService from '@/services/BackendService.ts';

@Component({
  components: {
    Referential
  }
})
export default class LakesVue extends Vue {

  lakeColumns:any[] = [
    {
      field: 'id',
      label: 'Identifiant',
      visible: false,
      readOnly: true
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

  createLake(): any {
    return {
      'name': 'Nouveau lac',
      'exportAs': 'NouveauLac',
      'latitude': 45.5,
      'longitude': 5.8
    };
  }
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
