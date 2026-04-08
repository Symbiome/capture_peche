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
  <div class="pages">
    <Referential
      name="Documentations en téléchargement"
      url="/v1/documentations"
      :columns="docColumns"
      :createElement=createDocumentation
      :canDelete=true
      ></Referential>
  </div>
</template>

<script lang="ts">
import Referential from '@/components/Referential.vue'

import { Component, Vue } from 'vue-facing-decorator';

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
      'name': 'Nouvelle documentation',
      'url': '',
      'base64Content': ''
    };
  }
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
