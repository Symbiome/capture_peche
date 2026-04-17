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
      v-if="docColumns.length > 4"
      name="Communications"
      url="/v1/news-all"
      :default-sort="['isPublic', 'desc']"
      :columns="docColumns"
      :createElement="createDocumentation"
      :canDelete="true"
      @elements-loaded="computeIsPublicAndMiniatureURLs"
      @send-notification="sendNotification"
      :nextPlannifiedDate="nextPlannifiedDate"
    ></Referential>
  </div>
</template>

<script setup lang="ts">
import Referential from "@/components/Referential.vue";

import { LocalDateTime, ZoneOffset, nativeJs } from "@js-joda/core";
import BackendService from "@/services/BackendService";
import Constants from "@/services/Constants";

import router from "@/router";
import { useDialog, useToast } from "buefy";
import { reactive, Reactive, ref, Ref } from "vue";

const Dialog = useDialog();
const Toast = useToast();

const lakesIdToNameMap = reactive(new Map<string, string>());
const lakesOptions: Reactive<any[]> = reactive([]);
const docColumns: Ref<any[]> = ref([]);
const loggedAdmin: Ref<any> = ref({});
const nextPlannifiedDate: Ref<number[]> = ref([]);

refreshNextPlannifiedDate();

function createDocumentation() {
  const tomorow = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
  return {
    name: "Titre de votre communication",
    content: "<h1>Partie 1</h1><p>Le corps de votre <b>communication</b>",
    lakeIds: [],
    isNational: false,
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

function computeIsPublicAndMiniatureURLs(actualites: any[]) {
  actualites.forEach(actualite => {
    const now = LocalDateTime.now(ZoneOffset.UTC);
    const dateDebut = parseLocalDateTime(actualite.datePublicationDebut);
    const dateFin = parseLocalDateTime(actualite.datePublicationFin);

    actualite.isPublic =
      now.isAfter(LocalDateTime.from(nativeJs(dateDebut))) &&
      now.isBefore(LocalDateTime.from(nativeJs(dateFin)));

    actualite.miniatureURL = actualite.miniatureId
      ? Constants.apiUrl("/v1/news-picture/" + actualite.miniatureId)
      : "";

    actualite.lakeNames = actualite.isNational ?
      "National" :
      actualite.lakeIds.map((lakeId: string) => lakesIdToNameMap.get(lakeId)).join(", ");
  });
}

function sendNotification(actualite: any) {
  Dialog.confirm({
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
        Toast.open({
          type: "is-success",
          message: "Notification envoyée"
        });
        refreshNextPlannifiedDate();
      } catch (e) {
        Toast.open({
          message:
            "Une erreur est survenue lors de l'envoi de la notification",
          type: "is-danger"
        });
      }
    }
  });
}

function parseLocalDateTime(someLocalDateTime: number[]): Date {
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

async function refreshNextPlannifiedDate(): Promise<void> {
  try {
    loggedAdmin.value = await BackendService.backendGet("/v1/admin/check");
    const lakes = await BackendService.backendGet("/v1/referential/lakes");
    lakes.forEach((lake: { id: string; name: string }) => {
      lakesIdToNameMap.set(lake.id, lake.name);
    });
    lakes.forEach((l: any) => {
      lakesOptions.push({
        id: l.id,
        label: l.name,
      })
    });

    docColumns.value = [
      {
        field: "id",
        label: "Identifiant technique",
        visible: false,
        readOnly: true,
        hiddenInPopup: true
      },
      {
        field: "lakeNames",
        label: "Plans d'eau",
        searchable: true,
        hiddenInPopup: true
      },
      {
        field: "name",
        label: "Nom",
        searchable: true,
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
        field: "isNational",
        isABoolean: true,
        label: "National (concerne tous les plans d'eau)",
        visible: false,
        showItemIfFunction: (news: News) => {
          return loggedAdmin.value.isNationalAdmin;
        },
      },
      {
        field: "lakeIds",
        label: "Plans d'eau",
        isArray: true,
        visible: false,
        showItemIfFunction: (news: News) => {
          return !news.isNational
        },
        possibleValuesForItemFunction: (news: News) => {
          return news.lakeIds ?? [];
        },
        arrayOptions: lakesOptions
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

    const nextCheckDate = await BackendService.backendGet("/v1/news-notifications/next-check")
    nextPlannifiedDate.value = nextCheckDate;
  } catch (error) {
    Toast.open({
      message: "Vous n'êtes plus connecté\u00B7e",
      type: "is-danger"
    });
    await router.push("/login");
  }
}
</script>
