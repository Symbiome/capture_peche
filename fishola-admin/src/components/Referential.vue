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
    <h1>{{ name }}</h1>
    <b-table
      :data="data"
      :striped="true"
      :default-sort="defaultSort"
      :selected.sync="selection.item"
      :loading="!data"
    >
      <template slot-scope="props">
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
          sortable
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
                >Envoyer maintenant</b-button
              >
            </span>
            <span v-else>
              Non envoyé
            </span>
          </span>
          <span v-else-if="col.isABoolean && !props.row[col.field]">
            Non
          </span>
          <span
            v-else-if="
              (col.isADate || col.isAPeriodBeginning || col.isAPeriodEnd) &&
                props.row[col.field]
            "
          >
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
          @click.native="showLink($event, props.row[col.field])"
          :key="col.name"
          sortable
        >
          <span v-if="col.isUrl">
            {{ props.row[col.field] }}
          </span>
          <button class="button is-small is-info">
            <b-icon icon="eye" size="is-small"></b-icon>
          </button>
        </b-table-column>
        <!-- Deletion button (only displayed if delete is allow) -->
        <b-table-column
          v-if="editable && canDelete"
          label="Action"
          @click.native="showDeleteDialog($event, props.row)"
        >
          <button
            v-if="allowedDeletionElements.includes(props.row['id'])"
            class="button is-small is-danger"
          >
            <b-icon icon="delete" size="is-small"></b-icon>
          </button>
          <button
            v-if="!allowedDeletionElements.includes(props.row['id'])"
            class="button is-small is-light"
          >
            <b-icon icon="delete-off" size="is-small"></b-icon>
          </button>
        </b-table-column>
      </template>
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
      :active.sync="selection.item"
      trap-focus
      :destroy-on-hide="false"
      aria-role="dialog"
      full-screen
      aria-modal
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

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";

import BackendService from "@/services/BackendService";

import ReferentialItem from "@/components/ReferentialItem.vue";
import UtilityServices from "@/services/UtilityServices";

@Component({
  components: {
    ReferentialItem
  }
})
export default class Refenretial extends Vue {
  @Prop() name!: string;
  @Prop() url!: string;
  @Prop() columns!: any[];
  @Prop() data: any[] = [];
  @Prop({ default: true }) editable: boolean;
  @Prop({ default: ["id", "desc"] }) defaultSort: string[];
  @Prop() nextPlannifiedDate: number[];
  selection = { item: null };

  /* The function used to create new elements. If not specified, create button will not be displayed */
  @Prop({ default: null }) createElement: () => any;
  /* Indicates whether user is allowed to deleted elements in the table. */
  @Prop({ default: false }) canDelete: boolean;
  /** The function used to determine if a given element can be deleted.
   * If not specified, only the "canDelete" boolean wil be used to determine if deletion is allowed.
   * */
  @Prop({ default: null }) canDeletePredicate: (
    elemenToDelete: any
  ) => Promise<boolean>;
  // Cached value of all elements for which deletion is allowed
  allowedDeletionElements: any[] = [];

  mounted() {
    this.loadData();
  }

  @Watch("nextPlannifiedDate")
  nextPlannifiedDateChanged(): void {
    this.loadData();
  }

  formatDate(puet: number[]): string {
    return UtilityServices.formatDate(puet);
  }

  loadData() {
    while (this.data && this.data.length) {
      this.data.pop();
    }
    this.allowedDeletionElements = [];
    BackendService.backendGet(this.url).then(res => {
      this.data = res;
      this.$emit("elementsLoaded", this.data);
      this.checkCanDeletePredicate();
    });
  }

  showCreateDialog() {
    let newElement = this.createElement();
    // This will trigger modal appearance
    this.selection.item = newElement;
  }

  sendNotification(target: any, event: Event) {
    event.stopPropagation();
    this.$emit("send-notification", target);
  }

  /**
   * If required by configuration, ask to server if delete is allowed.
   */
  checkCanDeletePredicate() {
    if (this.canDelete && this.data) {
      this.data.forEach(element => {
        // Call predicate for each element
        if (this.canDeletePredicate != null) {
          this.canDeletePredicate(element).then(allowDeletion => {
            if (allowDeletion && this.allowedDeletionElements != null) {
              this.allowedDeletionElements.push(element["id"]);
            }
          });
        } else {
          if (this.allowedDeletionElements != null) {
            this.allowedDeletionElements.push(element["id"]);
          }
        }
      });
    }
  }

  showLink(event: Event, url: string) {
    // Do not foward click event to row (would trigger modal)
    event.stopPropagation();

    window.open(url, "_blank");
  }

  showDeleteDialog(event: Event, element: any) {
    // Do not foward click event to row (would trigger modal)
    event.stopPropagation();

    if (
      this.allowedDeletionElements &&
      this.allowedDeletionElements.includes(element["id"])
    ) {
      // Ask for confirmation
      this.$buefy.dialog.confirm({
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
          BackendService.backendDelete(`${this.url}/${element["id"]}`).then(
            res => {
              this.$buefy.toast.open({
                message: (element["name"] || "Élément") + " supprimé",
                type: "is-success"
              });
              this.loadData();
            },
            error => {
              this.$buefy.toast.open({
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
      this.$buefy.dialog.alert(
        "Impossible de supprimer cet élément car il est référencé ailleurs au sein de l'application"
      );
    }
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
  }
  table {
    tr {
      td {
        overflow: hidden;
        max-width: 200px;
        white-space: nowrap;
        text-overflow: ellipsis;
      }
    }
  }
}
</style>
