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
  <div class="referential-item" v-if="item">
    <h2 v-if="item.name">{{ item.name }}</h2>
    <div v-for="col in columns" v-bind:key="col.field">

      <b-field
        :label="
          col.field == 'datePublicationDebut'
            ? 'Période de publication'
            : col.label
        "
        :message="col.helpMessage ? col.helpMessage : null"
        v-if="!col.hiddenInPopup && (col.showItemIfFunction === undefined || col.showItemIfFunction(item))"
      >
        <!-- HTML text -->
        <div v-if="col.isHTML" class="editor-holder">
          <div v-if="editor">
            <div class="editor-buttons-holder">
              <button
                @click="
                  editor
                    .chain()
                    .focus()
                    .setParagraph()
                    .run()
                "
                class="editor-button"
                :class="{ 'is-active': editor.isActive('paragraph') }"
              >
                Paragraphe
              </button>

              <button
                @click="
                  editor
                    .chain()
                    .focus()
                    .toggleHeading({ level: 1 })
                    .run()
                "
                class="editor-button"
                :class="{
                  'is-active': editor.isActive('heading', { level: 1 })
                }"
              >
                Titre 1
              </button>
              <button
                @click="
                  editor
                    .chain()
                    .focus()
                    .toggleHeading({ level: 2 })
                    .run()
                "
                class="editor-button"
                :class="{
                  'is-active': editor.isActive('heading', { level: 2 })
                }"
              >
                Titre 2
              </button>

              <button
                @click="
                  editor
                    .chain()
                    .focus()
                    .toggleBulletList()
                    .run()
                "
                class="editor-button"
                :class="{ 'is-active': editor.isActive('bulletList') }"
              >
                Liste
              </button>

              <button
                @click="
                  editor
                    .chain()
                    .focus()
                    .toggleBlockquote()
                    .run()
                "
                class="editor-button"
                :class="{ 'is-active': editor.isActive('blockquote') }"
              >
                Citation
              </button>
              <button
                @click="
                  editor
                    .chain()
                    .focus()
                    .setHorizontalRule()
                    .run()
                "
              >
                Ligne Horizontale
              </button>
              <button
                @click="
                  editor
                    .chain()
                    .focus()
                    .toggleBold()
                    .run()
                "
                :disabled="
                  !editor
                    .can()
                    .chain()
                    .focus()
                    .toggleBold()
                    .run()
                "
                class="editor-button"
                :class="{ 'is-active': editor.isActive('bold') }"
              >
                Gras
              </button>
              <button
                @click="
                  editor
                    .chain()
                    .focus()
                    .toggleItalic()
                    .run()
                "
                :disabled="
                  !editor
                    .can()
                    .chain()
                    .focus()
                    .toggleItalic()
                    .run()
                "
                class="editor-button"
                :class="{ 'is-active': editor.isActive('italic') }"
              >
                Italique
              </button>
              <button
                @click="
                  editor
                    .chain()
                    .focus()
                    .undo()
                    .run()
                "
                :disabled="
                  !editor
                    .can()
                    .chain()
                    .focus()
                    .undo()
                    .run()
                "
                class="editor-button"
              >
                Annuler
              </button>
              <button
                @click="
                  editor
                    .chain()
                    .focus()
                    .redo()
                    .run()
                "
                :disabled="
                  !editor
                    .can()
                    .chain()
                    .focus()
                    .redo()
                    .run()
                "
                class="editor-button"
              >
                Rétablir
              </button>
              <ImageUploader
                :item-id="item['id']"
                @uploaded-pic="uploadedPic"
              />
            </div>
          </div>
          <EditorContent class="editor" :editor="editor" />
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
              !col.isPicture &&
              !col.isABoolean &&
              !col.isADate &&
              !col.isArray &&
              ('' + item[col.field]).length < 200
          "
          :disabled="col.readOnly || col.readOnlyIfFunction && col.readOnlyIfFunction(item) || (col.readOnlyEdition && item['id'])"
        ></b-input>

        <!-- Long strings -->
        <b-input
          v-model="item[col.field]"
          type="textarea"
          v-else-if="
            !col.isFile &&
              !col.isPicture &&
              !col.isABoolean &&
              !col.isADate &&
              !col.isArray &&
              ('' + item[col.field]).length >= 200
          "
          :disabled="col.readOnly || (col.readOnlyEdition && item['id'])"
        ></b-input>

        <!-- PDF Files -->
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

        <div v-else-if="col.isPicture">
          <img
            alt="miniature"
            class="miniture-pic"
            v-if="item['miniatureURL']"
            :src="item['miniatureURL']"
          />
          <ImageUploader
            :isMiniature="true"
            :item-id="item['id']"
            @uploaded-pic="uploadedMiniature"
          />
        </div>

        <!-- Array -->
        <MultipleAutoComplete
          v-else-if="col.isArray"
          :defaultSelection="col.possibleValuesForItemFunction ? col.possibleValuesForItemFunction(item) :  []"
          :data="col.arrayOptions"
          @updated="(value) => item[col.field] = value"
        />
        <!-- Booleans -->
        <div v-else-if="col.isABoolean">
          <b-radio
            v-model="item[col.field]"
            :name="col.field"
            :native-value="true"
            :disabled="col.readonly"
          >
            Oui
          </b-radio>
          <b-radio
            v-model="item[col.field]"
            :name="col.field"
            :native-value="false"
            :disabled="col.readonly"
          >
            Non
          </b-radio>
        </div>
      </b-field>
      <div class="spacer" />
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
import { Component, Prop, Vue, Watch } from "vue-facing-decorator";

import BackendService from "@/services/BackendService";

import { Editor, EditorContent } from "@tiptap/vue-3";
import Image from "@tiptap/extension-image";
import StarterKit from "@tiptap/starter-kit";

import { LocalDateTime, nativeJs } from "@js-joda/core";
import ImageUploader from "./ImageUploader.vue";
import MultipleAutoComplete from "./MultipleAutoComplete.vue";

@Component({
  components: { EditorContent, ImageUploader, MultipleAutoComplete }
})
export default class RefenretialItem extends Vue {
  @Prop() item!: any;
  @Prop() columns!: any[];
  @Prop() backendUrl!: string;
  input = { file: null };
  picture = { file: null };
  miniaturePicURL = "";
  editor = new Editor({
    content: "",
    extensions: [StarterKit, Image]
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

  uploadedPic(url: string): void {
    this.editor
      .chain()
      .focus()
      .setImage({ src: url })
      .run();
  }

  uploadedMiniature(url: string): void {
    this.item["miniatureURL"] = url;
    const splitted = url.split("/");
    this.item["miniatureId"] = splitted[splitted.length - 1];
    this.$forceUpdate();
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

    if (this.dateRange && this.dateRange[0] && this.dateRange[1]) {
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
    }
    let onSavedCallback = () => {
      this.$emit("referential-updated");
      this.input.file = null;
      this.picture.file = null;
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
    } else if (this.picture.file) {
      this.getBase64(this.picture.file).then(
        base64 => {
          this.item["miniaturePic"] = base64;
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
        this.picture.file = null;
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
        this.picture.file = null;
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
    const hourOptions: Intl.DateTimeFormatOptions = {
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
      const reader = new FileReader();
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
    this.picture.file = null;
    if (closeModal) {
      closeModal();
    }
  }
}
</script>

<style lang="less">

.referential-item {
  padding: 30px;

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

  .spacer {
    height: 8px;
  }

  .editor-holder {
    min-height: 200px;
    padding: 10px;
    border: 2px solid grey;
    border-radius: 20px;
  }

  .editor-buttons-holder {
    padding-bottom: 10px;
    .editor-button {
      margin-left: 2px;
      margin-right: 2px;
      border-radius: 2px;
    }
  }

  .editor {
    padding-left: 30px;
    li {
      list-style: circle;
      margin-left: 20px;
      padding-left: 5px;
    }

    img {
      max-height: 500px;
      max-width: 500px;
    }

    blockquote {
      min-height: 80px;
      position: relative;
      margin: 40px 0;
      padding: 1.6em 2.4em 0.7em calc(1.4em + var(--quote-image-width));
      font: italic 1.2rem var(--type-quote);
      background: var(--quote-bg) no-repeat left / var(--quote-image-width);
      border-radius: var(--border-rad);
      box-shadow: 2px 2px 4px hsl(0 0% 0% / 20%);
      text-indent: 1.6em;
    }

    @media (min-width: 768px) {
      blockquote {
        margin: 40px 60px;
      }
    }

    blockquote::before {
      content: "";
      pointer-events: none;
      position: absolute;
      z-index: 1;
      left: 0;
      top: 0;
      right: 0;
      bottom: 0;
      border-radius: var(--border-rad);
      box-shadow: inset -2px -2px 1px hsl(0 0% 100%),
        inset 2px 2px 4px hsl(0 0% 0% / 20%);
    }

    blockquote::after {
      content: "❝";
      position: absolute;
      z-index: 1;
      left: 50%;
      top: -2px;
      transform: translate(-50%, -50%);
      width: 1.3em;
      height: 1.3em;
      background: @pelorous;
      box-shadow: 0 4px 5px -1px hsla(0 0% 0% / 20%);
      border-radius: 999px;
      display: grid;
      place-content: center;
      padding-top: 0.5em;
      color: var(--accent-color);
      font-size: 36px;
      font-style: normal;
      text-indent: 0;
    }
  }
  .is-active {
    background-color: @pelorous;
    color: white;
    font-weight: bold;
  }
}

.miniture-pic {
  width: 80px;
  height: auto;
}
</style>
