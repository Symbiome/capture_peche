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
<!--
  Export PDF d'une ou plusieurs sorties (#173) : carte (début/fin/captures),
  liste des prises et leurs photos, technique(s), durée. Ouvert via
  l'évènement `$root` `open-trip-pdf-export` (même pattern que
  BadgeShareCard.vue, #146) avec la liste des identifiants de sorties à
  exporter, depuis EditTrip.vue (une sortie) ou MyTrips.vue (une sélection).
  La génération se fait entièrement côté client (jsPDF), sans nouvel appel
  backend, une sortie à la fois (une page par sortie), chaque sortie étant
  rechargée en entier (`TripsService.getTrip`) car la liste des sorties ne
  transporte que des `TripLight` sans captures ni positions.
  -->
<template>
  <div class="trip-pdf-export page-with-header-and-footer" v-bind:class="display ? '' : 'trip-pdf-export-hidden'">
    <div class="page trip-pdf-export-page">
      <div class="pane">
        <div class="pane-content rounded">
          <h1>Exporter en PDF</h1>

          <div class="trip-pdf-export-summary">
            <div class="trip-pdf-export-name" v-if="tripIds.length <= 1">Sortie sélectionnée</div>
            <div class="trip-pdf-export-name" v-else>{{ tripIds.length }} sorties sélectionnées</div>
          </div>

          <!--
            Une seule carte à la fois, réellement rendue (pas juste affichée
            en aperçu) : c'est elle qui sera capturée par `captureImage()`
            pour le PDF, d'où `captureMode` (préserve le tampon WebGL, cf.
            TripPositionsMap.vue). La `key` force un remontage complet à
            chaque sortie (nouvelle instance MapLibre, recentrage inclus)
            plutôt que de réutiliser l'instance précédente.
          -->
          <TripPositionsMap
            v-if="currentTrip"
            v-bind:key="currentTrip.id"
            ref="map"
            v-bind:beginLatitude="currentTrip.beginLatitude"
            v-bind:beginLongitude="currentTrip.beginLongitude"
            v-bind:endLatitude="currentTrip.endLatitude"
            v-bind:endLongitude="currentTrip.endLongitude"
            v-bind:catches="catchPoints"
            v-bind:captureMode="true"
          />

          <div class="trip-pdf-export-generating" v-if="generating">
            <i class="icon-clock" /> {{ generatingLabel }}
          </div>

          <div class="buttons-bar hide-on-mobile">
            <div class="button button-primary">
              <button v-bind:disabled="generating" v-on:click="generateClicked">
                <i class="icon-download" /> Générer le PDF
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
      button-icon="icon-download"
      button-text="Générer le PDF"
      v-bind:hideButton="generating"
      v-on:buttonClicked="generateClicked"
      back-event="onBackButton"
      v-on:onBackButton="close"
    />
  </div>
</template>

<script lang="ts">
import FisholaFooter from "@/components/layout/FisholaFooter.vue";
import TripPositionsMap, { TripPositionsCatchPoint } from "@/components/trip/TripPositionsMap.vue";

import { CatchBean, TripBean } from "@/pojos/BackendPojos";
import { SpeciesWithAliasAndTechnique } from "@/services/ReferentialService";
import ReferentialService from "@/services/ReferentialService";
import TripsService from "@/services/TripsService";
import PicturesService from "@/services/PicturesService";
import ShareService from "@/services/ShareService";
import Constants from "@/services/Constants";
import Helpers from "@/services/Helpers";

import { jsPDF } from "jspdf";

import { Component, Vue } from "vue-property-decorator";

interface NormalizedImage {
  dataUrl: string;
  width: number;
  height: number;
}

const PAGE_WIDTH_MM = 210;
const PAGE_HEIGHT_MM = 297;
const MARGIN_MM = 15;
const CONTENT_WIDTH_MM = PAGE_WIDTH_MM - 2 * MARGIN_MM;

@Component({
  components: {
    TripPositionsMap,
    FisholaFooter,
  },
})
export default class TripPdfExportCard extends Vue {
  display = false;
  generating = false;
  tripIds: string[] = [];
  currentTrip: TripBean | null = null;
  currentIndex = 0;
  duration = "";

  private cursorY = MARGIN_MM;

  mounted() {
    this.$root.$on("open-trip-pdf-export", this.open);
  }

  beforeDestroy() {
    this.$root.$off("open-trip-pdf-export", this.open);
  }

  /** @param tripIds Identifiants des sorties à exporter (une seule = export simple, plusieurs = fusionnées en un PDF, #173). */
  open(tripIds: string[]) {
    this.tripIds = tripIds;
    this.currentTrip = null;
    this.display = true;
  }

  close() {
    this.display = false;
    this.currentTrip = null;
  }

  get catchPoints(): TripPositionsCatchPoint[] {
    if (!this.currentTrip || !this.currentTrip.catchs) {
      return [];
    }
    return this.currentTrip.catchs
      .filter((c) => c.latitude != null && c.longitude != null)
      .map((c) => ({ lat: c.latitude!, lng: c.longitude! }));
  }

  get generatingLabel(): string {
    if (this.tripIds.length <= 1) {
      return "Génération du PDF en cours…";
    }
    return `Génération du PDF en cours… (sortie ${this.currentIndex + 1} / ${this.tripIds.length})`;
  }

  async generateClicked() {
    if (!this.tripIds.length || this.generating) {
      return;
    }
    this.generating = true;
    try {
      await this.generatePdf();
    } catch (error) {
      console.error("Erreur lors de la génération du PDF", error);
      this.$root.$emit("toaster-error", "Impossible de générer le PDF");
    } finally {
      this.generating = false;
      this.currentTrip = null;
    }
  }

  private async generatePdf() {
    const refData = await ReferentialService.getSpeciesAndTechniques();
    const doc = new jsPDF({ unit: "mm", format: "a4" });
    let pageStarted = false;

    for (let i = 0; i < this.tripIds.length; i++) {
      this.currentIndex = i;
      const trip = await this.loadTrip(this.tripIds[i]);
      if (!trip) {
        console.error(`Sortie ${this.tripIds[i]} introuvable, ignorée dans l'export PDF`);
        continue;
      }
      if (pageStarted) {
        doc.addPage();
      }
      pageStarted = true;
      this.cursorY = MARGIN_MM;
      await this.writeTripSection(doc, trip, i, refData);
    }

    if (!pageStarted) {
      throw new Error("Aucune des sorties sélectionnées n'a pu être chargée");
    }

    await ShareService.shareGeneratedPdf(doc.output("datauristring"), this.buildFileName());
  }

  private loadTrip(id: string): Promise<TripBean | null> {
    return new Promise((resolve) => {
      TripsService.getTrip(id, (trip: TripBean) => resolve(trip || null));
    });
  }

  private buildFileName(): string {
    if (this.tripIds.length === 1 && this.currentTrip) {
      return `sortie_${Helpers.formatToDate(new Date(this.currentTrip.date))}.pdf`;
    }
    return `sorties_${Helpers.formatToDate(new Date())}.pdf`;
  }

  private async writeTripSection(
    doc: jsPDF,
    trip: TripBean,
    index: number,
    refData: SpeciesWithAliasAndTechnique
  ) {
    this.currentTrip = trip;
    this.duration = Helpers.renderDuration(trip.startedAt, trip.finishedAt);
    // Laisse le temps à TripPositionsMap (remonté via sa `key`) de s'initialiser :
    // son propre mounted() planifie initMap() dans un $nextTick imbriqué.
    await this.$nextTick();
    await this.$nextTick();

    const [lakeName, mapImage, catchesWithPhotos] = await Promise.all([
      this.resolveLakeName(trip),
      this.captureMapImage(),
      this.resolveCatchPhotos(trip.catchs || []),
    ]);

    if (this.tripIds.length > 1) {
      this.writePageIndex(doc, index);
    }
    this.writeHeader(doc, trip, lakeName, refData);
    if (mapImage) {
      this.writeMapImage(doc, mapImage);
    }
    this.writeCatches(doc, trip, refData, catchesWithPhotos);
  }

  private async resolveLakeName(trip: TripBean): Promise<string> {
    const lakeId = (trip as any).lakeId || (trip as any).waterEntityId;
    if (!lakeId) {
      return "";
    }
    try {
      const index = await ReferentialService.getLakesIndex();
      const lake = index.get(lakeId);
      return lake ? lake.name : "";
    } catch (e) {
      // Hors ligne / erreur réseau : le nom du plan d'eau reste optionnel dans le PDF
      return "";
    }
  }

  // Attend que la carte soit initialisée (#173 : elle n'existe pas si la
  // sortie n'a aucune position ni capture géolocalisée) puis capture le
  // canvas WebGL une fois le rendu stabilisé.
  private async captureMapImage(): Promise<NormalizedImage | null> {
    await this.$nextTick();
    const mapComponent = this.$refs.map as TripPositionsMap | undefined;
    if (!mapComponent) {
      return null;
    }
    const dataUrl = await mapComponent.captureImage();
    if (!dataUrl) {
      return null;
    }
    return this.normalizeImage(dataUrl, 1200);
  }

  private async resolveCatchPhotos(
    catchs: CatchBean[]
  ): Promise<Map<string, NormalizedImage>> {
    const result = new Map<string, NormalizedImage>();
    for (const aCatch of catchs) {
      const photo = await this.resolveCatchPhoto(aCatch);
      if (photo) {
        result.set(aCatch.id, photo);
      }
    }
    return result;
  }

  private async resolveCatchPhoto(aCatch: CatchBean): Promise<NormalizedImage | null> {
    try {
      const localPics = await PicturesService.getPicturesFromLocalDB(aCatch.id);
      if (localPics.length) {
        return await this.normalizeImage(localPics[0].content, 500);
      }
      if (aCatch.hasPicture) {
        const base64 = await ShareService.getBase64FromUrl(
          Constants.apiUrl(`/v1/pictures/${aCatch.id}/preview`)
        );
        return await this.normalizeImage(`data:image/jpeg;base64,${base64}`, 500);
      }
    } catch (e) {
      console.error(`Impossible de charger la photo de la capture ${aCatch.id}`, e);
    }
    return null;
  }

  // Convertit toute image source (PNG/JPEG, capture carte ou photo capture)
  // en JPEG fond blanc : évite les soucis de format non supporté par
  // `jsPDF.addImage` et donne les dimensions pour un placement proportionné.
  private normalizeImage(dataUrl: string, maxDimensionPx: number): Promise<NormalizedImage> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => {
        const scale = Math.min(1, maxDimensionPx / Math.max(img.width, img.height));
        const canvas = document.createElement("canvas");
        canvas.width = Math.round(img.width * scale);
        canvas.height = Math.round(img.height * scale);
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          reject(new Error("Canvas 2D non disponible"));
          return;
        }
        ctx.fillStyle = "#ffffff";
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        resolve({
          dataUrl: canvas.toDataURL("image/jpeg", 0.85),
          width: canvas.width,
          height: canvas.height,
        });
      };
      img.onerror = () => reject(new Error("Image invalide"));
      img.src = dataUrl;
    });
  }

  private ensureSpace(doc: jsPDF, neededHeightMm: number) {
    if (this.cursorY + neededHeightMm > PAGE_HEIGHT_MM - MARGIN_MM) {
      doc.addPage();
      this.cursorY = MARGIN_MM;
    }
  }

  private writePageIndex(doc: jsPDF, index: number) {
    doc.setFontSize(9);
    doc.setTextColor(150);
    doc.text(`Sortie ${index + 1} / ${this.tripIds.length}`, PAGE_WIDTH_MM - MARGIN_MM, MARGIN_MM, {
      align: "right",
    });
    doc.setTextColor(0);
  }

  private writeHeader(
    doc: jsPDF,
    trip: TripBean,
    lakeName: string,
    refData: SpeciesWithAliasAndTechnique
  ) {
    doc.setFontSize(18);
    doc.text(trip.name || "Sortie de pêche", MARGIN_MM, this.cursorY);
    this.cursorY += 9;

    doc.setFontSize(11);
    doc.text(`Date : ${Helpers.formatToLongDate(new Date(trip.date))}`, MARGIN_MM, this.cursorY);
    this.cursorY += 6;
    doc.text(`Durée : ${this.duration}`, MARGIN_MM, this.cursorY);
    this.cursorY += 6;
    if (lakeName) {
      doc.text(`Plan d'eau : ${lakeName}`, MARGIN_MM, this.cursorY);
      this.cursorY += 6;
    }
    const techniqueNames = this.techniqueNames(trip, refData);
    if (techniqueNames.length) {
      doc.text(`Technique(s) : ${techniqueNames.join(", ")}`, MARGIN_MM, this.cursorY);
      this.cursorY += 6;
    }
    this.cursorY += 4;
  }

  private techniqueNames(trip: TripBean, refData: SpeciesWithAliasAndTechnique): string[] {
    if (!trip.techniqueIds || !trip.techniqueIds.length) {
      return [];
    }
    return refData.techniques
      .filter((t) => trip.techniqueIds.includes(t.id))
      .map((t) => t.name);
  }

  private writeMapImage(doc: jsPDF, image: NormalizedImage) {
    const width = CONTENT_WIDTH_MM;
    const height = Math.min(width * (image.height / image.width), 100);
    this.ensureSpace(doc, height + 8);
    doc.addImage(image.dataUrl, "JPEG", MARGIN_MM, this.cursorY, width, height);
    this.cursorY += height + 8;
  }

  private writeCatches(
    doc: jsPDF,
    trip: TripBean,
    refData: SpeciesWithAliasAndTechnique,
    photos: Map<string, NormalizedImage>
  ) {
    const catchs = trip.catchs || [];
    this.ensureSpace(doc, 10);
    doc.setFontSize(14);
    doc.text(`Captures (${catchs.length})`, MARGIN_MM, this.cursorY);
    this.cursorY += 8;
    doc.setFontSize(10);

    if (!catchs.length) {
      doc.text("Aucune capture enregistrée pour cette sortie.", MARGIN_MM, this.cursorY);
      this.cursorY += 6;
      return;
    }

    catchs.forEach((aCatch) => this.writeCatchRow(doc, aCatch, refData, photos.get(aCatch.id)));
  }

  private writeCatchRow(
    doc: jsPDF,
    aCatch: CatchBean,
    refData: SpeciesWithAliasAndTechnique,
    photo?: NormalizedImage
  ) {
    const thumbSize = photo ? 25 : 0;
    const rowHeight = Math.max(thumbSize, 16) + 4;
    this.ensureSpace(doc, rowHeight);

    if (photo) {
      const height = Math.min(thumbSize * (photo.height / photo.width), thumbSize);
      doc.addImage(photo.dataUrl, "JPEG", MARGIN_MM, this.cursorY, thumbSize, height);
    }

    const textX = MARGIN_MM + (photo ? thumbSize + 4 : 0);
    let textY = this.cursorY + 4;

    const speciesLabel = this.speciesLabel(aCatch, refData);
    const quantitySuffix = aCatch.quantity && aCatch.quantity > 1 ? ` × ${aCatch.quantity}` : "";
    doc.text(`${speciesLabel}${quantitySuffix}`, textX, textY);
    textY += 5;

    const details: string[] = [];
    if (aCatch.size) {
      details.push(`${aCatch.size} cm`);
    }
    if (aCatch.weight) {
      details.push(`${aCatch.weight} g`);
    }
    if (details.length) {
      doc.text(details.join(" - "), textX, textY);
      textY += 5;
    }

    const techniqueLabel = this.techniqueLabel(aCatch, refData);
    if (techniqueLabel) {
      doc.text(techniqueLabel, textX, textY);
    }

    this.cursorY += rowHeight;
  }

  private speciesLabel(aCatch: CatchBean, refData: SpeciesWithAliasAndTechnique): string {
    const species = refData.species.find((s) => s.id === aCatch.speciesId);
    if (species) {
      return species.alias ? species.alias : species.name;
    }
    return aCatch.otherSpecies || "Espèce non renseignée";
  }

  private techniqueLabel(aCatch: CatchBean, refData: SpeciesWithAliasAndTechnique): string {
    const technique = refData.techniques.find((t) => t.id === aCatch.techniqueId);
    return technique ? technique.name : "";
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.trip-pdf-export-hidden {
  display: none;
}

.trip-pdf-export {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 998;
  background: @white;
}

.trip-pdf-export-summary {
  text-align: center;
  margin-bottom: @vertical-margin-small;

  .trip-pdf-export-name {
    font-weight: bold;
    color: @gunmetal;
  }
}

.trip-pdf-export-generating {
  text-align: center;
  color: @pelorous;
  margin-top: @vertical-margin-small;

  i {
    margin-right: @margin-x-small;
  }
}
</style>
