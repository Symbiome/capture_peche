<!--
  #%L
  Fishola :: Mobile
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
  <div class="edit-catch page-with-header-and-footer picture-background">
    <FisholaHeader />
    <div class="catch-picture keyboardSensitive hide-on-desktop">
      <!-- Show all gallery pics -->
      <PicturePreview
        v-for="pictureSrc in allNonMeasurePictures"
        :key="pictureSrc.order"
        v-bind:src="pictureSrc.content"
        v-bind:deletable="modifiable"
        v-on:take-picture="takePicture"
        v-on:delete-picture="deletePicture(pictureSrc.content)"
      />
      <!-- Then measurement pic (if any) -->
      <PicturePreview
        v-if="measurementPictureSrc"
        v-bind:src="measurementPictureSrc"
        v-bind:deletable="false"
        v-on:take-picture="takePicture"
      />
      <!-- Empty picture if no picture yet -->
      <PicturePreview
        v-else-if="!allNonMeasurePictures.length && !measurementPictureSrc"
        noPictureText="Appuyer pour ajouter une photo"
        v-bind:deletable="false"
        v-on:take-picture="takePicture"
      />
    </div>
    <div class="edit-catch-page page">
      <div class="pane keyboardSensitive">
        <div class="pane-content rounded" v-if="ready">
          <h1>
            <BackButton class="hide-on-mobile" />
            Capture
          </h1>

          <span v-if="catchPositionError" class="position-error">
            <i class="icon-warning" />
            {{ catchPositionError }}
          </span>

          <div class="catch-picture-desktop-and-form">
            <div class="catch-picture-desktop hide-on-mobile">
              <!-- Show focused pic -->
              <PicturePreview
                class="pic-focused"
                v-bind:src="focusedPicSrc"
                noPictureText="Appuyer pour ajouter une photo"
                v-bind:deletable="focusedPicSrc != measurementPictureSrc"
                v-on:take-picture="takePicture"
                v-on:delete-picture="deletePicture(focusedPicSrc)"
              />
              <div class="pic-miniatures-container">
                <!-- Show all gallery pics -->
                <PicturePreview
                  style="padding:10px;"
                  :enableModal="false"
                  v-for="pictureSrc in allNonMeasurePictures"
                  :key="pictureSrc.order"
                  v-bind:src="pictureSrc.content"
                  v-bind:deletable="false"
                  @picture-clicked="focusedPicSrc = pictureSrc.content"
                  v-on:take-picture="takePicture"
                  :class="{
                    'pic-miniature': true,
                    'pic-selected': pictureSrc.content == focusedPicSrc,
                  }"
                />
                <!-- Then measurement pic (if any) -->
                <PicturePreview
                  :enableModal="false"
                  v-if="measurementPictureSrc"
                  :key="measurementPictureSrc"
                  v-bind:src="measurementPictureSrc"
                  v-bind:deletable="false"
                  v-on:take-picture="takePicture"
                  @picture-clicked="focusedPicSrc = measurementPictureSrc"
                  :class="{
                    'pic-miniature': true,
                    'pic-selected': measurementPictureSrc == focusedPicSrc,
                  }"
                />
                <!-- Empty miniature picture for adding pictures -->
                <PicturePreview
                  :enableModal="false"
                  class="pic-miniature"
                  v-if="focusedPicSrc"
                  noPictureText="Appuyer pour ajouter une photo"
                  v-bind:deletable="modifiable"
                  v-on:take-picture="takePicture"
                />
              </div>
            </div>
            <div class="edit-catch-form">
              <div
                :class="{
                  'two-columns-row-on-desktop': aCatch.speciesId == '__other__',
                }"
              >
                <FormSelect
                  name="species"
                  label="Espèce"
                  v-bind:options="allSpeciesWithAliases"
                  v-model="aCatch.speciesId"
                  v-bind:error="speciesIdError"
                  v-bind:readonly="!modifiable"
                />
                <FormInput
                  name="otherSpecies"
                  label="Si autre"
                  type="text"
                  placeholder="Renseigner l’espèce"
                  v-model="aCatch.otherSpecies"
                  v-bind:error="otherSpeciesError"
                  v-bind:readonly="!modifiable"
                  v-if="aCatch.speciesId == '__other__'"
                />
              </div>
              <div class="measure-row">
                <div
                  class="button button-secondary-no-outline automatic-measure"
                  v-if="modifiable"
                >
                  <button
                    @click="
                      displayMeasurementPicturePopup = !displayMeasurementPicturePopup
                    "
                  >
                    <i class="icon-size measure-button-icon" />
                    <span id="measure-button-text"></span>
                  </button>
                </div>
                <FormInput
                  name="size"
                  :label="sizeLabel"
                  type="number"
                  :min="1"
                  placeholder="Entrez une taille en centimètres"
                  v-model="aCatch.size"
                  v-bind:error="sizeError"
                  v-bind:readonly="!modifiable"
                />
              </div>

              <div class="two-columns-row-on-desktop">
                <div class="multiple-catchs-info" v-if="multipleCatchsAllowed">
                  <i class="icon-info" />
                  <span>
                    Indiquez le poids total de vos captures si vous en avez
                    plusieurs pour cette espèce
                  </span>
                </div>
                <FormInput
                  v-if="aCatch.weight || (settings && settings.promptWeight)"
                  name="weight"
                  label="Poids en g (optionnel)"
                  type="number"
                  :min="1"
                  placeholder="Entrez un poids en grammes"
                  v-model="aCatch.weight"
                  v-bind:error="weightError"
                  v-bind:readonly="!modifiable"
                />
                <FormYesNo
                  name="keep"
                  v-bind:label="
                    tripMode == 'Live'
                      ? 'Conservez-vous ce poisson ?'
                      : 'Avez-vous conservé ce poisson ?'
                  "
                  v-model="aCatch.keep"
                  v-bind:error="keepError"
                  v-bind:readonly="!modifiable"
                />
              </div>
              <div class="two-columns-row-on-desktop">
                <!-- AThimel 27/02/2020 On désactive la saisie de l'état du poisson relâché. Cf cocoo n°9 -->
                <!--FormSelect name="releaseState"
                            label="État du poisson relâché"
                            v-bind:options="allReleasedFishStates"
                            v-model="aCatch.releasedStateId"
                            v-bind:error="releasedStateIdError"
                            v-bind:readonly="!modifiable"/-->
                <FormSelect
                  name="technique"
                  label="Technique de pêche"
                  v-bind:options="allTechniques"
                  v-model="aCatch.techniqueId"
                  v-bind:error="techniqueIdError"
                  v-bind:readonly="!modifiable"
                />
                <FormInput
                  name="caughtAt"
                  label="Heure de la capture (optionnelle)"
                  type="time"
                  v-model="caughtAt"
                  v-bind:readonly="!modifiable"
                />
              </div>
              <div>
                <FormTextarea
                  name="description"
                  label="Observation (optionnelle)"
                  placeholder="Écrivez une description, une observation, une remarque à propos de votre capture"
                  v-model="aCatch.description"
                  v-bind:readonly="!modifiable"
                />
              </div>
              <FormToggle
                v-if="
                  withSample ||
                    (settings &&
                      settings.promptSamples &&
                      authorizedSampleSpeciesIds.indexOf(aCatch.speciesId) !=
                        -1)
                "
                label="Prélèvement (optionnel)"
                v-model="withSample"
                v-bind:readonly="!modifiable"
              />
              <div class="sample two-columns-row-on-desktop" v-if="withSample">
                <div class="info" v-if="samplesDocumentationUrl">
                  Pour pouvoir effectuer des prélèvements, vous devez vous munir
                  d'un kit dans un des points de collecte :
                  <a :href="samplesDocumentationUrl" target="_blank"
                    >consulter la liste</a
                  >
                </div>

                <div class="sample-id-container">
                  <div class="description">
                    Numéro d'échantillon à reporter :
                  </div>
                  <div v-if="!sampleIdReady" class="spinner">&nbsp;</div>
                  <div class="sample-id" v-if="sampleIdReady">
                    {{ aCatch.sampleId }}
                  </div>
                </div>
              </div>

              <div v-if="!inCreation" class="location">
                <!-- <div class="separator"></div>
                <p>Emplacement de la capture</p> -->
                <div class="empty" v-if="!gpsLocation">
                  <i class="icon icon-warning"></i> Aucune position enregistrée
                  pour cette prise
                </div>
                <div class="map" v-if="gpsLocation">
                  <l-map
                    :zoom="16"
                    :center="gpsLocation"
                    :options="{
                      zoomSnap: 0.5,
                    }"
                    style="height: 100%; width: 100%;"
                  >
                    <l-tile-layer
                      url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                      attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
                    />

                    <l-marker :lat-lng="gpsLocation"></l-marker>
                  </l-map>
                </div>
              </div>

              <div class="buttons-bar hide-on-mobile">
                <div class="button button-primary" v-if="modifiable">
                  <button v-on:click="validateClicked">
                    <i class="icon-fish" />
                    {{ inCreation ? "Valider" : "Enregistrer" }}
                  </button>
                </div>
                <div class="button button-secondary" v-if="inCreation">
                  <button v-on:click="cancel">
                    Annuler
                  </button>
                </div>
                <div
                  class="button button-danger"
                  v-if="!inCreation && modifiable"
                >
                  <button v-on:click="deleteCatch">
                    <i class="icon-delete" /> Supprimer
                  </button>
                </div>
              </div>
            </div>
            <!-- edit-catch-form -->
          </div>
          <!-- catch-picture-desktop-and-form -->

          <div class="bottom-page-spacer keyboardSensitive"></div>
        </div>
      </div>
    </div>
    <MeasurementPicturePopup
      v-if="displayMeasurementPicturePopup"
      @close="displayMeasurementPicturePopup = false"
      @measurementPictureTaken="measurementPictureTaken"
      @measured="gotAutomaticMeasure"
    />
    <PictureSourceChoice
      v-if="requestNewPicture"
      @close="requestNewPicture = false"
      @pictureTaken="pictureTaken"
      :directlyOpenGaleryInWebMode="true"
    />
    <FisholaFooter
      v-if="ready && modifiable"
      v-bind:button-text="inCreation ? 'Valider' : 'Enregistrer'"
      button-icon="icon-fish"
      v-on:buttonClicked="validateClicked"
      v-on:deleteClicked="deleteCatch"
      v-bind:shortcuts="'back,' + middleShortcut + ',' + rightShortcut"
    />
    <FisholaFooter v-if="ready && !modifiable" shortcuts="back,spacer,blank" />
    <!-- Invisible marker & pic (required for silent size computation) -->
    <img id="markerAutomatic" v-show="false" :src="markerSourceSRC" />
    <img
      id="sourcePictureAutomatic"
      :src="measurementPictureCandidateSrc"
      v-show="false"
      @load="launchSilentAutomaticMeasureIfRequired"
    />
  </div>
</template>

<script lang="ts">
import {
  TripBean,
  CatchBean,
  Technique,
  SpeciesWithAlias,
  TripMode,
} from "@/pojos/BackendPojos";
import CatchSummary from "@/pojos/CatchSummary";

import PicturesService from "@/services/PicturesService";
import TripsService from "@/services/TripsService";
import { SpeciesWithAliasAndTechnique } from "@/services/ReferentialService";
import ReferentialService from "@/services/ReferentialService";
import Helpers from "@/services/Helpers";
import GeolocationService from "@/services/GeolocationService";

import { UserSettings } from "@/pojos/BackendPojos";
import PictureContentWithOrder from "@/pojos/PictureContentWithOrder";
import ProfileService from "@/services/ProfileService";

import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import MeasurementPicturePopup from "@/components/trip/MeasurementPicturePopup.vue";
import PictureSourceChoice from "@/components/trip/PictureSourceChoice.vue";
import BackButton from "@/components/common/BackButton.vue";
import FormSelect from "@/components/common/FormSelect.vue";
import FormToggle from "@/components/common/FormToggle.vue";
import FormInput from "@/components/common/FormInput.vue";
import FormYesNo from "@/components/common/FormYesNo.vue";
import FormTextarea from "@/components/common/FormTextarea.vue";
import PicturePreview from "@/components/trip/PicturePreview.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import router from "../../router";
import Constants from "../../services/Constants";
import DocumentationService from "@/services/DocumentationService";

import { latLng, LatLng, Icon } from "leaflet";

type D = Icon.Default & {
  _getIconUrl?: string;
};

delete (Icon.Default.prototype as D)._getIconUrl;

Icon.Default.mergeOptions({
  iconRetinaUrl: require("leaflet/dist/images/marker-icon-2x.png"),
  iconUrl: require("leaflet/dist/images/marker-icon.png"),
  shadowUrl: require("leaflet/dist/images/marker-shadow.png"),
});

import { LMap, LTileLayer, LMarker } from "vue2-leaflet";
import FisholaOpenCVService from "@/services/opencv/FisholaOpenCVService";
import { OpenCVDetectionConfig } from "@/services/opencv/OpenCVDetectionConfig";
import { DetectedShape } from "@/services/opencv/DetectedShape";

@Component({
  components: {
    FisholaHeader,
    BackButton,
    FormInput,
    FormYesNo,
    FormTextarea,
    FormSelect,
    FormToggle,
    PicturePreview,
    LMap,
    LTileLayer,
    LMarker,
    MeasurementPicturePopup,
    PictureSourceChoice,
    FisholaFooter,
  },
})
export default class EditCatchView extends Vue {
  @Prop() tripId!: string;
  @Prop() catchId!: string;

  settings: UserSettings | null = null;

  // On est obligés de gérer un flag de ce genre, sinon les FormSelect
  // sont créés à vide et ne sélectionnent pas les bonnes valeurs
  ready: boolean = false;

  tripDate?: Date;
  tripMode: TripMode = "Live";
  tripSpeciesIds: string[] = [];
  tripOtherSpecies: string = "";
  aCatch: CatchSummary = { id: "" };

  allNonMeasurePictures: PictureContentWithOrder[] = [];
  measurementPictureSrc: string = "";
  measurementPictureCandidateSrc: string = "";
  focusedPicSrc: string = "";
  // Pictures that have just been taken and hence should be saved in local DB when validating
  newTakenPictures: PictureContentWithOrder[] = [];
  // Picture that should be deleted at next save
  picturesToDelete: PictureContentWithOrder[] = [];

  caughtAt: string = "";
  markerSourceSRC = "";

  defaultSizeLabel: string = "Taille en cm";
  sizeLabel: string = this.defaultSizeLabel;
  multipleCatchsAllowed: boolean = false;

  catchPositionError: string = "";
  speciesIdError: string = "";
  otherSpeciesError: string = "";
  sizeError: string = "";
  weightError: string = "";
  keepError: string = "";
  releasedStateIdError: string = "";
  techniqueIdError: string = "";

  modifiable: boolean = true;
  inCreation: boolean = true;
  inTripCreation: boolean = true;
  middleShortcut: string = "";
  rightShortcut: string = "delete";

  allSpeciesWithAliases: SpeciesWithAlias[] = [];
  allTechniques: Technique[] = [];
  // allReleasedFishStates:ReleasedFishState[] = [];

  withSample: boolean = false;
  samplesDocumentationUrl: string = "";
  sampleIdReady: boolean = false;
  authorizedSampleSpeciesIds: string[] = [];

  gpsLocation: LatLng | null = null;

  displayMeasurementPicturePopup = false;
  requestNewPicture = false;
  shouldLaunchAutomaticMeasure = false;

  created() {
    TripsService.getTripAndCatch(
      this.tripId,
      this.catchId,
      this.tripAndCatchLoaded
    );
    this.inCreation = this.catchId == Constants.NEW_CATCH_ID;
    this.loadSettings();
    this.markerSourceSRC = new OpenCVDetectionConfig().defaultMarkerSrc;
    FisholaOpenCVService.INSTANCE.loadOpenCVIfNeeded();
  }

  mounted() {
    this.samplesDocumentationUrl = DocumentationService.getSamplesDocumentationUrl();
  }

  loadSettings() {
    ProfileService.getSettings().then(this.settingsLoaded);
  }

  settingsLoaded(settings: UserSettings) {
    this.settings = settings;
  }

  async tripAndCatchLoaded(someTrip: TripBean, someCatch: CatchSummary) {
    const lakeId: string = someTrip.lakeId;
    this.tripDate = someTrip.date;
    this.tripSpeciesIds = someTrip.speciesIds;
    this.tripOtherSpecies = someTrip.otherSpecies;
    if (!someCatch.speciesId && someCatch.otherSpecies) {
      someCatch.speciesId = "__other__";
    }
    this.inTripCreation = !someTrip.createdOn;
    this.middleShortcut = this.inTripCreation ? "step-3-4" : "spacer";
    this.modifiable = this.inTripCreation || !!someTrip.modifiableUntil;
    this.tripMode = someTrip.mode;
    this.aCatch = someCatch;

    if (this.aCatch.automaticMeasure && !this.aCatch.size) {
      this.aCatch.size = this.aCatch.automaticMeasure;
    }

    if (someCatch.caughtAt) {
      this.caughtAt = Helpers.truncateTimeToMinutes(someCatch.caughtAt);

      if (this.inCreation && this.inTripCreation && this.tripMode == "Live") {
        const seconds: number = Helpers.computeDurationInSeconds(
          someTrip.startedAt,
          someCatch.caughtAt!
        );
        this.rightShortcut = "timer-" + seconds;
      }
    }

    if (someCatch.sampleId) {
      this.sampleIdReady = true;
      this.withSample = true;
    }

    ReferentialService.getSpeciesAndTechniques(lakeId).then(
      this.referentialLoaded
    );

    if (this.inCreation && this.inTripCreation && this.tripMode == "Live") {
      GeolocationService.checkWatchAndGetPositionUntilTimeout().then(
        (position) => {
          this.aCatch.latitude = position.coords.latitude;
          this.aCatch.longitude = position.coords.longitude;
          console.info(
            `Coordonnées de capture : ${this.aCatch.latitude},${this.aCatch.longitude}`
          );
        },
        (e) => {
          console.error(
            "Error while geting location from geolocation service",
            JSON.stringify(e)
          );
          if (JSON.stringify(e).indexOf("location unavailable") != -1) {
            this.catchPositionError =
              "La position n'est pas activée, la capture ne sera pas geolocalisée";
            if (!GeolocationService.notifiedPositionDisabled) {
              GeolocationService.notifiedPositionDisabled = true;
              Helpers.alert(
                this.$modal,
                "Vous devez activer la localisation de l'appareil pour que FISHOLA puisse acquérir votre position",
                "La position n'est pas activée"
              );
            }
          } else if (JSON.stringify(e).indexOf("User denied") != -1) {
            this.catchPositionError =
              "Partage de position refusé, la capture ne sera pas geolocalisée";
          }
        }
      );
    }

    if (!this.inCreation && this.aCatch.latitude && this.aCatch.longitude) {
      this.gpsLocation = latLng(this.aCatch.latitude, this.aCatch.longitude);
    }

    // Get pictures stored locally (not yet synchronized)
    const localPics = await PicturesService.getPicturesFromLocalDB(
      someCatch.id
    );
    if (localPics.length) {
      this.focusedPicSrc = localPics[0].content;
    }
    this.allNonMeasurePictures = localPics.filter(
      (pic) => !pic.isMeasurementPicture
    );
    const potentialMeasurePic = localPics.filter(
      (pic) => pic.isMeasurementPicture
    );
    if (potentialMeasurePic.length) {
      this.measurementPictureSrc = potentialMeasurePic[0].content;
    }

    // Get server gallery pics (reconstructed through the "orders" field)
    this.aCatch.pictureOrders?.forEach((order) => {
      const pictureFromServer: PictureContentWithOrder = {
        order: order,
        isMeasurementPicture: false,
        content: Constants.apiUrl(
          `/v1/pictures/${this.aCatch.id}/preview/${order}`
        ),
      };
      this.allNonMeasurePictures.push(pictureFromServer);

      if (!this.focusedPicSrc) {
        this.focusedPicSrc = pictureFromServer.content;
      }
    });

    // Finally get measurement pic (if any)
    if (this.aCatch.hasMeasurementPicture) {
      this.measurementPictureSrc = Constants.apiUrl(
        `/v1/pictures/measure/${this.aCatch.id}/preview`
      );

      if (!this.focusedPicSrc) {
        this.focusedPicSrc = this.measurementPictureSrc;
      }
    }
  }

  async launchSilentAutomaticMeasureIfRequired() {
    if (this.shouldLaunchAutomaticMeasure && !this.aCatch.automaticMeasure) {
      this.shouldLaunchAutomaticMeasure = false;
      // Launch a silent measure
      console.info("[Silent automatic measure] Loading opencv...");
      FisholaOpenCVService.INSTANCE.loadOpenCVIfNeeded().then(async () => {
        if (FisholaOpenCVService.INSTANCE.isOpenCVReady()) {
          try {
            const imageElement = document.getElementById(
              "sourcePictureAutomatic"
            );
            const markerElement = document.getElementById("markerAutomatic");
            console.info(
              "[Silent automatic measure] Launching automatic measure...",
              imageElement,
              markerElement
            );
            if (imageElement && markerElement) {
              const openCVConfig = new OpenCVDetectionConfig();
              openCVConfig.drawDebugCanvas = false;
              openCVConfig.maxRetries = -1;
              const detectedShapes: Array<DetectedShape> = await FisholaOpenCVService.INSTANCE.calculateAndDrawFishSizes(
                imageElement,
                markerElement,
                openCVConfig,
                ""
              );

              const markers = detectedShapes.filter(
                (shape: DetectedShape) => shape.isMarker
              ).length;
              const markerFound = markers === 1;
              const fishes = detectedShapes.filter(
                (shape: DetectedShape) => shape.isFish
              );
              let fishSize = 0;
              if (fishes.length === 1) {
                fishSize = fishes[0].calculatedLenght;
              }
              console.info(
                "[Silent automatic measure] Success : " +
                  markers +
                  " markers and fish of " +
                  fishSize +
                  "mm"
              );
              if (markerFound && fishSize) {
                this.gotAutomaticMeasure(fishSize);
              }
            }
            // Step 2: launch calculation
          } catch (error) {
            console.error("[Silent automatic measure] Failure ", error);
          }
        }
      });
    }
  }

  embedAlias(s: SpeciesWithAlias): SpeciesWithAlias {
    const result: SpeciesWithAlias = {
      id: s.id,
      name: s.alias ? `${s.alias} (${s.name})` : s.name,
      builtIn: s.builtIn,
      mandatorySize: s.mandatorySize,
      authorizedSample: s.authorizedSample,
    };
    return result;
  }

  referentialLoaded(data: SpeciesWithAliasAndTechnique) {
    data.species.forEach((s) => {
      if (
        s.builtIn || // Espèce de base
        this.aCatch.speciesId == s.id || // Espèce custom sélectionnée pour la capture
        this.tripSpeciesIds.indexOf(s.id) != -1 // Espèce custom sélectionnée pour la sortie
      ) {
        const speciesWithAlias: SpeciesWithAlias = this.embedAlias(s);
        this.allSpeciesWithAliases.push(speciesWithAlias);
        if (s.authorizedSample) {
          this.authorizedSampleSpeciesIds.push(s.id);
        }
      }
    });
    // Espèce custom sélectionnée pour la sortie mais pas encore synchro
    if (this.tripOtherSpecies) {
      this.tripOtherSpecies.split(",").forEach((name) => {
        const customSpecies: SpeciesWithAlias = {
          id: name,
          name: name,
          builtIn: false,
          mandatorySize: false,
          authorizedSample: false,
        };
        this.allSpeciesWithAliases.push(customSpecies);
      });
    }
    this.allSpeciesWithAliases = Vue.lodash.orderBy(
      this.allSpeciesWithAliases,
      "name"
    );
    this.allSpeciesWithAliases.push({
      id: "__other__",
      name: "Autre ...",
      builtIn: false,
      mandatorySize: false,
      authorizedSample: false,
    });
    data.techniques.forEach((t) => this.allTechniques.push(t));
    // data.states.forEach((s) => this.allReleasedFishStates.push(s));
    this.ready = true;

    // On déclenche l'ouverture de l'APN dès le début de la capture
    if (this.inCreation) {
      this.takePicture();
    }
  }

  isMandatorySize(speciesId?: string): boolean {
    let result = true;
    if (speciesId) {
      this.allSpeciesWithAliases.forEach((s: SpeciesWithAlias) => {
        if (s.id == speciesId && s.mandatorySize === false) {
          result = false;
        }
      });
    }
    return result;
  }

  @Watch("aCatch.speciesId")
  speciesChanged(newValue: string, oldValue: string) {
    this.sizeLabel = this.defaultSizeLabel;
    this.multipleCatchsAllowed = false;
    if (newValue && this.isMandatorySize(newValue) === false) {
      this.sizeLabel = this.defaultSizeLabel + " (optionnelle)";
      this.multipleCatchsAllowed = true;
    }
  }

  async takePicture() {
    if (this.modifiable) {
      this.shouldLaunchAutomaticMeasure = true;
      this.requestNewPicture = true;
    }
  }

  async deletePicture(pictureToDeleteSrc: string) {
    // Add pic to list of pictures to delete on local storage / server
    this.picturesToDelete = this.picturesToDelete.concat(
      this.allNonMeasurePictures.filter(
        (pic) => pic.content == pictureToDeleteSrc
      )
    );

    // Filter currently displayed list
    this.allNonMeasurePictures = this.allNonMeasurePictures.filter(
      (pic) => pic.content != pictureToDeleteSrc
    );

    // Remove pic from pictures that are about to be stored in local storage
    this.newTakenPictures = this.newTakenPictures.filter(
      (pic) => pic.content != pictureToDeleteSrc
    );

    // Update focused pic
    if (this.focusedPicSrc == pictureToDeleteSrc) {
      if (this.allNonMeasurePictures.length) {
        this.focusedPicSrc = this.allNonMeasurePictures[0].content;
      } else if (this.measurementPictureSrc) {
        this.focusedPicSrc = this.measurementPictureSrc;
      } else {
        this.focusedPicSrc = "";
      }
    }
  }

  pictureTaken(pictureContent: string, isMeasurementPicture: boolean) {
    this.requestNewPicture = false;
    var maxOrder = Math.max(
      1,
      Math.max.apply(
        Math,
        this.allNonMeasurePictures.map(function(o) {
          return o.order;
        })
      )
    );
    maxOrder += 1;
    const pictureInDb: PictureContentWithOrder = {
      order: maxOrder,
      content: pictureContent,
      isMeasurementPicture: isMeasurementPicture,
    };
    this.allNonMeasurePictures.unshift(pictureInDb);
    this.newTakenPictures.unshift(pictureInDb);
    this.focusedPicSrc = pictureInDb.content;

    // If no automatic measure has been determined yet, let's try with this new picture
    if (isMeasurementPicture || !this.aCatch.automaticMeasure) {
      this.measurementPictureCandidateSrc = pictureContent;
    }
  }

  measurementPictureTaken(newMeasurementPictureSrc: string) {
    this.shouldLaunchAutomaticMeasure = false;
    this.measurementPictureSrc = newMeasurementPictureSrc;
    this.pictureTaken(newMeasurementPictureSrc, true);
  }

  @Watch("withSample")
  onWithSampleChanged(value: boolean, oldValue: boolean) {
    if (value && !this.aCatch.sampleId) {
      TripsService.newSampleId().then(this.onNewSampleId);
    }
  }

  onNewSampleId(newSampleId: string) {
    this.aCatch.sampleId = newSampleId;
    this.sampleIdReady = true;
  }

  validateClicked() {
    let hasError: boolean = false;

    if (this.aCatch.speciesId == "__other__") {
      this.speciesIdError = "";

      if (this.aCatch.otherSpecies) {
        if (this.aCatch.otherSpecies.indexOf(",") == -1) {
          this.otherSpeciesError = "";
        } else {
          hasError = true;
          this.otherSpeciesError =
            "Vous ne pouvez pas spécifier plusieurs espèces ici";
        }
      } else {
        hasError = true;
        this.otherSpeciesError = "Espèce obligatoire";
      }
    } else {
      this.otherSpeciesError = "";

      if (this.aCatch.speciesId) {
        this.speciesIdError = "";
      } else {
        hasError = true;
        this.speciesIdError = "Espèce obligatoire";
      }
    }

    const mandatorySize = this.isMandatorySize(this.aCatch.speciesId);
    if (mandatorySize && !this.aCatch.size) {
      hasError = true;
      this.sizeError = "Taille obligatoire";
    } else if (this.aCatch.size && this.aCatch.size <= 0) {
      hasError = true;
      this.sizeError = "La taille doit être strictement positive";
    } else {
      if (this.aCatch.size) {
        if (this.aCatch.size != Math.floor(this.aCatch.size)) {
          hasError = true;
          this.sizeError = "La taille doit être un nombre entier";
        } else {
          // On force pour stocker uniquement la valeur tronquée
          this.aCatch.size = Math.floor(this.aCatch.size);
          this.sizeError = "";
        }
      } else {
        this.sizeError = "";
      }
    }

    if (!this.aCatch.weight || this.aCatch.weight > 0) {
      if (this.aCatch.weight) {
        if (this.aCatch.weight != Math.floor(this.aCatch.weight)) {
          hasError = true;
          this.weightError = "Le poids doit être un nombre entier";
        } else {
          // On force pour stocker uniquement la valeur tronquée
          this.aCatch.weight = Math.floor(this.aCatch.weight);
          this.weightError = "";
        }
      } else {
        this.weightError = "";
      }
    } else {
      hasError = true;
      this.weightError = "Le poids doit être strictement positif";
    }

    if (
      this.aCatch.size &&
      !this.sizeError &&
      this.aCatch.weight &&
      !this.weightError
    ) {
      if (this.aCatch.size >= 25) {
        const minValue = 0.01 * Math.pow(this.aCatch.size, 2.7);
        const maxValue = 0.01 * Math.pow(this.aCatch.size, 3.2);
        if (this.aCatch.weight < minValue || this.aCatch.weight > maxValue) {
          console.info(
            `Le poids (${this.aCatch.weight}g) devrait se situer entre ${minValue}g et ${maxValue}g`
          );
          hasError = true;
          this.weightError = "Le poids n'est pas cohérent avec la taille";
        }
      } else if (this.aCatch.weight > 300) {
        console.info(
          `Le poids (${this.aCatch.weight}g) ne devrait pas dépasser 300g`
        );
        hasError = true;
        this.weightError = "Le poids n'est pas cohérent avec la taille";
      }
    }

    if (this.aCatch.keep === true || this.aCatch.keep === false) {
      this.keepError = "";
    } else {
      hasError = true;
      this.keepError = "Information obligatoire";
    }

    // if (this.aCatch.keep === false)  {
    //   if (this.aCatch.releasedStateId) {
    //     this.releasedStateIdError = '';
    //   } else {
    //     hasError = true;
    //     this.releasedStateIdError = 'État du poisson relâché obligatoire';
    //   }
    // } else {
    this.releasedStateIdError = "";
    // }

    if (this.aCatch.techniqueId) {
      this.techniqueIdError = "";
    } else {
      hasError = true;
      this.techniqueIdError = "Technique de pêche obligatoire";
    }

    if (this.caughtAt && this.caughtAt.length > 0) {
      this.aCatch.caughtAt = this.caughtAt;
    } else {
      delete this.aCatch.caughtAt;
    }

    if (hasError) {
      this.$root.$emit(
        "toaster-error",
        "Vous devez renseigner les champs obligatoires"
      );
    } else {
      const aCatchBean: CatchBean = this.castToBean(this.aCatch);
      if (aCatchBean.speciesId == "__other__") {
        aCatchBean.speciesId = "";
      }
      aCatchBean.hasPicture =
        this.allNonMeasurePictures && this.allNonMeasurePictures.length > 0;
      if (!this.withSample) {
        aCatchBean.sampleId = "";
      }
      TripsService.saveCatch(this.tripId, aCatchBean, this.catchSaved);
    }
  }

  castToBean(input: any): CatchBean {
    return input;
  }

  async catchSaved(catchId: string) {
    for (var i = 0; i < this.newTakenPictures.length; i++) {
      const picToSaveInLocalDb = this.newTakenPictures[i];
      await PicturesService.savePictureInLocalDB(
        catchId + picToSaveInLocalDb.order,
        catchId,
        picToSaveInLocalDb.content,
        picToSaveInLocalDb.isMeasurementPicture,
        picToSaveInLocalDb.order
      );
    }
    for (var j = 0; j < this.picturesToDelete.length; j++) {
      const pictureToDelete = this.picturesToDelete[j];
      await PicturesService.deletePicture(
        catchId + pictureToDelete.order,
        catchId,
        pictureToDelete.order
      );
    }
    this.leavePage();
  }

  deleteCatch() {
    Helpers.confirm(this.$modal, "Voulez-vous supprimer la capture ?").then(
      () => {
        TripsService.deleteCatch(this.tripId, this.catchId, this.leavePage);
      }
    );
  }

  cancel() {
    TripsService.deleteCatch(this.tripId, this.catchId, this.leavePage);
  }

  leavePage() {
    if (this.inTripCreation) {
      router.push({ name: "trip-catchs", params: { id: this.tripId } });
    } else {
      router.push({ name: "trip", params: { id: this.tripId } });
    }
  }

  gotAutomaticMeasure(measure: number) {
    this.displayMeasurementPicturePopup = false;
    this.aCatch.automaticMeasure = measure;
    // Override manual size, but user will still be able to modify it later on
    this.aCatch.size = measure;

    // If we had a measure, means that latest taken picture is a measurement pic
    if (this.newTakenPictures.length) {
      // All previously taken pictures should not be considered as measurement
      this.newTakenPictures.forEach(
        (pic) => (pic.isMeasurementPicture = false)
      );
      this.measurementPictureSrc = this.newTakenPictures[0].content;
      this.newTakenPictures[0].isMeasurementPicture = true;
      this.allNonMeasurePictures = this.allNonMeasurePictures.filter(
        (pic) => !pic.isMeasurementPicture
      );
    }
    this.$forceUpdate();
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../../less/main";

.edit-catch {
  .automatic-measure {
    margin-top: 20px;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
    overflow: hidden;
    @media screen and (max-width: @desktop-min-width) {
      margin-top: 0px;
      padding-right: 30px;
      width: 100%;
    }
  }
  .catch-picture {
    height: calc(165px + env(safe-area-inset-top));
    width: 100%;
    position: absolute;
    top: env(safe-area-inset-top);
    background-color: @gainsboro;

    &.keyboardShowing {
      display: none;
    }
  }
  .pic-focused {
    border: 10px solid red;
  }

  .pic-miniatures-container {
    display: flex;

    .pic-miniature {
      margin: 10px;
      max-width: 100px;
      max-height: 100px;

      &.pic-selected {
        border: 4px solid blue;
      }
    }
  }

  .position-error {
    font-size: 14px;
    color: @carrot-orange;
    font-style: italic;
  }

  .info {
    font-style: italic;
    font-weight: 300;
    font-size: @fontsize-info;
    line-height: calc(@fontsize-info + @line-height-padding-medium);
    color: @pale-sky;
    text-align: center;
  }

  .multiple-catchs-info {
    width: 100%;
    display: flex;
    align-items: center;
    color: @carrot-orange;
    i {
      margin-right: @margin-small;
    }
    span {
      font-style: italic;
      font-weight: normal;
      font-size: @fontsize-info;
    }
  }

  .sample-id-container {
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-top: @vertical-margin-medium;
    margin-bottom: @vertical-margin-medium;

    .description {
      font-size: @fontsize-header-paragraph;
      line-height: calc(
        @fontsize-header-paragraph + @line-height-padding-small
      );
      color: @black;
      margin: @vertical-margin-small;
    }

    @keyframes spin {
      100% {
        -webkit-transform: rotate(360deg);
        transform: rotate(360deg);
      }
    }

    .spinner {
      height: 40px;
      width: 40px;
      border-radius: 50%;
      border-top: 3px solid @pelorous;
      border-left: 3px solid @pelorous;
      animation: spin 2s linear infinite;
    }

    .sample-id {
      font-family: monospace, sans-serif;
      font-size: @fontsize-span-big;
      background-color: @gainsboro;
      padding-left: @margin-small;
      padding-right: @margin-small;
      padding-top: @vertical-margin-xx-small;
      padding-bottom: @vertical-margin-xx-small;
      border: 1px solid @gunmetal;
      border-radius: 5px;
      width: fit-content;
    }
  }

  .location {
    font-size: 14px;
    padding-top: 30px;
    display: flex;
    flex-direction: column;
    align-items: center;
    .separator {
      height: 1px;
      border-top: 1px solid @very-light-grey;
      border-radius: 1px;
      width: 80%;
    }
    .empty {
      color: @carrot-orange;
      font-style: italic;
    }
    .map {
      width: 100%;
      height: 200px;
    }
  }

  .measure-row {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    width: 100%;
    > :nth-child(1) {
      width: calc(100% - 200px);
      margin-top: 20px;
      min-width: 150px;
    }
    > :nth-child(2) {
      width: 200px;
    }
    @media screen and (min-width: 515px) {
      > :nth-child(1) {
        width: 300px;
      }
      > :nth-child(2) {
        width: calc(100% - 300px);
      }
    }
    @media screen and (min-width: @desktop-min-width) {
      > :nth-child(1) {
        width: calc(100% - 200px);
        margin-top: 20px;
      }
      > :nth-child(2) {
        width: 200px;
      }
    }
    @media screen and (min-width: 880px) {
      > :nth-child(1) {
        width: 300px;
      }
      > :nth-child(2) {
        width: calc(100% - 300px);
      }
    }
  }

  .measure-button-icon {
    margin-left: -4px;
  }
  #measure-button-text {
    padding-left: 8px;
  }
  #measure-button-text:after {
    content: "Mesure";
    @media screen and (min-width: 515px) {
      content: "Mesure automatique";
    }
    @media screen and (min-width: @desktop-min-width) {
      content: "Mesure";
    }
    @media screen and (min-width: 880px) {
      content: "Mesure automatique";
    }
  }
  @media screen and (min-width: @desktop-min-width) {
    &.picture-background .edit-catch-page {
      .pane {
        margin-top: unset;
        .pane-content {
          height: 100%;
          .catch-picture-desktop-and-form {
            display: flex;
            flex-direction: column;
          }
        }
      }
    }
    .catch-picture-desktop {
      width: 100%;
      height: 220px;

      .no-picture {
        border: 1px dashed @pale-sky;
        border-radius: 8px;
      }
    }

    .sample {
      &.two-columns-row-on-desktop {
        align-items: center;
      }
      .info {
        font-size: @fontsize-info-desktop;
        line-height: calc(@fontsize-info-desktop + @line-height-padding-medium);
      }

      .sample-id-container {
        margin-top: 0px;
        margin-bottom: 0px;
      }
    }
  }

  @media screen and (min-width: 1264px) {
    &.picture-background
      .edit-catch-page
      .pane
      .pane-content
      .catch-picture-desktop-and-form {
      flex-direction: row;
      justify-content: space-between;
    }
  }

  @media screen and (min-width: 1264px) and (max-width: 1407px) {
    .catch-picture-desktop {
      width: 339px;
      height: 437px;
    }
    .edit-catch-form {
      width: calc(100% - 339px - @margin-large);
    }
  }

  @media screen and (min-width: 1408px) and (max-width: 1599px) {
    .catch-picture-desktop {
      width: 424px;
      height: 547px;
    }
    .edit-catch-form {
      width: calc(100% - 424px - @margin-large);
    }
  }

  @media screen and (min-width: 1600px) {
    .catch-picture-desktop {
      width: 530px;
      height: 684px;
    }
    .edit-catch-form {
      width: calc(100% - 530px - @margin-large);
    }
  }
}
</style>
