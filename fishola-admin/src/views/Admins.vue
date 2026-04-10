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
    v-if="loaded"
    name="Administrateurs"
    url="/v1/admin"
     @elements-loaded="computeLakeNames"
    :columns="userColumns"
    :createElement="createAdmin"
    :editable="canCreateAdmins"
    :canDelete="false"
  ></Referential>
</template>

<script lang="ts">
import Referential from "@/components/Referential.vue";
import BackendService from "@/services/BackendService";

import { Component, Vue } from "vue-facing-decorator";

@Component({
  components: {
    Referential
  }
})
export default class UsersVue extends Vue {
  lakesIdToNameMap = new Map<string, string>();
  loaded = false;
  canCreateAdmins = false
  userColumns: any[] = [
  ];

  created() {
   this.loadLakes();
  }

  async loadLakes() {
     const admin = await BackendService.backendGet("/v1/admin/check");
     this.canCreateAdmins = admin.isNationalAdmin || admin.canCreateAdmins;
     const lakes = await BackendService.backendGet("/v1/referential/lakes");
     const lakesOptions: any[] = [];
     lakes.forEach( (l: any) => {
        lakesOptions.push({
          id: l.id,
          label: l.name
        });
        this.lakesIdToNameMap.set(l.id, l.name);
      });

     this.userColumns = [
      {
        field: "id",
        label: "Identifiant",
        visible: false,
        readOnly: true,
        hiddenInPopup: true
      },
      {
        field: "email",
        label: "E-mail",
        searchable: true,
        readOnlyIfFunction: (admin) => { return admin.id; }
      },
       {
        field: "lakeNames",
        label: "Plans d'eau",
        searchable: true,
        hiddenInPopup: true
      },
      {
        field: "password",
        label: "Mot de passe",
        visible: false,
         showItemIfFunction: (admin) => {
          return !admin.id;
        },
      },
      {
        field: "lakeIds",
        label: "Plans d'eau",
        isArray: true,
        visible: false,
        arrayOptions: lakesOptions,
        possibleValuesForItemFunction: (admin) => {
          return admin.lakeIds ?? [];
        },
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
    this.loaded = true;
  }
  computeLakeNames(admins: any[]) {
    admins.forEach(admin => {
     admin.lakeNames = admin.isNationalAdmin ?
        "National" :
        admin.lakeIds.map((lakeId: string) => this.lakesIdToNameMap.get(lakeId)).join(", ");
    });
  }

  createAdmin(): any {
    return {
      name: "Nouvel administrateur",
      email: "",
      password: "",
      isNationalAdmin: false,
      lakeIds: []
    };
  }
}
</script>
