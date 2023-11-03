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
  <div class="referential-aCatch">
    <h2 v-if="trip.name">{{ trip.name }}</h2>
    <div>
      <b-field label="Identifiant">
        <b-input v-model="aCatch.id" />
      </b-field>
      <b-field label="Image">
        <img
          class="miniture-pic"
          v-if="aCatch['miniatureURL']"
          :src="aCatch['miniatureURL']"
        />
      </b-field>

      <b-field label="Bool">
        <b-radio
          v-model="aCatch.excludeFromExport"
          name="excludeFromExport"
          :native-value="true"
        >
          Oui
        </b-radio>
        <b-radio
          v-model="aCatch.excludeFromExport"
          name="excludeFromExport"
          :native-value="false"
        >
          Non
        </b-radio>
      </b-field>
      <div class="spacer" />
    </div>

    {{ aCatch }}
    {{ trip }}
    <div class="buttons">
      <button
        v-if="aCatch.id"
        class="button is-primary"
        @click="save($parent.close)"
      >
        Enregistrer
      </button>
      <button
        class="button"
        type="button"
        @click="cancelAndClose($parent.close)"
      >
        Annuler
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";

import BackendService from "@/services/BackendService";

@Component({
  components: {}
})
export default class CatchModal extends Vue {
  @Prop() catchId: string;
  aCatch: any = {};
  trip: any = {};

  mounted(): void {
    this.loadCatch();
  }

  async loadCatch() {
    this.trip = await await BackendService.backendGet(
      "/v1/trips/catches/" + this.catchId
    );
    this.aCatch = this.trip.catchs.find(c => c.id == this.catchId);
  }

  async save(closeModal: () => void) {
    let url = "v1/trips/catches/" + this.catchId;
    try {
      await BackendService.backendPut(url, this.aCatch);
      this.$emit("referential-updated");
      if (closeModal) {
        closeModal();
      }
    } catch (e) {
      console.error(e);
      this.$buefy.toast.open({
        message:
          "Erreur lors de la modification de la prise. Veuillez vérifier vos modifications.",
        type: "is-danger"
      });
    }
  }

  cancelAndClose(closeModal: () => void) {
    this.$emit("referential-updated");
    if (closeModal) {
      closeModal();
    }
  }
}
</script>

<style lang="less">
@import "../less/main";

.referential-aCatch {
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
      padding: 40px;
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
