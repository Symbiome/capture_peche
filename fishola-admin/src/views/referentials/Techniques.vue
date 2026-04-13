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
      :createElement="createTechnique"
      :canDelete="true"
      :canDeletePredicate="canDeleteTechnique"
    ></Referential>
  </div>
</template>

<script setup lang="ts">
import Referential from "@/components/Referential.vue";
import BackendService from "@/services/BackendService";

const url = "/v1/referential/techniques";
const techniqueColumns: any[] = [
  {
    field: "id",
    label: "Identifiant",
    visible: false,
    readOnly: true
  },
  {
    field: "name",
    label: "Nom"
  },
  {
    field: "exportAs",
    label: "Nom d'export"
  }
];

function createTechnique(): any {
  return {
    name: "Nouvelle technique",
    builtIn: true,
    exportAs: "technique"
  };
}

function canDeleteTechnique(technique: any): Promise<boolean> {
  return BackendService.backendGet(
    url + "/can-delete/" + technique["id"]
  );
}
</script>
