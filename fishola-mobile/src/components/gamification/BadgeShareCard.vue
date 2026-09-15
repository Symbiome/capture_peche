<!--
  #%L
  Fishola :: Mobile
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
<template>
  <div
    class="badge-share page-with-header-and-footer"
    v-bind:class="display ? '' : 'badge-share-hidden'"
  >
    <div class="page badge-share-page">
      <div class="pane">
        <div class="pane-content rounded">
          <h1>Partager mon badge</h1>

          <div class="badge-card-wrapper">
            <div class="badge-card" ref="card" v-if="badge">
              <div class="badge-card-icon">{{ badge.icon }}</div>
              <div class="badge-card-name">{{ badge.name }}</div>
              <div class="badge-card-description">{{ badge.description }}</div>
              <div class="badge-card-pseudo" v-if="pseudo">{{ pseudo }}</div>
              <img
                class="badge-card-logo"
                src="/img/logo-FISHOLA-nom.svg"
                alt="FISHOLA"
              />
            </div>
          </div>

          <div class="buttons-bar hide-on-mobile">
            <div class="button button-primary">
              <button v-on:click="shareClicked">
                <i class="icon-share" /> Partager
              </button>
            </div>
            <div class="button button-secondary">
              <button v-on:click="close">Fermer</button>
            </div>
          </div>

          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter
      shortcuts="back"
      button-icon="icon-share"
      button-text="Partager"
      v-on:buttonClicked="shareClicked"
      back-event="onBackButton"
      v-on:onBackButton="close"
    />
  </div>
</template>

<script lang="ts">
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import { BadgeBean } from "@/pojos/BackendPojos";
import ProfileService from "@/services/ProfileService";
import ShareService from "@/services/ShareService";

import html2canvas from "html2canvas";

import { Component, Vue } from "vue-property-decorator";

const TARGET_SIZE_PX = 1080;

@Component({
  components: {
    FisholaFooter,
  },
})
export default class BadgeShareCard extends Vue {
  display = false;
  badge: BadgeBean | null = null;
  pseudo: string = "";

  mounted() {
    this.$root.$on("open-badge-share", (badge: BadgeBean) => {
      this.badge = badge;
      this.display = true;
      ProfileService.getProfile().then((profile) => {
        this.pseudo = profile.pseudo;
      });
    });
  }

  beforeDestroy() {
    this.$root.$off("open-badge-share");
  }

  close() {
    this.display = false;
  }

  shareClicked() {
    const cardElement = this.$refs.card as HTMLElement;
    if (!cardElement || !this.badge) {
      return;
    }
    const scale = TARGET_SIZE_PX / cardElement.offsetWidth;
    html2canvas(cardElement, { scale }).then((canvas: HTMLCanvasElement) => {
      const dataUrl = canvas.toDataURL("image/png");
      ShareService.shareGeneratedImage(dataUrl, `badge_${this.badge?.code}.png`);
    });
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.badge-share-hidden {
  display: none;
}

.badge-share {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 998;
  background: @white;
}

.badge-card-wrapper {
  display: flex;
  justify-content: center;
  margin: @vertical-margin-medium 0;
}

// Rendue à taille d'affichage réduite puis capturée par html2canvas avec un
// `scale` recalculé pour produire un export carré 1080x1080 (#146), quelle que
// soit la taille d'écran.
.badge-card {
  width: 280px;
  height: 280px;
  border-radius: 24px;
  background: linear-gradient(160deg, @pelorous, @cyprus);
  color: @white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: @vertical-margin-medium;
  box-sizing: border-box;

  .badge-card-icon {
    font-size: 4rem;
    line-height: 1;
    margin-bottom: @vertical-margin-small;
  }

  .badge-card-name {
    font-weight: bold;
    font-size: @fontsize-header-title-small;
    margin-bottom: @vertical-margin-x-small;
  }

  .badge-card-description {
    font-size: @fontsize-small-paragraph;
    opacity: 0.9;
  }

  .badge-card-pseudo {
    margin-top: @vertical-margin-small;
    font-size: @fontsize-small-paragraph;
    opacity: 0.8;
  }

  .badge-card-logo {
    margin-top: @vertical-margin-small;
    width: 90px;
  }
}
</style>
