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
  <div class="lakes">
    <Referential
      name="Utilisateurs"
      url="/v1/security/users"
      :columns="userColumns"
      :canDelete=true></Referential>
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
export default class UsersVue extends Vue {

  userColumns:any[] = [
    {
      field: 'id',
      label: 'Identifiant',
      visible: false,
      readOnly: true
    },
    {
      field: 'firstName',
      label: 'Prénom',
      readOnly: true
    },
    {
      field: 'lastName',
      label: 'Nom',
      readOnly: true
    },
    {
      field: 'email',
      label: 'E-mail',
      readOnly: true
    },
    {
      field: 'gender',
      label: 'Genre',
      readOnly: true
    },
    {
      field: 'birthYear',
      label: 'Année de naissance',
      readOnly: true
    },
    {
      field: 'excludeFromExports',
      label: 'Exclu des exports',
      isABoolean: true
    },
    {
      field: 'createdOn',
      label: 'Date de création',
      isADate: true,
      readOnly: true
    }
  ];
}
</script>

<style scoped lang="less">

@import "../less/main";

</style>
