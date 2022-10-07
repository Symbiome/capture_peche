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
  <div class="referential-item">
    <h2 v-if="item.name">{{ item.name }}</h2>
    <div v-for="col in columns" v-bind:key="col.field">
      <b-field
        :label="
          col.field == 'datePublicationDebut'
            ? 'Période de publication'
            : col.label
        "
        v-if="!col.isAPeriodEnd"
      >
        <!-- HTML text -->
        <div v-if="col.isHTML">
          <EditorContent :editor="editor" />
        </div>
        <!-- DateTime -->
        <div v-else-if="col.isADate">
          {{ formatDate(item[col.field]) }}
        </div>

        <!-- DateRange -->
        <div v-else-if="col.isAPeriodBeginning">
          <b-datepicker
            v-model="dateRange"
            placeholder="Type or select a date..."
            icon="calendar-today"
            locale="fr-FR"
            range
            editable
          >
          </b-datepicker>
        </div>
        <!-- Short strings -->
        <b-input
          v-model="item[col.field]"
          v-else-if="
            !col.isFile &&
              !col.isABoolean &&
              !col.isADate &&
              ('' + item[col.field]).length < 200
          "
          :disabled="col.readOnly || (col.readOnlyEdition && item['id'])"
        ></b-input>

        <!-- Long strings -->
        <b-input
          v-model="item[col.field]"
          type="textarea"
          v-else-if="
            !col.isFile &&
              !col.isABoolean &&
              !col.isADate &&
              ('' + item[col.field]).length >= 200
          "
          :disabled="col.readOnly || (col.readOnlyEdition && item['id'])"
        ></b-input>

        <!-- Files -->
        <b-upload
          v-model="input.file"
          v-else-if="col.isFile"
          accept="application/pdf"
        >
          <a class="button is-primary">
            <b-icon icon="upload"></b-icon>
            <span>Cliquez pour téléverser un nouveau fichier</span>
          </a>
          <a
            v-if="!input.file && item[col.field] && item[col.field].length > 10"
            :href="item[col.field]"
            target="blank"
          >
            Voir le fichier actuel
          </a>
          <span v-if="input.file" :href="item[col.field]" target="blank">
            {{ input.file.name }}
          </span>
        </b-upload>

        <!-- Booleans -->
        <div v-else-if="col.isABoolean">
          <b-radio
            v-model="item[col.field]"
            :name="col.field"
            :native-value="true"
          >
            Oui
          </b-radio>
          <b-radio
            v-model="item[col.field]"
            :name="col.field"
            :native-value="false"
          >
            Non
          </b-radio>
        </div>
      </b-field>
    </div>
    <div class="buttons">
      <button
        v-if="!item.id"
        class="button is-primary"
        @click="save($parent.close)"
      >
        Créer
      </button>
      <button
        v-if="item.id"
        class="button is-primary"
        @click="save($parent.close)"
      >
        Enregistrer
      </button>
      <button class="button" type="button" @click="onSaved($parent.close)">
        Annuler
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";

import BackendService from "@/services/BackendService";

import { Editor, EditorContent } from "@tiptap/vue-2";
import StarterKit from "@tiptap/starter-kit";
import { LocalDateTime, Instant, ZoneOffset, nativeJs } from "@js-joda/core";

@Component({
  components: { EditorContent }
})
export default class RefenretialItem extends Vue {
  @Prop() item!: any;
  @Prop() columns!: any[];
  @Prop() backendUrl!: string;
  input = { file: null };
  editor = new Editor({
    content: "",
    extensions: [StarterKit]
  });
  dateRange: Date[] = [];

  mounted(): void {
    this.itemChanged();
  }

  @Watch("item")
  itemChanged(): void {
    // Search for an html column (only one per item permited)
    let htmlColumns = this.columns.filter(c => c.isHTML);
    if (htmlColumns.length > 0) {
      let htmlContent = this.item[htmlColumns[0].field];
      // Update editor accordingly
      if (htmlContent) {
        this.editor.commands.setContent(htmlContent, false);
      }
    }

    // Initialize dateRange
    let rangeBeginningColumn = this.columns.filter(c => c.isAPeriodBeginning);
    let rangeEndColumn = this.columns.filter(c => c.isAPeriodEnd);
    if (rangeBeginningColumn.length && rangeEndColumn.length) {
      this.dateRange = [];
      this.dateRange.push(
        this.parseLocalDateTime(this.item[rangeBeginningColumn[0].field])
      );
      this.dateRange.push(
        this.parseLocalDateTime(this.item[rangeEndColumn[0].field])
      );
    }
  }

  beforeDestroy() {
    this.editor?.destroy();
  }

  save(closeModal: () => void) {
    // Search for an html column (only one per item permited)
    let htmlColumns = this.columns.filter(c => c.isHTML);
    if (htmlColumns.length > 0) {
      // Get editor value
      this.item[htmlColumns[0].field] = this.editor.getHTML();
    }

    // Translate dateRange into beggining/end dates
    let rangeBeginningColumn = this.columns.filter(c => c.isAPeriodBeginning);
    let rangeEndColumn = this.columns.filter(c => c.isAPeriodEnd);

    this.dateRange[0].setHours(7);
    this.dateRange[0].setMinutes(30);
    this.item[rangeBeginningColumn[0].field] = LocalDateTime.from(
      nativeJs(this.dateRange[0])
    );

    this.dateRange[1].setHours(21);
    this.dateRange[1].setMinutes(0);
    this.item[rangeEndColumn[0].field] = LocalDateTime.from(
      nativeJs(this.dateRange[1])
    );

    let onSavedCallback = () => {
      this.$emit("referential-updated");
      this.input.file = null;
      this.$buefy.toast.open({
        message: "Modifications enregistrées",
        type: "is-success"
      });
      if (closeModal) {
        closeModal();
      }
    };
    // Convert file in base64 (if required)
    if (this.input.file) {
      this.getBase64(this.input.file).then(
        base64 => {
          this.item["base64Content"] = base64;
          this.doSave(onSavedCallback);
        },
        err => {
          this.$buefy.toast.open({
            message: "Erreur lors de la lecture du fichier.",
            type: "is-danger"
          });
        }
      );
    } else {
      this.doSave(onSavedCallback);
    }
  }

  doSave(onSavedCallback: () => void) {
    if (this.item.id) {
      // Update : PUT
      let url = this.backendUrl + "/" + this.item.id;
      BackendService.backendPut(url, this.item).then(onSavedCallback, err => {
        this.input.file = null;
        this.$buefy.toast.open({
          message:
            "Erreur lors de la modification de " +
            this.item["name"] +
            ". Veuillez vérifier vos modifications.",
          type: "is-danger"
        });
      });
    } else {
      // Create : POST
      let url = this.backendUrl;
      BackendService.backendPost(url, this.item).then(onSavedCallback, err => {
        this.input.file = null;
        this.$buefy.toast.open({
          message:
            "Erreur lors de la création. Veuillez vérifier qu'un élément avec ce nom n'existe pas déjà.",
          type: "is-danger"
        });
      });
    }
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

  formatDate(puet: any): string {
    let theDate: Date = this.parseLocalDateTime(puet);
    var hourOptions = {
      month: "numeric",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "numeric"
    };
    let result: string = theDate.toLocaleTimeString("fr-FR", hourOptions);
    return result;
  }

  getBase64(file: any): Promise<string> {
    return new Promise<any>((resolve, reject) => {
      var reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = function() {
        resolve(reader.result);
      };
      reader.onerror = function(error) {
        reject(error);
      };
    });
  }

  onSaved(closeModal: () => void) {
    this.$emit("referential-updated");
    this.input.file = null;
    if (closeModal) {
      closeModal();
    }
  }
}
</script>

<style lang="less">
@import "../less/main";

.referential-item {
  padding: 10px;

  h2 {
    font-size: 24px;
  }

  background-color: @white;

  display: flex;
  flex-direction: column;

  .buttons {
    margin-top: 20px;

    width: 100%;

    display: flex;
    flex-direction: row-reverse;

    button {
      margin-left: 5px;
      margin-right: 5px;
    }
  }
}
</style>
