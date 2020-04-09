<template>
  <div class="edit-catch page-with-header-and-footer picture-background">
    <FisholaHeader />
    <div class="catch-picture">
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
      <div class="pane">
        <h1>Capture</h1>
        <div class="pane-content" v-if="ready">
          <FormSelect name="species"
                      label="Espèce"
                      v-bind:options="allSpecies"
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
                     label="Taille en cm"
                     type="number"
                     placeholder="Entrez une taille en centimètres"
                     v-model="aCatch.size"
                     v-bind:error="sizeError"
                     v-bind:readonly="!modifiable"/>
          <FormInput v-if="aCatch.weight || (settings && settings.promptWeight)"
                     name="weight"
                     label="Poids en g (optionnel)"
                     type="number"
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
                        label="Description (optionnelle)"
                        placeholder="Écrivez une description"
                        v-model="aCatch.description"
                        v-bind:readonly="!modifiable"/>
          <FormInput name="caughtAt"
                     label="Heure de la capture (optionnelle)"
                     type="time"
                     v-model="caughtAt"
                     v-bind:readonly="!modifiable"/>
          <FormToggle v-if="withSample || (settings && settings.promptSamples)"
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
          <div class="bottom-page-spacer"></div>
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

@Component({
  components: {
    FisholaHeader,
    FormInput,
    FormYesNo,
    FormTextarea,
    FormSelect,
    FormToggle,
    PicturePreview,
    FisholaFooter
  }
})
export default class EditCatchView extends Vue {

  @Prop() tripId!:string;
  @Prop() catchId!:string;

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

  allSpecies:SpeciesWithAlias[] = [];
  allTechniques:Technique[] = [];
  // allReleasedFishStates:ReleasedFishState[] = [];

  withSample:boolean = false;
  samplesDocumentationUrl:string = '';
  sampleIdReady:boolean = false;

  created() {
    TripsService.getTripAndCatch(this.tripId, this.catchId, this.tripAndCatchLoaded);
    this.inCreation = this.catchId == Constants.NEW_CATCH_ID;
    this.loadSettings();
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

  getLatestCatchSpecies(someTrip:TripBean):string|undefined {
    if (someTrip.catchs && someTrip.catchs.length > 0) {
      let latestCatch = someTrip.catchs[someTrip.catchs.length - 1];
      return latestCatch.speciesId;
    }
    return;
  }

  tripAndCatchLoaded(someTrip:TripBean, someCatch:CatchSummary) {
    let lakeId:string = someTrip.lakeId;
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
        let seconds:number = Helpers.computeDurationInSeconds(someTrip.startedAt, someCatch.caughtAt!);
        this.rightShortcut = 'timer-' + seconds;
      }
    }

    PicturesService.getPicture(someCatch.id)
      .then(this.pictureLoaded, this.noPictureFound);

    // FIXME AThimel 08/04/2020 : On désactive pour l'instant car ça cause un bug de rafrachissement en cas de changement d'espèce
    // if (this.inCreation) {
    //   let latestSpeciesId = this.getLatestCatchSpecies(someTrip);
    //   if (latestSpeciesId) {
    //     someCatch.speciesId = latestSpeciesId;
    //   }
    // }

    if (someCatch.sampleId) {
      this.sampleIdReady = true;
      this.withSample = true;
    }

    ReferentialService.getSpeciesAndTechniques(lakeId)
      .then(this.referentialLoaded);

    if (this.inCreation && this.inTripCreation && this.tripMode == 'Live') {
      GeolocationService.getPosition()
        .then(
          (position) => {
            this.aCatch.latitude = position.coords.latitude;
            this.aCatch.longitude = position.coords.longitude;
          },
          (e) => {
            console.error("Merdu", e);
          }
        );
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

  referentialLoaded(data:SpeciesWithAliasAndTechnique) {
    data.species.forEach((s) => {
      if (s.builtIn // Espèce de base
          || this.aCatch.speciesId == s.id // Espèce custom sélectionnée pour la capture
          || this.tripSpeciesIds.indexOf(s.id) != -1 // Espèce custom sélectionnée pour la sortie
        ) {
        this.allSpecies.push(s);
      }
    });
     // Espèce custom sélectionnée pour la sortie mais pas encore synchro
    if (this.tripOtherSpecies) {
      this.tripOtherSpecies
        .split(',')
        .forEach((name) => {
          let customSpecies:SpeciesWithAlias = {
            id: name,
            name: name,
            builtIn: false
          };
          this.allSpecies.push(customSpecies);
      });
    }
    this.allSpecies.push({id:'__other__', name:'Autre ...', builtIn: false});
    data.techniques.forEach((t) => this.allTechniques.push(t));
    // data.states.forEach((s) => this.allReleasedFishStates.push(s));
    this.ready = true;

    // Tentative pour déclencher l'ouverture de l'APN dès le début de la capture
    // Mais : https://stackoverflow.com/questions/33911801/input-file-click-no-working-no-event
    // Et : https://stackoverflow.com/questions/29728705/trigger-click-on-input-file-on-asynchronous-ajax-done/29873845#29873845
    // if (this.inCreation) {
    //   setTimeout(this.takePicture, 500);
    // }
  }

  takePicture() {
    if (this.modifiable) {
      let input:any = this.$refs.fileInput;
      input.click();
    }
  }

  readUploadedFile(file:any, callback: (fileContent:string) => void) {
    var reader = new FileReader();
    reader.onload = function readSuccess(loadEvt:any) {
        let content:string = loadEvt.target.result;
        callback(content);
    };
    reader.readAsDataURL(file);
  }

  pictureTaken(evt:any) {
    let file = evt.srcElement.files[0];
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

    if (!this.aCatch.size) {
      hasError = true;
      this.sizeError = 'Taille obligatoire';
    } else if (this.aCatch.size > 0) {
      this.sizeError = '';
    } else {
      hasError = true;
      this.sizeError = 'La taille doit être strictement positive';
    }

    if (!this.aCatch.weight || this.aCatch.weight > 0) {
      this.weightError = '';
    } else {
      hasError = true;
      this.weightError = 'Le poids doit être strictement positif';
    }

    if (this.aCatch.size && !this.sizeError && this.aCatch.weight && !this.weightError) {
      if (this.aCatch.size >= 25) {
        let minValue = 0.01 * Math.pow(this.aCatch.size, 2.7);
        let maxValue = 0.01 * Math.pow(this.aCatch.size, 3.2);
        if (this.aCatch.weight < minValue || this.aCatch.weight > maxValue) {
          console.log(`Le poids (${this.aCatch.weight}g) devrait se situer entre ${minValue}g et ${maxValue}g`);
          hasError = true;
          this.weightError = "Le poids n'est pas cohérent avec la taille";
        }
      } else if (this.aCatch.weight > 300) {
        console.log(`Le poids (${this.aCatch.weight}g) ne devrait pas dépasser 300g`);
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
      let aCatchBean:CatchBean = this.castToBean(this.aCatch);
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
    console.log("Capture enregistrée", catchId);
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
    height: 165px;
    width: 100%;
    position: absolute;
    top: 0;
    background-color: @gainsboro;
  }

  .info {
    font-style: italic;
    font-weight: 300;
    font-size: 10px;
    line-height: 14px;
    color: @pale-sky;
    text-align: center;
  }

  .sample-id-container {
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-top: 20px;
    margin-bottom: 20px;

    .description {
      font-size: 14px;
      line-height: 16px;
      color: @black;
      margin: 10px;
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
      font-size: 18px;
      background-color: @gainsboro;
      padding-left: 10px;
      padding-right: 10px;
      padding-top: 5px;
      padding-bottom: 5px;
      border: 1px solid @gunmetal;
      border-radius: 5px;
      width: fit-content;
    }
  }
}

</style>
