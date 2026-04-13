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
      name="Pages éditoriales"
      url="/v1/editorial"
      :columns="pageColumns"
    ></Referential>
  </div>
</template>

<script setup lang="ts">
import Referential from '@/components/Referential.vue'

const pageColumns: any[] = [
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
    field: 'content',
    label: 'Contenu'
  },
  {
    field: 'link',
    label: 'Lien',
    isUrl: true
  }
];
</script>

<style scoped lang="less">
@import "../../less/main";
</style>
