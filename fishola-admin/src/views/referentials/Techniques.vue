<template>
  <div class="techniques">
    <Referential
      name="Techniques de pêche"
      :url="url"
      :columns="techniqueColumns"
      :createElement=createTechnique 
      :canDelete=true
      :canDeletePredicate=canDeleteTechnique></Referential>
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
export default class TechniquesVue extends Vue {

  url = "/v1/referential/techniques";
  techniqueColumns:any[] = [
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

  createTechnique(): any {
    return {
      'name': 'Nouvelle technique',
      'builtIn': true,
      'exportAs': 'technique'
    };
  }

  canDeleteTechnique(technique: any): Promise<boolean> {
    return BackendService.backendGet(this.url+"/can-delete/" + technique['id']);
  }
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
