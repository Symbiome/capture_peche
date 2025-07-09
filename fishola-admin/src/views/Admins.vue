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
  <Referential
    name="Administrateurs"
    url="/v1/admin/admins"
    :columns="userColumns"
    :canDelete="true"
  ></Referential>
</template>

<script lang="ts">
import Referential from "@/components/Referential.vue";

import { Component, Vue } from "vue-property-decorator";

@Component({
  components: {
    Referential
  }
})
export default class UsersVue extends Vue {
  userColumns: any[] = [
    {
      field: "id",
      label: "Identifiant",
      visible: false,
      readOnly: true
    },
    {
      field: "email",
      label: "E-mail",
      readOnly: true
    },
    {
      field: "isNationalAdmin",
      label: "Administrateur national",
      isABoolean: true
    },
    {
      field: "canCreateAdmin",
      label: "Droit de gestion des administrateurs",
      isABoolean: true
    },
    {
      field: "createdOn",
      label: "Date de création",
      isADate: true,
      readOnly: true
    }
  ];
}
</script>

<style scoped lang="less">
</style>
