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

function loadData() {
  while (data.value && data.value.length) {
    data.value.pop();
  }
  allowedDeletionElements.value = [];
  BackendService.backendGet(url).then(res => {
    data.value = res;
    selection.value.item = null;
    emit("elementsLoaded", data.value);
    checkCanDeletePredicate();
  });
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
function checkCanDeletePredicate() {
  if (canDelete && data.value) {
    data.value.forEach(element => {
      // Call predicate for each element
      if (canDeletePredicate != null) {
        canDeletePredicate(element).then(allowDeletion => {
          if (allowDeletion && allowedDeletionElements.value != null) {
            allowedDeletionElements.value.push(element["id"]);
          }
        });
      } else if (allowedDeletionElements.value != null) {
        allowedDeletionElements.value.push(element["id"]);
      }
    });
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
  } else {
    // Explain why we cannot delete
    Dialog.alert(
      "Impossible de supprimer cet élément car il est référencé ailleurs au sein de l'application"
    );
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
