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
  <div class="species">
    <Referential
      name="Espèces"
      :url="url"
      :columns="specieColumns"
      :createElement="createSpecie"
      :canDelete="true"
      :canDeletePredicate="canDeleteSpecie"
    ></Referential>
  </div>
</template>

<script lang="ts">
import Referential from "@/components/Referential.vue";

import BackendService from "@/services/BackendService";
import { Component, Vue } from "vue-property-decorator";

@Component({
  components: {
    Referential
  }
})
export default class SpeciesVue extends Vue {
  url = "/v1/referential/raw-species";
  specieColumns: any[] = [
    {
      field: "id",
      label: "Identifiant",
      visible: false,
      readOnly: true
    },
    {
      field: "name",
      label: "Nom",
      searchable: true,
    },
    {
      field: "exportAs",
      label: "Nom d'export",
      searchable: true,
    },
    {
      field: "builtIn",
      label: "Pour tout le monde ?",
      isABoolean: true
    },
    {
      field: "mandatorySize",
      label: "Taille obligatoire ?",
      isABoolean: true
    }
  ];

  createSpecie(): any {
    return {
      name: "Nouvelle espèce",
      builtIn: true,
      exportAs: "NouvelleEspece",
      mandatorySize: true
    };
  }

  canDeleteSpecie(specie: any): Promise<boolean> {
    return BackendService.backendGet(this.url + "/can-delete/" + specie["id"]);
  }
}
</script>
