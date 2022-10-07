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
      name="Actualités"
      url="/v1/news-all"
      :columns="docColumns"
      :createElement="createDocumentation"
      :canDelete="true"
    ></Referential>
  </div>
</template>

<script lang="ts">
import Referential from "@/components/Referential.vue";
import { Component, Vue } from "vue-property-decorator";

@Component({
  components: {
    Referential
  }
})
export default class DocumentationVue extends Vue {
  docColumns: any[] = [
    {
      field: "id",
      label: "Identifiant technique",
      visible: false,
      readOnly: true
    },
    {
      field: "name",
      label: "Nom"
    },
    {
      field: "content",
      label: "Contenu",
      isHTML: true
    },
    {
      field: "datePublicationDebut",
      label: "Début de publication"
    },
    {
      field: "datePublicationFin",
      label: "Fin de publication"
    }
  ];

  createDocumentation() {
    return {
      name: "Nouvelle actualité",
      content:
        "Ceci est un  <b>exemple</b> <ol><li>De</li><li>Liste</li></ol><hr/>",
      datePublicationDebut: new Date(),
      datePublicationFin: new Date()
    };
  }
}
</script>

<style scoped lang="less">
@import "../../less/main";
</style>
