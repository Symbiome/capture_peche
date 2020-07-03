<template>
  <div class="techniques">
    <Referential
      name="Techniques de pêche"
      url="http://localhost:8080/api/v1/referential/techniques"
      :columns="techniqueColumns"></Referential>
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
export default class TechniquesVue extends Vue {

  techniqueColumns:any[] = [
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
    }
  ];
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
