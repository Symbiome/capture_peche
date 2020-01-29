<template>
  <div class="edit-catch page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-catch-page page">
      <div class="pane">
        <h1>Capture</h1>
        <div class="pane-content">
          <FormSelect name="species"
                      label="Éspèce"
                      v-bind:options="allSpecies"
                      v-model="aCatch.speciesId"
                      v-bind:error="speciesIdError"
                      v-bind:readonly="readonly"/>
          <FormInput name="size"
                     label="Taille (en cm)"
                     type="number"
                     placeholder="Entrez une taille (en cm)"
                     v-model="aCatch.size"
                     v-bind:error="sizeError"
                     v-bind:readonly="readonly"/>
          <FormInput name="weight"
                     label="Poids (en g, optionnel)"
                     type="number"
                     placeholder="Entrez un poids (en g)"
                     v-model="aCatch.weight"
                     v-bind:error="weightError"
                     v-bind:readonly="readonly"/>
          <FormYesNo name="keep"
                     label="Conservez-vous ce poisson ?"
                     v-model="aCatch.keep"
                     v-bind:error="keepError"
                     v-bind:readonly="readonly"/>
          <FormSelect name="releaseState"
                      v-if="aCatch.keep === false"
                      label="État du poisson relâché"
                      v-bind:options="allReleasedFishStates"
                      v-model="aCatch.releasedStateId"
                      v-bind:error="releasedStateIdError"
                      v-bind:readonly="readonly"/>
          <FormSelect name="technique"
                      label="Technique de pêche"
                      v-bind:options="allTechniques"
                      v-model="aCatch.techniqueId"
                      v-bind:error="techniqueIdError"
                      v-bind:readonly="readonly"/>
          <FormTextarea name="description"
                        label="Description (optionnelle)"
                        placeholder="Écrivez une description"
                        v-model="aCatch.description"
                        v-bind:readonly="readonly"/>
          <FormInput name="caughtAt"
                     label="Heure (optionnelle)"
                     type="time"
                     v-model="caughtAt"
                     v-bind:readonly="readonly"/>
          <FormToggle label="Prélèvement (optionnel)"
                      v-model="aCatch.withSample"
                      v-bind:readonly="readonly"/>
          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter v-if="modifiable"
                   button-text="Valider"
                   button-icon="icon-fish"
                   v-on:buttonClicked="validateClicked"
                   shortcuts="back,step-3-4,delete"/>
    <FisholaFooter v-if="!modifiable"
                   shortcuts="back,step-3-4,delete"/>
  </div>
</template>

<script lang="ts">
import {TripBean, Technique, SpeciesWithAlias, ReleasedFishState} from '@/pojos/BackendPojos';
import CatchSummary from '@/pojos/CatchSummary';

import TripsService from '@/services/TripsService';
import ReferentialService from '@/services/ReferentialService';
import Helpers from '@/pojos/Helpers';

import FisholaHeader from '@/layout/FisholaHeader.vue'
import FormSelect from '@/components/common/FormSelect.vue'
import FormToggle from '@/components/common/FormToggle.vue'
import FormInput from '@/components/common/FormInput.vue'
import FormYesNo from '@/components/common/FormYesNo.vue'
import FormTextarea from '@/components/common/FormTextarea.vue'
import FormMultiValues from '@/components/common/FormMultiValues.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

@Component({
  components: {
    FisholaHeader,
    FormInput,
    FormYesNo,
    FormTextarea,
    FormMultiValues,
    FormSelect,
    FormToggle,
    FisholaFooter
  }
})
export default class EditCatch extends Vue {

  @Prop() tripId!:string;
  @Prop() catchId!:string;

  aCatch: CatchSummary = {id: '', withSample: false};

  caughtAt:string = '';

  speciesIdError:string = '';
  sizeError:string = '';
  weightError:string = '';
  keepError:string = '';
  releasedStateIdError:string = '';
  techniqueIdError:string = '';

  readonly:boolean = false;

  modifiable:boolean = true;

  allSpecies:SpeciesWithAlias[] = [];
  allTechniques:Technique[] = [];
  allReleasedFishStates:ReleasedFishState[] = [];

  created() {
    TripsService.getTripAndCatch(this.tripId, this.catchId, this.tripAndCatchLoaded);
  }

  mounted() {
  }

  tripAndCatchLoaded(someTrip:TripBean, someCatch:CatchSummary) {
    let lakeId:string = someTrip.lakeId;
    this.aCatch = someCatch;

    if (someCatch.caughtAt) {
      this.caughtAt = Helpers.formatToTime(someCatch.caughtAt);
    }

    ReferentialService.getSpeciesTechniquesAndReleasedFishStates(lakeId, this.speciesAndTechniquesLoaded);
  }

  speciesAndTechniquesLoaded(species:SpeciesWithAlias[], techniques:Technique[], states:ReleasedFishState[]) {
    this.allSpecies = species;
    this.allTechniques = techniques;
    this.allReleasedFishStates= states;
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

    if (hasError) {
      this.$root.$emit('toaster-error', 'Vous devez renseigner les champs obligatoires');
    } else {
      this.$root.$emit('toaster-warning', 'Work in progress');
    }
  }

  onUpdatedTrip(trip:TripBean) {
      // On reçoit le modèle mis à jour, on le sauvegarde
      // TripsService.sendTrip(trip, this.tripSaved);
  }

  // tripSaved() {
  //   router.push('/trips');
  // }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

.edit-catch-page {

}

</style>
