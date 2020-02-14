<template>
  <div class="edit-catch page-with-header-and-footer picture-background">
    <FisholaHeader />
    <div class="catch-picture">
      <PicturePreview v-bind:src="pictureSrc"
                      noPictureText="Appuyer pour ajouter une photo"
                      v-on:take-picture="takePicture" />
      <input type="file"
             capture="camera"
             accept="image/*"
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
                      label="Éspèce"
                      v-bind:options="allSpecies"
                      v-model="aCatch.speciesId"
                      v-bind:error="speciesIdError"
                      v-bind:readonly="!modifiable"/>
          <FormInput name="size"
                     label="Taille (en cm)"
                     type="number"
                     placeholder="Entrez une taille (en cm)"
                     v-model="aCatch.size"
                     v-bind:error="sizeError"
                     v-bind:readonly="!modifiable"/>
          <FormInput name="weight"
                     label="Poids (en g, optionnel)"
                     type="number"
                     placeholder="Entrez un poids (en g)"
                     v-model="aCatch.weight"
                     v-bind:error="weightError"
                     v-bind:readonly="!modifiable"/>
          <FormYesNo name="keep"
                     label="Conservez-vous ce poisson ?"
                     v-model="aCatch.keep"
                     v-bind:error="keepError"
                     v-bind:readonly="!modifiable"/>
          <FormSelect name="releaseState"
                      v-if="aCatch.keep === false"
                      label="État du poisson relâché"
                      v-bind:options="allReleasedFishStates"
                      v-model="aCatch.releasedStateId"
                      v-bind:error="releasedStateIdError"
                      v-bind:readonly="!modifiable"/>
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
                     label="Heure (optionnelle)"
                     type="time"
                     v-model="caughtAt"
                     v-bind:readonly="!modifiable"/>
          <FormToggle label="Prélèvement (optionnel)"
                      v-model="aCatch.withSample"
                      v-bind:readonly="!modifiable"/>
          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter v-if="ready && modifiable"
                   v-bind:button-text="inCreation ? 'Valider' : 'Modifier'"
                   button-icon="icon-fish"
                   v-on:buttonClicked="validateClicked"
                   v-on:deleteClicked="deleteCatch"
                   v-bind:shortcuts="'back,' + middleShortcut + ',' + rightShortcut"/>
    <FisholaFooter v-if="ready && !modifiable"
                   shortcuts="back,spacer,blank"/>
  </div>
</template>

<script lang="ts">
import {TripBean, CatchBean, Technique, SpeciesWithAlias, ReleasedFishState} from '@/pojos/BackendPojos';
import CatchSummary from '@/pojos/CatchSummary';

import PicturesService from '@/services/PicturesService';
import TripsService from '@/services/TripsService';
import {SpeciesTechniquesAndReleasedFishStates} from '@/services/ReferentialService';
import ReferentialService from '@/services/ReferentialService';
import Helpers from '@/pojos/Helpers';

import FisholaHeader from '@/layout/FisholaHeader.vue'
import FormSelect from '@/components/common/FormSelect.vue'
import FormToggle from '@/components/common/FormToggle.vue'
import FormInput from '@/components/common/FormInput.vue'
import FormYesNo from '@/components/common/FormYesNo.vue'
import FormTextarea from '@/components/common/FormTextarea.vue'
import FormMultiValues from '@/components/common/FormMultiValues.vue'
import PicturePreview from '@/components/trip/PicturePreview.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';
import Constants from '../../services/Constants';

@Component({
  components: {
    FisholaHeader,
    FormInput,
    FormYesNo,
    FormTextarea,
    FormMultiValues,
    FormSelect,
    FormToggle,
    PicturePreview,
    FisholaFooter
  }
})
export default class EditCatch extends Vue {

  @Prop() tripId!:string;
  @Prop() catchId!:string;

  // On est obligés de gérer un flag de ce genre, sinon les FormSelect
  // sont créés à vide et ne sélectionnent pas les bonnes valeurs
  ready:boolean = false;

  tripDate?:Date;
  aCatch: CatchSummary = {id: '', withSample: false};

  pictureSrc:string = '';
  newPictureTaken:boolean = false;

  caughtAt:string = '';

  speciesIdError:string = '';
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
  allReleasedFishStates:ReleasedFishState[] = [];

  created() {
    TripsService.getTripAndCatch(this.tripId, this.catchId, this.tripAndCatchLoaded);
    this.inCreation = this.catchId == Constants.NEW_CATCH_ID;
  }

  mounted() {
  }

  tripAndCatchLoaded(someTrip:TripBean, someCatch:CatchSummary) {
    let lakeId:string = someTrip.lakeId;
    this.tripDate = someTrip.date;
    this.inTripCreation = !someTrip.createdOn;
    this.middleShortcut = (this.inTripCreation ? 'step-3-4':'spacer');
    this.modifiable = (this.inTripCreation || !!someTrip.modifiableUntil);
    this.aCatch = someCatch;

    if (someCatch.caughtAt) {
      this.caughtAt = Helpers.formatToTime(someCatch.caughtAt);

      if (this.inCreation && someTrip.mode == 'Live') {
        let millis:number = someCatch.caughtAt!.getTime() - someTrip.startedAt.getTime();
        this.rightShortcut = 'timer-' + Math.floor(millis/1000);
      }
    }

    if (someCatch.hasPicture) {
      PicturesService.getPicture(someCatch.id, this.pictureLoaded);
    }

    ReferentialService.getSpeciesTechniquesAndReleasedFishStates(lakeId).then(this.referentialLoaded);
  }

  pictureLoaded(content?:string) {
    this.pictureSrc = content ? content : Constants.apiUrl(`/v1/pictures/${this.aCatch.id}/preview`);
  }

  referentialLoaded(data:SpeciesTechniquesAndReleasedFishStates) {
    data.species.forEach((s) => this.allSpecies.push(s));
    data.techniques.forEach((t) => this.allTechniques.push(t));
    data.states.forEach((s) => this.allReleasedFishStates.push(s));
    this.ready = true;
  }

  takePicture() {
    let input:any = this.$refs.fileInput;
    input.click();
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

  validateClicked() {
    let hasError:boolean = false;
    if (this.aCatch.speciesId) {
      this.speciesIdError = '';
    } else {
      hasError = true;
      this.speciesIdError = 'Éspèce obligatoire';
    }

    if (!this.aCatch.size) {
      hasError = true;
      this.sizeError = 'Taille nobligatoire';
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
      this.weightError = 'Le poids ne peut pas être négatif';
    }

    if (this.aCatch.keep === true || this.aCatch.keep === false) {
      this.keepError = '';
    } else {
      hasError = true;
      this.keepError = 'Information obligatoire';
    }

    if (this.aCatch.keep === false)  {
      if (this.aCatch.releasedStateId) {
        this.releasedStateIdError = '';
      } else {
        hasError = true;
        this.releasedStateIdError = 'État du poisson relâché obligatoire';
      }
    } else {
        this.releasedStateIdError = '';
    }

    if (this.aCatch.techniqueId) {
      this.techniqueIdError = '';
    } else {
      hasError = true;
      this.techniqueIdError = 'Technique de pêche obligatoire';
    }

    if (this.caughtAt && this.caughtAt.length > 0) {
      let newDate = Helpers.parseDateTime(this.tripDate || new Date(), this.caughtAt);
      this.aCatch.caughtAt = newDate;
    } else {
      delete this.aCatch.caughtAt;
    }

    if (hasError) {
      this.$root.$emit('toaster-error', 'Vous devez renseigner les champs obligatoires');
    } else {
      let aCatchBean:CatchBean = this.castToBean(this.aCatch);
      aCatchBean.hasPicture = this.pictureSrc != '';
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
    TripsService.deleteCatch(this.tripId, this.catchId, this.leavePage);
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
}

</style>
