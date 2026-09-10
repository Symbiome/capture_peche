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
    @elements-loaded="computeDepartmentNames"
    :columns="userColumns"
    :createElement="createAdmin"
    :editable="canCreateAdmins"
    :canDelete="false"
  ></Referential>
</template>

<script setup lang="ts">
import Referential from "@/components/Referential.vue";
import BackendService from "@/services/BackendService";
import { ref, Ref } from "vue";

const departmentCodeToNameMap = ref(new Map<string, string>());
const loaded = ref(false);
const canCreateAdmins = ref(false);
const userColumns: Ref<any[]> = ref([]);

loadDepartments();

async function loadDepartments() {
  const admin = await BackendService.backendGet("/v1/admin/check");
  canCreateAdmins.value = admin.isNationalAdmin || admin.canCreateAdmins;
  // Périmètre exprimé en départements (#159) : on ne charge plus tout le référentiel hydro.
  const departments = await BackendService.backendGet("/v1/referential/departments");
  const departmentOptions: any[] = [];
  departments.forEach((d: any) => {
    departmentOptions.push({
      id: d.code,
      label: d.code + " — " + d.name
    });
    departmentCodeToNameMap.value.set(d.code, d.name);
  });

  userColumns.value = [
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
      field: "departmentNames",
      label: "Départements",
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
      // Le backend (RegisterAdminBean / AdminProfileForAdmin) lit et renvoie « departmentCodes ».
      field: "departmentCodes",
      label: "Départements",
      isArray: true,
      visible: false,
      arrayOptions: departmentOptions,
      possibleValuesForItemFunction: (admin) => {
        return admin.departmentCodes ?? [];
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
  loaded.value = true;
}

function computeDepartmentNames(admins: any[]) {
  admins.forEach(admin => {
    admin.departmentNames = admin.isNationalAdmin ?
      "National" :
      (admin.departmentCodes ?? []).map((code: string) => code + " — " + departmentCodeToNameMap.value.get(code)).join(", ");
  });
}

function createAdmin(): any {
  return {
    name: "Nouvel administrateur",
    email: "",
    password: "",
    isNationalAdmin: false,
    departmentCodes: []
  };
}
</script>
