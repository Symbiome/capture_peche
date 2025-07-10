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
    url="/v1/admin/"
    :columns="userColumns"
    :createElement="createAdmin"
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
      visibility: false,
      readOnly: true,
      hiddenInPopup: true
    },
    {
      field: "email",
      label: "E-mail",
    },
    {
      field: "password",
      label: "Mot de passe",
      visible: false
    },
    {
      field: "lakeIds",
      label: "Lacs",
      isArray: true,
      /* FIXME TODO LK : Tmp for tests */
      arrayOptions: [
        {
          id: "8672a54a-7c9e-47da-8b0c-889575cba2ba",
          label: "Léman"
        },
        {
          id: "244e8912-a447-4770-8e5c-e48ebd135a65",
          label: "Lac du Bourget"
        },
        {
          id: "09e57c19-b129-4d59-ae63-759d032dda06",
          label: "Lac d'Aiguebelette"
        }
      ],
      visible: false
    },
    {
      field: "isNationalAdmin",
      label: "Administrateur national",
      isABoolean: true,
      readonly: true,
      helpMessage: "Non modifiable depuis les écrans d'administration, par mesure de sécurité."
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
      readOnly: true,
      visible: false,
      hiddenInPopup: true
    }
  ];

  createAdmin(): any {
    return {
      name: "Nouvel administrateur",
      builtIn: true,
      exportAs: "NouvelAdministrateur",
      mandatorySize: true
    };
  }
}
</script>

<style scoped lang="less">
</style>
