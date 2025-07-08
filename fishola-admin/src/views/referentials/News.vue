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
      name="Communications"
      url="/v1/news-all"
      :default-sort="['isPublic', 'desc']"
      :columns="docColumns"
      :createElement="createDocumentation"
      :canDelete="true"
      @elementsLoaded="computeIsPublicAndMiniatureURLs"
      @send-notification="sendNotification"
      :nextPlannifiedDate="nextPlannifiedDate"
    ></Referential>
  </div>
</template>

<script lang="ts">
import Referential from "@/components/Referential.vue";
import { Component, Vue } from "vue-property-decorator";

import { LocalDateTime, ZoneOffset, nativeJs } from "@js-joda/core";
import BackendService from "@/services/BackendService";
import Constants from "@/services/Constants";
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
      field: "miniatureUrl",
      label: "Miniature",
      visible: false,
      isPicture: true
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
      field: "dateNotificationSent",
      label: "Date d'envoi de la notification mail",
      isANotificationDate: true,
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

  nextPlannifiedDate: number[] = [];

  created() {
    this.refreshNextPlannifiedDate();
  }

  createDocumentation() {
    const tomorow = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
    return {
      name: "Titre de votre communication",
      content: "<h1>Partie 1</h1><p>Le corps de votre <b>communication</b>",
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

  computeIsPublicAndMiniatureURLs(actualites: any[]) {
    actualites.forEach(actualite => {
      let now = LocalDateTime.now(ZoneOffset.UTC);
      let dateDebut = this.parseLocalDateTime(actualite.datePublicationDebut);
      let dateFin = this.parseLocalDateTime(actualite.datePublicationFin);

      actualite.isPublic =
        now.isAfter(LocalDateTime.from(nativeJs(dateDebut))) &&
        now.isBefore(LocalDateTime.from(nativeJs(dateFin)));

      actualite.miniatureURL = actualite.miniatureId
        ? Constants.apiUrl("/v1/news-picture/" + actualite.miniatureId)
        : "";
      console.error(actualite.miniatureId + " => " + actualite.miniatureURL);
    });
  }

  sendNotification(actualite: any) {
    this.$buefy.dialog.confirm({
      title: "Envoi de notification mail",
      message: `Confirmez-vous souhaiter vouloir envoyer immédiatement un courriel aux utilisateurs de Fishola avec le contenu de l'actualité <i><b>${actualite["name"]}</b></i> ?`,
      cancelText: "Annuler",
      confirmText: "Envoyer notification immédiatement",
      type: "is-success",
      onConfirm: async () => {
        try {
          await BackendService.backendGet(
            "/v1/news-notifications/send/" + actualite.id
          );
          this.$buefy.toast.open({
            type: "is-success",
            message: "Notification envoyée"
          });
          this.refreshNextPlannifiedDate();
        } catch (e) {
          this.$buefy.toast.open({
            message:
              "Une erreur est survenue lors de l'envoi de la notification",
            type: "is-danger"
          });
        }
      }
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

  refreshNextPlannifiedDate(): void {
    BackendService.backendGet("/v1/news-notifications/next-check").then(
      nextCheckDate => {
        this.nextPlannifiedDate = nextCheckDate;
      },
      error => {
        this.$buefy.toast.open({
          message: "Vous n'êtes plus connecté\u00B7e",
          type: "is-danger"
        });
        this.$router.push("/login");
      }
    );
  }
}
</script>

<style scoped lang="less">
</style>
