<template>
  <div class="pages">
    <Referential
      name="Documentation" 
      url="/v1/documentations"
      :columns="docColumns"
      :createElement=createDocumentation 
      :canDelete=true
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
export default class DocumentationVue extends Vue {

  docColumns:any[] = [
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
      field: 'url',
      label: 'Fichier',
      isFile: true
    }
  ];

  createDocumentation() {
    return {
      'name': 'Nouveau document'
    };
  }
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
