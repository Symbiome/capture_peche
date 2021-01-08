<!--
  #%L
  Fishola :: Admin
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #L%
  -->
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
