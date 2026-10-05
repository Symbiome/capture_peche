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
  <div class="referential">
    <h1>{{ name }} <span class="count">({{ data.length }})</span></h1>
    <b-table
      :data="data"
      :striped="true"
      :default-sort="defaultSort"
      v-model:selected="selection.item"
      :loading="!data"
      :class="{ 'clickable-rows': editable }"
    >
      <b-table-column
        v-for="col in columns.filter(
          col =>
            col.visible !== false &&
            !col.isUrl &&
            !col.isFile &&
            !col.isHTML &&
            !col.isPicture
        )"
        :field="col.field"
        :label="col.label"
        :key="col.name"
        :searchable="col.searchable"
        sortable
        v-slot="props"
      >
        <span v-if="col.isABoolean && props.row[col.field]">
          Oui
        </span>
        <span v-else-if="col.isANotificationDate">
          <span v-if="props.row[col.field]">
            {{ formatDate(props.row[col.field]) }}
          </span>
          <span v-else-if="props.row['isPublic']">
            Plannifié le {{ formatDate(nextPlannifiedDate) }}
            <b-button
              class="button is-small is-primary"
              @click="sendNotification(props.row, $event)"
            >Envoyer
              maintenant</b-button>
          </span>
          <span v-else>
            Non envoyé
          </span>
        </span>
        <span v-else-if="col.isABoolean && !props.row[col.field]">
          Non
        </span>
        <span v-else-if="
          (col.isADate || col.isAPeriodBeginning || col.isAPeriodEnd) &&
          props.row[col.field]
        ">
          {{ formatDate(props.row[col.field]) }}
        </span>
        <span v-else-if="!col.isABoolean && !col.isADate">
          {{ props.row[col.field] }}
        </span>
      </b-table-column>
      <b-table-column
        v-for="col in columns.filter(
          col =>
            col.visible !== false &&
            (col.isUrl || col.isFile || col.isPicture)
        )"
        :field="col.field"
        :label="col.label"
        :key="col.name"
        sortable
        v-slot="props"
      >
        <div @click="showLink($event, props.row[col.field])">
          <span v-if="col.isUrl">
            {{ props.row[col.field] }}
          </span>
          <button class="button is-small is-info">
            <b-icon
              icon="eye"
              size="is-small"
            ></b-icon>
          </button>
        </div>
      </b-table-column>
      <!-- Deletion button (only displayed if delete is allow) -->
      <b-table-column
        v-if="editable && canDelete"
        label="Action"
        v-slot="props"
      >
        <div @click="showDeleteDialog($event, props.row)">
          <button
            v-if="allowedDeletionElements.includes(props.row['id'])"
            class="button is-small is-danger"
          >
            <b-icon
              icon="delete"
              size="is-small"
            ></b-icon>
          </button>
          <button
            v-if="!allowedDeletionElements.includes(props.row['id'])"
            class="button is-small is-light"
          >
            <b-icon
              icon="delete-off"
              size="is-small"
            ></b-icon>
          </button>
        </div>
      </b-table-column>
    </b-table>
    <div class="buttons">
      <!-- Creation button (only displayed if createElement is defined) -->
      <button
        class="button is-primary"
        v-if="editable && createElement != null"
        @click="showCreateDialog()"
      >
        Nouveau
      </button>
    </div>
    <b-modal
      v-if="editable"
      v-model="isItemSelected"
      trap-focus
      :destroy-on-hide="false"
      aria-role="dialog"
      full-screen
      :aria-modal="true"
    >
      <ReferentialItem
        :item="selection.item"
        :columns="columns"
        :backendUrl="url"
        v-on:referential-updated="loadData"
      >
      </ReferentialItem>
    </b-modal>
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";

import ReferentialItem from "@/components/ReferentialItem.vue";
import UtilityServices from "@/services/UtilityServices";
import { showLink } from "@/utils/utils";
import { computed, onMounted, ref, Ref, watch } from "vue";
import { useDialog, useToast } from "buefy";

const Dialog = useDialog();
const Toast = useToast();

interface Props {
  name: string;
  url: string;
  columns: any[];
  editable?: boolean;
  defaultSort?: string[];
  nextPlannifiedDate?: number[];
  /* The function used to create new elements. If not specified, create button will not be displayed */
  createElement?: (() => any) | null;
  /* Indicates whether user is allowed to deleted elements in the table. */
  canDelete?: boolean;
  /** The function used to determine if a given element can be deleted.
   * If not specified, only the "canDelete" boolean wil be used to determine if deletion is allowed.
   * */
  canDeletePredicate?: ((elementToDelete: any) => Promise<boolean>) | null;
  /** Elements used elsewhere can be archived instead of deleted (#202): the backend
   * exposes their usage at url/usage/id and an "archived" flag. */
  archivable?: boolean;
}

const {
  name,
  url,
  columns,
  editable = true,
  defaultSort = ["id", "desc"],
  nextPlannifiedDate = [],
  createElement = null,
  canDelete = false,
  canDeletePredicate = null,
  archivable = false,
} = defineProps<Props>();

const data = ref([]);
const selection = ref({ item: null });
// Cached value of all elements for which deletion is allowed
const allowedDeletionElements: Ref<any[]> = ref([]);
// Fix for something no longer working in Vue3:
// v-model value must be a valid JavaScript member expression.
const isItemSelected = computed(() => selection.value.item != null);

const emit = defineEmits<{
  (e: "elementsLoaded", data: any[]): void,
  (e: "sendNotification", notification: any): void,
}>();

onMounted(loadData);

watch(() => nextPlannifiedDate, (oldDate, newDate) => {
  loadData();
});

async function loadData() {
  while (data.value && data.value.length) {
    data.value.pop();
  }
  allowedDeletionElements.value = [];

  const res = await BackendService.backendGet(url);
  data.value = res;
  selection.value.item = null;
  emit("elementsLoaded", data.value);
  checkCanDeletePredicate();
}

function formatDate(puet: number[]): string {
  return UtilityServices.formatDate(puet);
}

function showCreateDialog() {
  const newElement = createElement?.();
  // This will trigger modal appearance
  selection.value.item = newElement;
}

function sendNotification(target: any, event: Event) {
  event.stopPropagation();
  emit("sendNotification", target);
}

/**
 * If required by configuration, ask to server if delete is allowed.
 */
async function checkCanDeletePredicate() {
  if (canDelete && data.value) {
    await Promise.all(data.value.map(async (element) => {
      // Call predicate for each element
      if (canDeletePredicate != null) {
        const allowDeletion = await canDeletePredicate(element);
        
        if (allowDeletion && allowedDeletionElements.value != null) {
          allowedDeletionElements.value.push(element["id"]);
        }
      } else if (allowedDeletionElements.value != null) {
        allowedDeletionElements.value.push(element["id"]);
      }
    }));
  }
}

function showDeleteDialog(event: Event, element: any) {
  // Do not foward click event to row (would trigger modal)
  event.stopPropagation();

  if (
    allowedDeletionElements.value &&
    allowedDeletionElements.value.includes(element["id"])
  ) {
    // Ask for confirmation
    Dialog.confirm({
      title: "Suppression",
      message:
        "Êtes-vous sûr de vouloir supprimer " +
        (element["name"] || "cet élément") +
        " ?",
      confirmText: "Supprimer",
      type: "is-danger",
      hasIcon: true,
      onConfirm: () => {
        // Sends an HTTP DELETE request at url/id
        BackendService.backendDelete(`${url}/${element["id"]}`).then(
          res => {
            Toast.open({
              message: (element["name"] || "Élément") + " supprimé",
              type: "is-success"
            });
            loadData();
          },
          error => {
            Toast.open({
              message:
                "Erreur lors de la supression de " +
                (element["name"] || "l'élément") +
                " : " +
                error.message,
              type: "is-danger"
            });
          }
        );
      }
    });
  } else if (archivable) {
    proposeArchiving(element);
  } else {
    // Explain why we cannot delete
    Dialog.alert(
      "Impossible de supprimer cet élément car il est référencé ailleurs au sein de l'application"
    );
  }
}

function escapeHtml(value: string): string {
  const div = document.createElement("div");
  div.textContent = value;
  return div.innerHTML;
}

function describeUsage(usage: { catches: number; trips: number }): string {
  const parts = [];
  if (usage.catches) parts.push(usage.catches + (usage.catches > 1 ? " captures" : " capture"));
  if (usage.trips) parts.push(usage.trips + (usage.trips > 1 ? " sorties" : " sortie"));
  return parts.join(" et ");
}

/** Used element (#202): explain where it is used and offer to archive it instead. */
async function proposeArchiving(element: any) {
  const label = "« " + escapeHtml(element["name"] || "Cet élément") + " »";
  const usage = await BackendService.backendGet(`${url}/usage/${element["id"]}`);
  const reason = label + " est utilisé par " + describeUsage(usage)
    + " : le supprimer ferait perdre cet historique.";
  if (element["archived"]) {
    Dialog.alert(reason + " Il est déjà archivé et n'est donc plus proposé à la saisie.");
    return;
  }
  Dialog.confirm({
    title: "Suppression impossible",
    message: reason + "<br><br>Vous pouvez l'<b>archiver</b> : il ne sera plus proposé à la saisie, "
      + "mais les données existantes sont conservées. Il pourra être restauré en décochant « Archivé ».",
    confirmText: "Archiver",
    cancelText: "Annuler",
    type: "is-warning",
    hasIcon: true,
    onConfirm: () => archive(element)
  });
}

async function archive(element: any) {
  try {
    await BackendService.backendPut(`${url}/${element["id"]}`, { ...element, archived: true });
    Toast.open({ message: (element["name"] || "Élément") + " archivé", type: "is-success" });
    loadData();
  } catch (error) {
    Toast.open({ message: "Erreur lors de l'archivage de " + (element["name"] || "l'élément"), type: "is-danger" });
  }
}
</script>

<style lang="less">
.referential {
  .buttons {
    width: 100%;
    display: flex;
    flex-direction: row-reverse;
    padding-right: 30px;
    padding-top: 10px;
    position: sticky;
    bottom: 0;
    background: linear-gradient(to bottom, #fff0, #fff);
  }

  table {
    tr {
      th {
        border-bottom-color: @pelorous;
      }

      td {
        overflow: hidden;
        max-width: 200px;
        white-space: nowrap;
        text-overflow: ellipsis;
      }
    }
  }

  // Une ligne ouvre la fiche d'édition (#201) : curseur main et léger
  // surlignage au survol, y compris sur les lignes zébrées.
  .clickable-rows table tbody tr {
    cursor: pointer;

    &:hover {
      background-color: fade(@pelorous, 10%) !important;
    }
  }

  h1 {
    margin-bottom: 10px;
  }

  .count {
    color: @pelorous;
    font-weight: 100;
    padding-left: 10px;
  }
}
</style>
