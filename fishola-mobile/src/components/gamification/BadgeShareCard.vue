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
  Carte souvenir d'un badge (#275, carte de partage #146) : la photo, l'espèce,
  la taille, la date et le lieu des prises qui ont débloqué le badge, avec une
  mini-carte approximative (aucune position précise par défaut, option
  « Masquer le lieu »). Les données viennent de l'endpoint « souvenir », appelé
  à l'ouverture ; sans prise associée ou hors ligne, la carte générique
  (icône, nom, description) est conservée. Photos et carte sont converties en
  data URL avant la capture html2canvas (1080×1080) pour ne pas entacher le canvas.
  -->
<template>
  <div
    class="badge-share page-with-header-and-footer force-light-theme"
    v-bind:class="display ? '' : 'badge-share-hidden'"
  >
    <div class="page badge-share-page">
      <div class="pane">
        <div class="pane-content rounded">
          <h1>Partager mon badge</h1>

          <div class="badge-card-wrapper">
            <div class="badge-card" ref="card" v-if="badge && view" v-bind:class="'badge-card-on-' + background">
              <div v-if="background === 'photo'" class="badge-card-background" v-bind:style="cover(heroPhoto)" />
              <div v-else-if="background === 'map'" class="badge-card-background" v-bind:style="cover(mapImage)" />
              <div v-else-if="background === 'mosaic'" class="badge-card-mosaic" v-bind:style="mosaicGrid">
                <div
                  class="badge-card-tile"
                  v-for="aCatch in view.mosaicCatches"
                  :key="aCatch.id"
                  v-bind:style="cover(photosByCatchId[aCatch.id])"
                >
                  <span v-if="!photosByCatchId[aCatch.id]">{{ aCatch.speciesName }}</span>
                </div>
              </div>
              <div v-if="background !== 'gradient'" class="badge-card-shade" />

              <div
                v-if="background !== 'gradient'"
                class="badge-card-header"
                v-bind:class="{ 'badge-card-header-beside-map': showInsetMap }"
              >
                <span class="badge-card-header-icon">{{ badge.icon }}</span>
                <span class="badge-card-header-name">{{ badge.name }}</span>
              </div>
              <div v-if="showInsetMap" class="badge-card-inset-map" v-bind:style="cover(mapImage)" />

              <div class="badge-card-body">
                <template v-if="background === 'gradient'">
                  <div class="badge-card-icon">{{ badge.icon }}</div>
                  <div class="badge-card-name">{{ badge.name }}</div>
                </template>
                <div class="badge-card-headline">{{ view.headline }}</div>
                <div class="badge-card-line" v-if="view.detail(hidePlace)">{{ view.detail(hidePlace) }}</div>
                <div class="badge-card-line" v-if="view.placeLine(hidePlace)">{{ view.placeLine(hidePlace) }}</div>
                <div class="badge-card-signature">
                  <span class="badge-card-pseudo" v-if="pseudo">{{ pseudo }}</span>
                  <CarnetLogo class="badge-card-logo" :onDark="true" />
                </div>
              </div>
            </div>
          </div>

          <div class="badge-share-preparing" v-if="preparing">
            <i class="icon-clock" /> Préparation de votre carte souvenir…
          </div>

          <div class="form-checkbox badge-share-hide-place" v-if="view && view.hasPlace">
            <input type="checkbox" id="badge-share-hide-place" class="pelorous-checkbox" v-model="hidePlace" />
            <label for="badge-share-hide-place"></label>
            <label for="badge-share-hide-place" class="real-label">Masquer le lieu</label>
          </div>

          <!-- Rendu hors écran de la mini-carte, capturée en image puis démontée. -->
          <div class="badge-share-map-renderer" v-if="renderingMap && view">
            <TripPositionsMap
              ref="map"
              v-bind:key="session"
              v-bind:catches="view.mapPoints"
              v-bind:captureMode="true"
              v-bind:approximate="true"
            />
          </div>

          <div class="buttons-bar hide-on-mobile">
            <div class="button button-primary">
              <button v-bind:disabled="preparing" v-on:click="shareClicked">
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
    <CarnetFooter
      shortcuts="back"
      button-icon="icon-share"
      button-text="Partager"
      v-bind:hideButton="preparing"
      v-on:buttonClicked="shareClicked"
      back-event="onBackButton"
      v-on:onBackButton="close"
    />
  </div>
</template>

<script lang="ts">
import CarnetLogo from "@/components/layout/CarnetLogo.vue";
import CarnetFooter from "@/components/layout/CarnetFooter.vue";
import TripPositionsMap from "@/components/trip/TripPositionsMap.vue";

import { BadgeBean, BadgeSouvenirBean, SouvenirCatchBean } from "@/pojos/BackendPojos";
import BadgeSouvenir from "@/services/BadgeSouvenir";
import GamificationService from "@/services/GamificationService";
import PicturesService from "@/services/PicturesService";
import ProfileService from "@/services/ProfileService";
import ShareService from "@/services/ShareService";

import html2canvas from "html2canvas";

import { Component, Vue } from "vue-facing-decorator";
import EventBus from "@/services/EventBus";

const TARGET_SIZE_PX = 1080;
// Garde-fou : une carte qui ne se charge pas ne doit pas bloquer le partage.
const MAP_CAPTURE_TIMEOUT_MS = 15000;

type CardBackground = "gradient" | "photo" | "mosaic" | "map";

@Component({
  components: {
    CarnetLogo,
    CarnetFooter,
    TripPositionsMap,
  },
})
export default class BadgeShareCard extends Vue {
  display = false;
  badge: BadgeBean | null = null;
  pseudo: string = "";
  souvenir: BadgeSouvenirBean | null = null;
  photosByCatchId: { [catchId: string]: string } = {};
  mapImage: string | null = null;
  hidePlace = false;
  preparing = false;
  renderingMap = false;
  // Incrémenté à chaque ouverture : un chargement d'une ouverture précédente est ignoré.
  session = 0;

  mounted() {
    EventBus.on("open-badge-share", this.open);
  }

  beforeUnmount() {
    EventBus.off("open-badge-share", this.open);
  }

  open(badge: BadgeBean) {
    this.session++;
    this.badge = badge;
    this.souvenir = null;
    this.photosByCatchId = {};
    this.mapImage = null;
    this.hidePlace = false;
    this.renderingMap = false;
    this.display = true;
    ProfileService.getProfile().then((profile) => {
      this.pseudo = profile.pseudo;
    });
    this.prepareSouvenir(badge, this.session);
  }

  close() {
    this.display = false;
    this.renderingMap = false;
    this.session++;
  }

  get view(): BadgeSouvenir | null {
    return this.badge ? new BadgeSouvenir(this.badge, this.souvenir) : null;
  }

  get heroPhoto(): string | null {
    const hero = this.view && this.view.heroCatch;
    return (hero && this.photosByCatchId[hero.id]) || null;
  }

  get background(): CardBackground {
    const layout = this.view ? this.view.layout : "generic";
    if (layout === "mosaic") {
      return "mosaic";
    }
    if (layout === "catch" && this.heroPhoto) {
      return "photo";
    }
    if (layout !== "generic" && this.mapImage && !this.hidePlace) {
      return "map";
    }
    return "gradient";
  }

  get mosaicGrid(): any {
    const view = this.view!;
    return {
      gridTemplateColumns: `repeat(${view.mosaicColumns}, 1fr)`,
      gridTemplateRows: `repeat(${view.mosaicRows}, 1fr)`,
    };
  }

  get showInsetMap(): boolean {
    return this.background === "photo" && !!this.mapImage && !this.hidePlace;
  }

  cover(dataUrl: string | null | undefined): any {
    return dataUrl ? { backgroundImage: `url("${dataUrl}")` } : {};
  }

  private isCurrent(session: number): boolean {
    return session === this.session;
  }

  private async prepareSouvenir(badge: BadgeBean, session: number) {
    if (!badge.unlockId || badge.competitionId) {
      return;
    }
    this.preparing = true;
    try {
      const souvenir = await GamificationService.getBadgeSouvenir(badge.unlockId);
      if (!this.isCurrent(session)) {
        return;
      }
      this.souvenir = souvenir;
      await Promise.all([this.loadPhotos(session), this.captureMap(session)]);
    } catch (error) {
      console.error("Carte souvenir indisponible, repli sur la carte générique", error);
    } finally {
      if (this.isCurrent(session)) {
        this.preparing = false;
      }
    }
  }

  private async loadPhotos(session: number) {
    const view = this.view!;
    const catches: SouvenirCatchBean[] =
      view.layout === "mosaic" ? view.mosaicCatches : view.heroCatch ? [view.heroCatch] : [];
    const photos = await Promise.all(
      catches.map((c) => PicturesService.getCatchPictureDataUrl(c.id, c.hasPicture))
    );
    if (!this.isCurrent(session)) {
      return;
    }
    const photosByCatchId: { [catchId: string]: string } = {};
    catches.forEach((c, index) => {
      const photo = photos[index];
      if (photo) {
        photosByCatchId[c.id] = photo;
      }
    });
    this.photosByCatchId = photosByCatchId;
  }

  // Même mécanique que l'export PDF (#173) : la carte MapLibre (mode capture,
  // ici approximatif) est rendue, attendue jusqu'à `idle`, puis lue en image.
  private async captureMap(session: number) {
    if (!this.view || !this.view.mapPoints.length) {
      return;
    }
    this.renderingMap = true;
    await this.$nextTick();
    await this.$nextTick();
    await this.$nextTick();
    const mapComponent = this.$refs.map as TripPositionsMap | undefined;
    const image = mapComponent ? await this.captureWithTimeout(mapComponent) : null;
    if (this.isCurrent(session)) {
      this.mapImage = image;
      this.renderingMap = false;
    }
  }

  private captureWithTimeout(mapComponent: TripPositionsMap): Promise<string | null> {
    const timeout = new Promise<null>((resolve) => setTimeout(() => resolve(null), MAP_CAPTURE_TIMEOUT_MS));
    return Promise.race([mapComponent.captureImage(), timeout]);
  }

  shareClicked() {
    const cardElement = this.$refs.card as HTMLElement;
    if (!cardElement || !this.badge || this.preparing) {
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
@card-size: 280px;
@card-padding: 14px;
@card-shade: rgba(20, 38, 52, 0.92);
@card-shade-light: rgba(20, 38, 52, 0.55);

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
// soit la taille d'écran. Les fonds sont des `background-image` (et non des
// <img> en object-fit, mal rendus par html2canvas).
.badge-card {
  position: relative;
  width: @card-size;
  height: @card-size;
  border-radius: 24px;
  overflow: hidden;
  background: linear-gradient(160deg, @pelorous, @cyprus);
  color: @white;
  box-sizing: border-box;
}

.badge-card-background,
.badge-card-mosaic {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-size: cover;
  background-position: center;
}

.badge-card-mosaic {
  display: grid;
  gap: 2px;
}

.badge-card-tile {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 4px;
  background-color: rgba(255, 255, 255, 0.1);
  background-size: cover;
  background-position: center;
  font-size: 11px;
  font-weight: bold;
}

.badge-card-shade {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 65%;
  background: linear-gradient(to top, @card-shade, rgba(20, 38, 52, 0));
}

// Le nom du badge passe à la ligne plutôt que d'être tronqué : html2canvas
// ne dessine pas les points de suspension de `text-overflow`.
.badge-card-header {
  position: absolute;
  top: 12px;
  left: 12px;
  max-width: calc(100% - 24px);
  display: flex;
  align-items: center;
  padding: 4px 10px 4px 6px;
  border-radius: 12px;
  background: @card-shade-light;
  font-size: 11px;
  font-weight: bold;
  line-height: 1.2;
  box-sizing: border-box;

  &.badge-card-header-beside-map {
    max-width: calc(100% - 24px - 92px);
  }

  .badge-card-header-icon {
    font-size: 16px;
    margin-right: 4px;
  }
}

.badge-card-inset-map {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 84px;
  height: 84px;
  border-radius: 12px;
  border: 2px solid @white;
  box-sizing: border-box;
  background-size: cover;
  background-position: center;
}

.badge-card-body {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: @card-padding;
  display: flex;
  flex-direction: column;
}

.badge-card-headline {
  font-weight: bold;
  font-size: 18px;
  line-height: 1.2;
}

.badge-card-line {
  font-size: @fontsize-smallest-paragraph;
  opacity: 0.9;
  margin-top: 2px;
}

.badge-card-signature {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;

  .badge-card-pseudo {
    font-size: @fontsize-smallest-paragraph;
    opacity: 0.85;
  }

  .badge-card-logo {
    width: 80px;
    margin-left: auto;
  }
}

// Sans photo ni carte : présentation centrée de la carte de partage d'origine (#146).
.badge-card-on-gradient .badge-card-body {
  top: 0;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: @vertical-margin-medium;

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

  .badge-card-headline {
    font-weight: normal;
    font-size: @fontsize-small-paragraph;
    opacity: 0.9;
  }

  .badge-card-signature {
    flex-direction: column;
    margin-top: @vertical-margin-small;

    .badge-card-logo {
      width: 96px;
      margin: @vertical-margin-small 0 0;
    }
  }
}

.badge-share-preparing {
  text-align: center;
  color: @pale-sky;
  font-size: @fontsize-small-paragraph;
  margin-bottom: @vertical-margin-small;
}

.badge-share-hide-place {
  display: flex;
  justify-content: center;
  margin-bottom: @vertical-margin-small;
}

// Hors écran mais mise en page réelle : MapLibre a besoin d'un conteneur
// dimensionné pour dessiner la carte à capturer (2× la carte affichée).
.badge-share-map-renderer {
  position: fixed;
  top: 0;
  left: -10000px;
  width: 2 * @card-size;
}
</style>
