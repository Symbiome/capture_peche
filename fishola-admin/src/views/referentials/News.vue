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
      @elementsLoaded="computeIsPublic"
    ></Referential>
  </div>
</template>

<script lang="ts">
import Referential from "@/components/Referential.vue";
import { Component, Vue } from "vue-property-decorator";

import { LocalDateTime, ZoneOffset, nativeJs } from "@js-joda/core";
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
      field: "datePublicationDebut",
      label: "Début de publication",
      isAPeriodBeginning: true
    },
    {
      field: "datePublicationFin",
      label: "Fin de publication",
      isAPeriodEnd: true,
      hiddenInPopup: true
    },

    {
      field: "isPublic",
      label: "Visible sur le site",
      isABoolean: true,
      hiddenInPopup: true
    },
    {
      field: "content",
      label: "Contenu",
      isHTML: true
    }
  ];

  createDocumentation() {
    const tomorow = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
    return {
      name: "Titre de votre actualité",
      content: "<h1>Partie 1</h1><p>Le corps de votre <b>actualité</b>",
      datePublicationDebut: [
        tomorow.year(),
        tomorow.monthValue(),
        tomorow.dayOfMonth(),
        7,
        30
      ],
      datePublicationFin: [
        tomorow.year(),
        tomorow.monthValue(),
        tomorow.dayOfMonth(),
        7,
        30
      ],
      isPublished: false
    };
  }

  computeIsPublic(actualites: any[]) {
    actualites.forEach(actualite => {
      let now = LocalDateTime.now(ZoneOffset.UTC);
      let dateDebut = this.parseLocalDateTime(actualite.datePublicationDebut);
      let dateFin = this.parseLocalDateTime(actualite.datePublicationFin);

      actualite.isPublic =
        now.isAfter(LocalDateTime.from(nativeJs(dateDebut))) &&
        now.isBefore(LocalDateTime.from(nativeJs(dateFin)));
    });
  }

  parseLocalDateTime(someLocalDateTime: number[]): Date {
    if (someLocalDateTime[5]) {
      return new Date(
        someLocalDateTime[0],
        someLocalDateTime[1] - 1,
        someLocalDateTime[2],
        someLocalDateTime[3],
        someLocalDateTime[4],
        someLocalDateTime[5]
      );
    } else {
      return new Date(
        someLocalDateTime[0],
        someLocalDateTime[1] - 1,
        someLocalDateTime[2],
        someLocalDateTime[3],
        someLocalDateTime[4]
      );
    }
  }
}
</script>

<style scoped lang="less">
@import "../../less/main";
</style>
