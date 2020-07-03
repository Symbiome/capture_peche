<template>
  <div class="species">
    <Referential
      name="Espèces"
      url="http://localhost:8080/api/v1/referential/species"
      :columns="specieColumns"></Referential>
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
export default class SpeciesVue extends Vue {

  specieColumns:any[] = [
    {
      field: 'id',
      label: 'Identifiant'
    },
    {
      field: 'name',
      label: 'Nom'
    },
    {
      field: 'builtIn',
      label: 'De base'
    },
    {
      field: 'mandatorySize',
      label: 'Taille obligatoire'
    },
    {
      field: 'authorizedSample',
      label: 'Prélèvements autorisé'
    }
  ];
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
