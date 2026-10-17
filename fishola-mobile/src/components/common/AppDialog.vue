<!--
  #%L
  Carnet de pêche :: Mobile
  %%
  Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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
<!--
  Boîte de dialogue de l'application (#284), pilotée par DialogService :
  reprend le rendu et le comportement du dialogue de vue-js-modal (Vue 2
  uniquement) — titre et texte en HTML, boutons de largeur égale, Échap ou
  clic sur le fond pour fermer, Entrée pour valider un bouton unique.
-->
<template>
  <div class="app-dialog-overlay" v-if="state.visible && state.params" v-on:mousedown.self="close">
    <div class="app-dialog" role="dialog" aria-modal="true">
      <div class="dialog-content">
        <div class="dialog-c-title" v-if="state.params.title" v-html="state.params.title"></div>
        <div class="dialog-c-text" v-html="state.params.text"></div>
      </div>
      <div class="dialog-buttons">
        <button
          type="button"
          class="dialog-button"
          v-for="(button, index) in state.params.buttons"
          :key="index"
          :style="buttonStyle"
          v-html="button.title"
          v-on:click.stop="button.handler()"
        ></button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-facing-decorator";
import DialogService from "@/services/DialogService";

@Component
export default class AppDialog extends Vue {
  state = DialogService.state;

  get buttonStyle(): { flex: string } {
    const count = this.state.params?.buttons.length || 1;
    return { flex: `1 1 ${100 / count}%` };
  }

  mounted() {
    window.addEventListener("keyup", this.onKeyUp);
  }

  beforeUnmount() {
    window.removeEventListener("keyup", this.onKeyUp);
  }

  close() {
    DialogService.hide();
  }

  onKeyUp(event: KeyboardEvent) {
    if (!this.state.visible || !this.state.params) {
      return;
    }
    if (event.key === "Escape") {
      this.close();
    } else if (event.key === "Enter" && this.state.params.buttons.length === 1) {
      this.state.params.buttons[0].handler();
    }
  }
}
</script>

<style lang="less">
.app-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 999;
  width: 100vw;
  height: 100vh;
  background-color: @black-alpha-50;

  .app-dialog {
    position: absolute;
    top: calc(100vh / 2 - 80px);
    left: 50%;
    transform: translateX(-50%);
    width: 270px;
    max-width: calc(100vw - 20px);
    overflow: hidden;
    box-sizing: border-box;
    border-radius: 13px;
    background-color: @white-smoke;
    box-shadow: 0 20px 60px -2px rgba(27, 33, 58, 0.4);

    @media screen and (min-width: @desktop-min-width) {
      width: 350px;
    }

    div {
      box-sizing: border-box;
    }

    .dialog-content {
      width: 100%;
      padding: 15px;
      text-align: center;

      @media screen and (min-width: @desktop-min-width) {
        padding: 20px;
      }

      .dialog-c-title {
        padding-bottom: 15px;
        font-weight: 600;
        color: @gunmetal;
        font-size: @fontsize-dialog-title;
        line-height: calc(@fontsize-dialog-title + @line-height-padding-large);

        @media screen and (min-width: @desktop-min-width) {
          font-size: calc(@fontsize-dialog-title-desktop);
          line-height: calc(@fontsize-dialog-title-desktop + @line-height-padding-large);
        }
      }

      .dialog-c-text {
        color: @pale-sky;
        font-size: calc(@fontsize-dialog-text);
        line-height: calc(@fontsize-dialog-text + @line-height-padding-large);

        @media screen and (min-width: @desktop-min-width) {
          font-size: calc(@fontsize-dialog-text-desktop);
          line-height: calc(@fontsize-dialog-text-desktop + @line-height-padding-large);
        }
      }

      ul {
        text-align: left;
      }
    }

    .dialog-buttons {
      display: flex;
      width: 100%;
      color: @accent-text;
      border-top: 1px solid @very-light-grey;

      button.dialog-button {
        height: 40px;
        margin: 0;
        padding: 0;
        border: 0;
        background: transparent;
        color: inherit;
        font: inherit;
        font-size: @fontsize-dialog-button;
        line-height: 40px;
        cursor: pointer;
        outline: none;

        &:active {
          background: rgba(0, 0, 0, 0.025);
        }

        &:not(:first-of-type) {
          border-left: 1px solid @very-light-grey;
        }
      }
    }
  }
}
</style>
