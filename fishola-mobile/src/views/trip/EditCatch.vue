<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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
    <div class="catch-picture keyboardSensitive">
      <PicturePreview v-bind:src="pictureSrc"
                      v-bind:modifiable="modifiable"
                      noPictureText="Appuyer pour ajouter une photo"
                      v-on:take-picture="takePicture" />
      <input type="file"
             accept=".png,.PNG,.jpg,.JPG,.jpeg,.JPEG"
             id="cameraInput"
             name="cameraInput"
             v-on:change="pictureTaken"
             style="display:none;"
             ref="fileInput">
    </div>
    <div class="edit-catch-page page">
      <div class="pane keyboardSensitive">
        <div class="pane-content rounded" v-if="ready">
          <h1>Capture</h1>

          <span v-if="catchPositionError" class="position-error">
            <i class="icon-warning"/>
            {{catchPositionError}}
          </span>

          <FormSelect name="species"
                      label="Espèce"
                      v-bind:options="allSpeciesWithAliases"
                      v-model="aCatch.speciesId"
                      v-bind:error="speciesIdError"
                      v-bind:readonly="!modifiable"/>
          <FormInput name="otherSpecies"
                     label="Si autre"
                     type="text"
                     placeholder="Renseigner l’espèce"
                     v-model="aCatch.otherSpecies"
                     v-bind:error="otherSpeciesError"
                     v-bind:readonly="!modifiable"
                     v-if="aCatch.speciesId == '__other__'"/>
          <FormInput name="size"
                     :label="sizeLabel"
                     type="number"
                     :min="1"
                     placeholder="Entrez une taille en centimètres"
                     v-model="aCatch.size"
                     v-bind:error="sizeError"
                     v-bind:readonly="!modifiable"/>
          <div class="multiple-catchs-info" v-if="multipleCatchsAllowed">
            <i class="icon-info"/>
            <span>
              Indiquez le poids total de vos captures si vous en avez plusieurs pour cette espèce
            </span>
          </div>
          <FormInput v-if="aCatch.weight || (settings && settings.promptWeight)"
                     name="weight"
                     label="Poids en g (optionnel)"
                     type="number"
                     :min="1"
                     placeholder="Entrez un poids en grammes"
                     v-model="aCatch.weight"
                     v-bind:error="weightError"
                     v-bind:readonly="!modifiable"/>
          <FormYesNo name="keep"
                     v-bind:label="tripMode == 'Live' ? 'Conservez-vous ce poisson ?' : 'Avez-vous conservé ce poisson ?'"
                     v-model="aCatch.keep"
                     v-bind:error="keepError"
                     v-bind:readonly="!modifiable"/>
          <!-- AThimel 27/02/2020 On désactive la saisie de l'état du poisson relâché. Cf cocoo n°9 -->
          <!--FormSelect name="releaseState"
                      label="État du poisson relâché"
                      v-bind:options="allReleasedFishStates"
                      v-model="aCatch.releasedStateId"
                      v-bind:error="releasedStateIdError"
                      v-bind:readonly="!modifiable"/-->
          <FormSelect name="technique"
                      label="Technique de pêche"
                      v-bind:options="allTechniques"
                      v-model="aCatch.techniqueId"
                      v-bind:error="techniqueIdError"
                      v-bind:readonly="!modifiable"/>
          <FormTextarea name="description"
                        label="Observation (optionnelle)"
                        placeholder="Écrivez une description, une observation, une remarque à propos de votre capture"
                        v-model="aCatch.description"
                        v-bind:readonly="!modifiable"/>
          <FormInput name="caughtAt"
                     label="Heure de la capture (optionnelle)"
                     type="time"
                     v-model="caughtAt"
                     v-bind:readonly="!modifiable"/>
          <FormToggle v-if="withSample
                            || ( settings
                                 && settings.promptSamples
                                 && (authorizedSampleSpeciesIds.indexOf(aCatch.speciesId) != -1)
                               )"
                      label="Prélèvement (optionnel)"
                      v-model="withSample"
                      v-bind:readonly="!modifiable"/>
          <div v-if="withSample">

            <div class="info"
                v-if="samplesDocumentationUrl">
              Pour pouvoir effectuer des prélèvements, vous devez vous munir
              d'un kit dans un des points de collecte :
              <a :href="samplesDocumentationUrl">consulter la liste</a>
            </div>

            <div class="sample-id-container">
              <div class="description">
                Numéro d'échantillon à reporter :
              </div>
              <div v-if="!sampleIdReady" class="spinner">&nbsp;</div>
              <div class="sample-id"
                   v-if="sampleIdReady">
                {{aCatch.sampleId}}
              </div>
            </div>

          </div>

          <div v-if="!inCreation" class="location">
            <div class="separator"></div>
            <p>Emplacement de la capture</p>
            <div class="empty" v-if="!gpsLocation">
              <i class="icon icon-warning"></i> Aucune position enregistrée pour cette prise
            </div>
            <div class="map" v-if="gpsLocation">
              <l-map
                :zoom="15"
                :center="gpsLocation"
                :options="{
                  zoomSnap: 0.5
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

          <div class="bottom-page-spacer keyboardSensitive"></div>
        </div>
      </div>
    </div>
    <FisholaFooter v-if="ready && modifiable"
                   v-bind:button-text="inCreation ? 'Valider' : 'Enregistrer'"
                   button-icon="icon-fish"
                   v-on:buttonClicked="validateClicked"
                   v-on:deleteClicked="deleteCatch"
                   v-bind:shortcuts="'back,' + middleShortcut + ',' + rightShortcut"/>
    <FisholaFooter v-if="ready && !modifiable"
                   shortcuts="back,spacer,blank"/>
  </div>
</template>

<script lang="ts">
import {TripBean, CatchBean, Technique, SpeciesWithAlias, TripMode} from '@/pojos/BackendPojos';
import CatchSummary from '@/pojos/CatchSummary';

import PicturesService from '@/services/PicturesService';
import TripsService from '@/services/TripsService';
import {SpeciesWithAliasAndTechnique} from '@/services/ReferentialService';
import ReferentialService from '@/services/ReferentialService';
import Helpers from '@/services/Helpers';
import GeolocationService from '@/services/GeolocationService';

import {UserSettings} from '@/pojos/BackendPojos';
import ProfileService from '@/services/ProfileService';

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import FormSelect from '@/components/common/FormSelect.vue'
import FormToggle from '@/components/common/FormToggle.vue'
import FormInput from '@/components/common/FormInput.vue'
import FormYesNo from '@/components/common/FormYesNo.vue'
import FormTextarea from '@/components/common/FormTextarea.vue'
import PicturePreview from '@/components/trip/PicturePreview.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import router from '../../router';
import Constants from '../../services/Constants';
import DocumentationService from '@/services/DocumentationService';

import { Plugins, CameraResultType } from '@capacitor/core';
const { Camera } = Plugins;
const { Device } = Plugins;

import { latLng, LatLng, Icon } from 'leaflet';

type D = Icon.Default & {
  _getIconUrl?: string;
};

delete (Icon.Default.prototype as D)._getIconUrl;

Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

import { LMap, LTileLayer, LMarker } from 'vue2-leaflet';

@Component({
  components: {
    FisholaHeader,
    FormInput,
    FormYesNo,
    FormTextarea,
    FormSelect,
    FormToggle,
    PicturePreview,
    LMap,
    LTileLayer,
    LMarker,
    FisholaFooter
  }
})
export default class EditCatchView extends Vue {

  @Prop() tripId!:string;
  @Prop() catchId!:string;

  platform:string = '';

  settings:UserSettings | null = null;

  // On est obligés de gérer un flag de ce genre, sinon les FormSelect
  // sont créés à vide et ne sélectionnent pas les bonnes valeurs
  ready:boolean = false;

  tripDate?:Date;
  tripMode:TripMode = 'Live';
  tripSpeciesIds:string[] = [];
  tripOtherSpecies:string = '';
  aCatch: CatchSummary = {id: ''};

  pictureSrc:string = '';
  newPictureTaken:boolean = false;

  caughtAt:string = '';

  defaultSizeLabel:string = "Taille en cm";
  sizeLabel:string = this.defaultSizeLabel;
  multipleCatchsAllowed:boolean = false;

  catchPositionError:string = '';
  speciesIdError:string = '';
  otherSpeciesError:string = '';
  sizeError:string = '';
  weightError:string = '';
  keepError:string = '';
  releasedStateIdError:string = '';
  techniqueIdError:string = '';

  modifiable:boolean = true;
  inCreation:boolean = true;
  inTripCreation:boolean = true;
  middleShortcut:string = '';
  rightShortcut:string = 'delete';

  allSpeciesWithAliases:SpeciesWithAlias[] = [];
  allTechniques:Technique[] = [];
  // allReleasedFishStates:ReleasedFishState[] = [];

  withSample:boolean = false;
  samplesDocumentationUrl:string = '';
  sampleIdReady:boolean = false;
  authorizedSampleSpeciesIds:string[] = [];

  gpsLocation:LatLng|null = null;

  created() {
    TripsService.getTripAndCatch(this.tripId, this.catchId, this.tripAndCatchLoaded);
    this.inCreation = this.catchId == Constants.NEW_CATCH_ID;
    this.loadSettings();

    Device.getInfo()
      .then(info => {this.platform = info.platform});
  }

  mounted() {
    this.samplesDocumentationUrl = DocumentationService.getSamplesDocumentationUrl();
  }

  loadSettings() {
    ProfileService.getSettings()
      .then(this.settingsLoaded);
  }

  settingsLoaded(settings:UserSettings) {
    this.settings = settings;
  }

  tripAndCatchLoaded(someTrip:TripBean, someCatch:CatchSummary) {
    const lakeId:string = someTrip.lakeId;
    this.tripDate = someTrip.date;
    this.tripSpeciesIds = someTrip.speciesIds;
    this.tripOtherSpecies = someTrip.otherSpecies;
    if (!someCatch.speciesId && someCatch.otherSpecies) {
      someCatch.speciesId = '__other__';
    }
    this.inTripCreation = !someTrip.createdOn;
    this.middleShortcut = (this.inTripCreation ? 'step-3-4':'spacer');
    this.modifiable = (this.inTripCreation || !!someTrip.modifiableUntil);
    this.tripMode = someTrip.mode;
    this.aCatch = someCatch;

    if (someCatch.caughtAt) {
      this.caughtAt = Helpers.truncateTimeToMinutes(someCatch.caughtAt);

      if (this.inCreation && this.inTripCreation && this.tripMode == 'Live') {
        const seconds:number = Helpers.computeDurationInSeconds(someTrip.startedAt, someCatch.caughtAt!);
        this.rightShortcut = 'timer-' + seconds;
      }
    }

    PicturesService.getPicture(someCatch.id)
      .then(this.pictureLoaded, this.noPictureFound);

    if (someCatch.sampleId) {
      this.sampleIdReady = true;
      this.withSample = true;
    }

    ReferentialService.getSpeciesAndTechniques(lakeId)
      .then(this.referentialLoaded);

    if (this.inCreation && this.inTripCreation && this.tripMode == 'Live') {
      GeolocationService.checkWatchAndGetPositionUntilTimeout()
        .then(
          (position) => {
            this.aCatch.latitude = position.coords.latitude;
            this.aCatch.longitude = position.coords.longitude;
            console.info(`Coordonnées de capture : ${this.aCatch.latitude},${this.aCatch.longitude}`);
          },
          (e) => {
            console.error("Error while geting location from geolocation service", JSON.stringify(e));
            if (JSON.stringify(e).indexOf('location unavailable') != -1) {
              this.catchPositionError = "La position n'est pas activée, la capture ne sera pas geolocalisée";
              if (!GeolocationService.notifiedPositionDisabled) {
                GeolocationService.notifiedPositionDisabled = true;
                Helpers.alert(this.$modal, 'Vous devez activer la localisation de l\'appareil pour que FISHOLA puisse acquérir votre position', 'La position n\'est pas activée');
              }
            } else if (JSON.stringify(e).indexOf('User denied') != -1) {
              this.catchPositionError = "Partage de position refusé, la capture ne sera pas geolocalisée";
            }

          }
        );
    }

    if (!this.inCreation && this.aCatch.latitude && this.aCatch.longitude) {
      this.gpsLocation = latLng(this.aCatch.latitude, this.aCatch.longitude);
    }
  }

  pictureLoaded(content:string) {
    this.pictureSrc = content;
  }

  noPictureFound() {
    if (this.aCatch.hasPicture) {
      this.pictureSrc = Constants.apiUrl(`/v1/pictures/${this.aCatch.id}/preview`);
    }
  }

  embedAlias(s:SpeciesWithAlias):SpeciesWithAlias {
    const result:SpeciesWithAlias = {
      id: s.id,
      name: s.alias ? (`${s.alias} (${s.name})`) : s.name,
      builtIn: s.builtIn,
      mandatorySize: s.mandatorySize,
      authorizedSample: s.authorizedSample
    };
    return result;
  }

  referentialLoaded(data:SpeciesWithAliasAndTechnique) {
    data.species.forEach((s) => {
      if (s.builtIn // Espèce de base
          || this.aCatch.speciesId == s.id // Espèce custom sélectionnée pour la capture
          || this.tripSpeciesIds.indexOf(s.id) != -1 // Espèce custom sélectionnée pour la sortie
        ) {
        const speciesWithAlias:SpeciesWithAlias = this.embedAlias(s);
        this.allSpeciesWithAliases.push(speciesWithAlias);
        if (s.authorizedSample) {
          this.authorizedSampleSpeciesIds.push(s.id);
        }
      }
    });
     // Espèce custom sélectionnée pour la sortie mais pas encore synchro
    if (this.tripOtherSpecies) {
      this.tripOtherSpecies
        .split(',')
        .forEach((name) => {
          const customSpecies:SpeciesWithAlias = {
            id: name,
            name: name,
            builtIn: false,
            mandatorySize: false,
            authorizedSample: false
          };
          this.allSpeciesWithAliases.push(customSpecies);
      });
    }
    this.allSpeciesWithAliases = Vue.lodash.orderBy(this.allSpeciesWithAliases, 'name');
    this.allSpeciesWithAliases.push({id:'__other__', name:'Autre ...', builtIn: false, mandatorySize: false, authorizedSample: false});
    data.techniques.forEach((t) => this.allTechniques.push(t));
    // data.states.forEach((s) => this.allReleasedFishStates.push(s));
    this.ready = true;

    // Tentative pour déclencher l'ouverture de l'APN dès le début de la capture
    // Mais : https://stackoverflow.com/questions/33911801/input-file-click-no-working-no-event
    // Et : https://stackoverflow.com/questions/29728705/trigger-click-on-input-file-on-asynchronous-ajax-done/29873845#29873845
    if (this.inCreation) {
      setTimeout(this.takePicture, 350);
    }
  }

  isMandatorySize(speciesId?:string):boolean {
    let result = true;
    if (speciesId) {
      this.allSpeciesWithAliases.forEach((s:SpeciesWithAlias) => {
        if (s.id == speciesId && s.mandatorySize === false) {
          result = false;
        }
      });
    }
    return result;
  }

  @Watch('aCatch.speciesId')
  speciesChanged(newValue:string, oldValue:string) {
    this.sizeLabel = this.defaultSizeLabel;
    this.multipleCatchsAllowed = false;
    if (newValue && this.isMandatorySize(newValue) === false) {
      this.sizeLabel = this.defaultSizeLabel + ' (optionnelle)';
      this.multipleCatchsAllowed = true;
    }
  }

  takePicture() {
    if (this.modifiable) {
      if (this.platform == "web") {
        // Si on est pas dans une APP on conserve le comportement avec le champ 'file'
        const input:any = this.$refs.fileInput;
        input.click();
      } else {
        Camera.getPhoto({
            quality: 95,
            allowEditing: false,
            resultType: CameraResultType.DataUrl,
            promptLabelCancel: 'Annuler',
            promptLabelPhoto: 'Sélectionner une image',
            promptLabelPicture: 'Prendre une photo'
          }).then(
            image => {
              var imageSrc = image.dataUrl;
              if (imageSrc) {
                this.pictureSrc = imageSrc;
                this.newPictureTaken = true;
              }
            },
            failure => {
              console.error('Unable to use camera', failure);
            }
          );
      }
    }
  }

  readUploadedFile(file:any, callback: (fileContent:string) => void) {
    var reader = new FileReader();
    reader.onload = function readSuccess(loadEvt:any) {
        const content:string = loadEvt.target.result;
        callback(content);
    };
    reader.readAsDataURL(file);
  }

  pictureTaken(evt:any) {
    const file = evt.srcElement.files[0];
    this.readUploadedFile(file, (content:string) => {
      this.pictureSrc = content;
      this.newPictureTaken = true;
    });
  }

  @Watch('withSample')
  onWithSampleChanged(value: boolean, oldValue: boolean) {
    if (value && !this.aCatch.sampleId) {
      TripsService.newSampleId()
        .then(this.onNewSampleId);
    }
  }

  onNewSampleId(newSampleId:string) {
    this.aCatch.sampleId = newSampleId;
    this.sampleIdReady = true;
  }

  validateClicked() {
    let hasError:boolean = false;

    if (this.aCatch.speciesId == '__other__') {

      this.speciesIdError = '';

      if (this.aCatch.otherSpecies) {
        if (this.aCatch.otherSpecies.indexOf(',') == -1) {
          this.otherSpeciesError = '';
        } else {
          hasError = true;
          this.otherSpeciesError = 'Vous ne pouvez pas spécifier plusieurs espèces ici';
        }
      } else {
        hasError = true;
        this.otherSpeciesError = 'Espèce obligatoire';
      }

    } else {

      this.otherSpeciesError = '';

      if (this.aCatch.speciesId) {
        this.speciesIdError = '';
      } else {
        hasError = true;
        this.speciesIdError = 'Espèce obligatoire';
      }

    }

    const mandatorySize = this.isMandatorySize(this.aCatch.speciesId);
    if (mandatorySize && !this.aCatch.size) {
      hasError = true;
      this.sizeError = 'Taille obligatoire';
    } else if (this.aCatch.size && this.aCatch.size <= 0) {
      hasError = true;
      this.sizeError = 'La taille doit être strictement positive';
    } else {
      if (this.aCatch.size) {
        if (this.aCatch.size != Math.floor(this.aCatch.size)) {
          hasError = true;
          this.sizeError = 'La taille doit être un nombre entier';
        } else {
          // On force pour stocker uniquement la valeur tronquée
          this.aCatch.size = Math.floor(this.aCatch.size);
          this.sizeError = '';
        }
      } else {
        this.sizeError = '';
      }
    }

    if (!this.aCatch.weight || this.aCatch.weight > 0) {
      if (this.aCatch.weight) {
        if (this.aCatch.weight != Math.floor(this.aCatch.weight)) {
          hasError = true;
          this.weightError = 'Le poids doit être un nombre entier';
        } else {
          // On force pour stocker uniquement la valeur tronquée
          this.aCatch.weight = Math.floor(this.aCatch.weight);
          this.weightError = '';
        }
      } else {
        this.weightError = '';
      }
    } else {
      hasError = true;
      this.weightError = 'Le poids doit être strictement positif';
    }

    if (this.aCatch.size && !this.sizeError && this.aCatch.weight && !this.weightError) {
      if (this.aCatch.size >= 25) {
        const minValue = 0.01 * Math.pow(this.aCatch.size, 2.7);
        const maxValue = 0.01 * Math.pow(this.aCatch.size, 3.2);
        if (this.aCatch.weight < minValue || this.aCatch.weight > maxValue) {
          console.info(`Le poids (${this.aCatch.weight}g) devrait se situer entre ${minValue}g et ${maxValue}g`);
          hasError = true;
          this.weightError = "Le poids n'est pas cohérent avec la taille";
        }
      } else if (this.aCatch.weight > 300) {
        console.info(`Le poids (${this.aCatch.weight}g) ne devrait pas dépasser 300g`);
        hasError = true;
        this.weightError = "Le poids n'est pas cohérent avec la taille";
      }
    }

    if (this.aCatch.keep === true || this.aCatch.keep === false) {
      this.keepError = '';
    } else {
      hasError = true;
      this.keepError = 'Information obligatoire';
    }

    // if (this.aCatch.keep === false)  {
    //   if (this.aCatch.releasedStateId) {
    //     this.releasedStateIdError = '';
    //   } else {
    //     hasError = true;
    //     this.releasedStateIdError = 'État du poisson relâché obligatoire';
    //   }
    // } else {
        this.releasedStateIdError = '';
    // }

    if (this.aCatch.techniqueId) {
      this.techniqueIdError = '';
    } else {
      hasError = true;
      this.techniqueIdError = 'Technique de pêche obligatoire';
    }

    if (this.caughtAt && this.caughtAt.length > 0) {
      this.aCatch.caughtAt = this.caughtAt;
    } else {
      delete this.aCatch.caughtAt;
    }

    if (hasError) {
      this.$root.$emit('toaster-error', 'Vous devez renseigner les champs obligatoires');
    } else {
      const aCatchBean:CatchBean = this.castToBean(this.aCatch);
      if (aCatchBean.speciesId == '__other__') {
        aCatchBean.speciesId = '';
      }
      aCatchBean.hasPicture = this.pictureSrc != '';
      if (!this.withSample) {
        aCatchBean.sampleId = '';
      }
      TripsService.saveCatch(this.tripId, aCatchBean, this.catchSaved);
    }
  }

  castToBean(input:any):CatchBean {
    return input;
  }

  catchSaved(catchId:string) {
    if (this.pictureSrc && this.newPictureTaken) {
      PicturesService.savePicture(catchId, this.pictureSrc, this.leavePage);
    } else {
      this.leavePage();
    }
  }

  deleteCatch() {
    Helpers.confirm(this.$modal, "Voulez-vous supprimer la capture ?")
      .then(() => {
        TripsService.deleteCatch(this.tripId, this.catchId, this.leavePage);
      });
  }

  leavePage() {
    if (this.inTripCreation) {
      router.push({name:'trip-catchs', params: {id: this.tripId}});
    } else {
      router.push({name:'trip', params: {id: this.tripId}});
    }
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

.edit-catch {
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
      line-height: calc(@fontsize-header-paragraph + @line-height-padding-small);
      color: @black;
      margin: @vertical-margin-small;
    }

    @keyframes spin { 100% { -webkit-transform: rotate(360deg); transform:rotate(360deg); } }

    .spinner {
      height: 40px;
      width: 40px;
      border-radius: 50%;
      border-top: 3px solid @pelorous;
      border-left: 3px solid @pelorous;
      animation:spin 2s linear infinite;
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

}

</style>
