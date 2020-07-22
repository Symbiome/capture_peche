<template>
  <div class="species">
    <Referential
      name="Espèces"
      :url="url"
      :columns="specieColumns"
      :createElement=createSpecie 
      :canDelete=true
      :canDeletePredicate=canDeleteSpecie    
      ></Referential>
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
export default class SpeciesVue extends Vue {

  url = "/v1/referential/raw-species";
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
      field: 'exportAs',
      label: 'Nom d\'export'
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
      'exportAs': 'NouvelleEspece',
      'mandatorySize': true
    };
  }

  canDeleteSpecie(specie: any): Promise<boolean> {
    return BackendService.backendGet(this.url+"/can-delete/" + specie['id']);
  }
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
