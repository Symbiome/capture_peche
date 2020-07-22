<template>
  <div class="weathers">
    <Referential
      name="Météo"
      :url="url"
      :columns="weatherColumns"
      :createElement=createWeather 
      :canDelete=true
      :canDeletePredicate=canDeleteWeather></Referential>
  </div>
</template>

<script lang="ts">
import Referential from '@/components/Referential.vue'

import BackendService from '@/services/BackendService.ts';
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    Referential
  }
})
export default class WeathersVue extends Vue {

  url="/v1/referential/weathers"
  weatherColumns:any[] = [
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
    }
  ];

  createWeather(): any {
    return {
      'name': 'Sans Nuage',
      'exportAs': 'nouveautemps'
    };
  }

  canDeleteWeather(weather: any): Promise<boolean> {
    return BackendService.backendGet(this.url+"/can-delete/" + weather['id']);
  }
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
