<template>
  <div class="species">
    <Referential
      name="Espèces"
      url="/v1/referential/raw-species"
      :columns="specieColumns"
      :createElement=createSpecie 
      :canDelete=true
      :canDeletePredicate=canDeleteSpecie    
      ></Referential>
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
      label: 'Identifiant',
      visible: false,
      readOnly: true
    },
    {
      field: 'name',
      label: 'Nom'
    },
    {
      field: 'builtIn',
      label: 'Pour tout le monde ?',
      isABoolean: true
    },
    {
      field: 'mandatorySize',
      label: 'Taille obligatoire ?',
      isABoolean: true
    }
  ];

  createSpecie(): any {
    return {
      'name': 'Nouvelle espèce',
      'builtIn': true,
      'mandatorySize': true
    };
  }

  canDeleteSpecie(specie: any): Promise<boolean> {
    return Promise.resolve(true);
  }
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
