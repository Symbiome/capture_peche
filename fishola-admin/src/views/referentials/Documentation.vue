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
      label: 'Identifiant technique',
      visible: false,
      readOnly: true
    },
    {
      field: 'naturalId',
      label: 'Identifiant naturel',
      readOnlyEdition: true
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
      'naturalId': '',
      'name': 'Nouveau document',
      'url': '',
      'base64Content': ''
    };
  }
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
