<!--
  #%L
  Fishola :: Admin
  %%
  Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
    name="Opérateurs"
    url="/v1/admin/operators"
    @elements-loaded="computeLakeNames"
    :columns="operatorColumns"
    :createElement="createOperator"
    :editable="canManageOperators"
    :canDelete="false"
  ></Referential>
</template>

<script setup lang="ts">
import Referential from "@/components/Referential.vue";
import BackendService from "@/services/BackendService";
import { ref, Ref } from "vue";

const lakesIdToNameMap = ref(new Map<string, string>());
const loaded = ref(false);
const canManageOperators = ref(false);
const operatorColumns: Ref<any[]> = ref([]);

loadLakes();

async function loadLakes() {
  const admin = await BackendService.backendGet("/v1/admin/check");
  // Les opérateurs sont gérés par les mêmes profils que les administrateurs.
  canManageOperators.value = admin.isNationalAdmin || admin.canCreateAdmins;
  const lakes = await BackendService.backendGet("/v1/referential/waterEntities");
  const lakesOptions: any[] = [];
  lakes.forEach((l: any) => {
    lakesOptions.push({
      id: l.id,
      label: l.name
    });
    lakesIdToNameMap.value.set(l.id, l.name);
  });

  operatorColumns.value = [
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
      readOnlyIfFunction: (operator) => { return operator.id; }
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
      showItemIfFunction: (operator) => {
        return !operator.id;
      },
    },
    {
      // Le backend (RegisterAdminBean / AdminProfileForAdmin) lit et renvoie « waterEntityIds ».
      field: "waterEntityIds",
      label: "Plans d'eau",
      isArray: true,
      visible: false,
      arrayOptions: lakesOptions,
      possibleValuesForItemFunction: (operator) => {
        return operator.waterEntityIds ?? [];
      },
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

function computeLakeNames(operators: any[]) {
  operators.forEach(operator => {
    operator.lakeNames = (operator.waterEntityIds ?? [])
      .map((waterEntityId: string) => lakesIdToNameMap.value.get(waterEntityId))
      .join(", ");
  });
}

function createOperator(): any {
  return {
    name: "Nouvel opérateur",
    email: "",
    password: "",
    waterEntityIds: []
  };
}
</script>
