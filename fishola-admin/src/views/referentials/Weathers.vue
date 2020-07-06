<template>
  <div class="weathers">
    <Referential
      name="Météo"
      url="/v1/referential/weathers"
      :columns="weatherColumns"></Referential>
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
export default class WeathersVue extends Vue {

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
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
