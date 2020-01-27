<template>
  <div class="edit-catch page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-catch-page page">
      <div class="pane">
        <h1>Capture</h1>
        <div class="pane-content">
          <FormSelect name="species"
                      label="Espèce"
                      v-bind:options="allSpecies"
                      v-model="speciesId"
                      v-bind:error="speciesIdError"
                      v-bind:readonly="readonly"/>
          <FormInput name="size"
                     label="Taille (en cm)"
                     type="number"
                     placeholder="Entrez une taille (en cm)"
                     v-model="size"
                     v-bind:error="sizeError"
                     v-bind:readonly="readonly"/>
          <FormInput name="weight"
                     label="Poids (en g, optionnel)"
                     type="number"
                     placeholder="Entrez un poids (en g)"
                     v-model="weight"
                     v-bind:error="weightError"
                     v-bind:readonly="readonly"/>
          <FormMultiValues name="keep"
                           label="Conservez-vous ce poisson ?"
                           v-bind:values="['Oui','Non']"
                           readonly="true"/>
          <FormSelect name="releaseState"
                      label="État du poisson relâché"
                      v-bind:options="allReleasedFishStates"
                      v-model="releasedStateId"
                      v-bind:error="releasedStateIdError"
                      v-bind:readonly="readonly"/>
          <FormSelect name="technique"
                      label="Technique de pêche"
                      v-bind:options="allTechniques"
                      v-model="techniqueId"
                      v-bind:error="techniqueIdError"
                      v-bind:readonly="readonly"/>
          <FormTextarea name="description"
                        label="Description (optionnel)"
                        placeholder="Écrivez une description"
                        v-model="description"
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

import TripsService from '@/services/TripsService';
import ReferentialService from '@/services/ReferentialService';
import Helpers from '@/pojos/Helpers';

import FisholaHeader from '@/layout/FisholaHeader.vue'
import FormSelect from '@/components/common/FormSelect.vue'
import FormInput from '@/components/common/FormInput.vue'
import FormTextarea from '@/components/common/FormTextarea.vue'
import FormMultiValues from '@/components/common/FormMultiValues.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

@Component({
  components: {
    FisholaHeader,
    FormInput,
    FormTextarea,
    FormMultiValues,
    FormSelect,
    FisholaFooter
  }
})
export default class EditCatch extends Vue {

  @Prop() tripId!:string;
  @Prop() catchId!:string;

  // TODO AThimel 27/01/2020 Utiliser plutôt le bean
  speciesId:string = '';
  techniqueId:string = '';
  size:string = '';
  weight:string = '';
  releasedStateId:string = '';
  description:string = '';

  speciesIdError:string = '';
  techniqueIdError:string = '';
  sizeError:string = '';
  weightError:string = '';
  releasedStateIdError:string = '';

  readonly:boolean = false;

  modifiable:boolean = true;

  allSpecies:SpeciesWithAlias[] = [];
  allTechniques:Technique[] = [];
  allReleasedFishStates:ReleasedFishState[] = [];

  created() {
    TripsService.getTrip(this.tripId, this.tripLoaded);
  }

  mounted() {
  }

  tripLoaded(someTrip:TripBean) {
    let lakeId:string = someTrip.lakeId;
    ReferentialService.getSpeciesTechniquesAndReleasedFishStates(lakeId, this.speciesAndTechniquesLoaded);
  }

  speciesAndTechniquesLoaded(species:SpeciesWithAlias[], techniques:Technique[], states:ReleasedFishState[]) {
    this.allSpecies = species;
    this.allTechniques = techniques;
    this.allReleasedFishStates= states;
  }

  validateClicked() {
    this.$root.$emit('toaster-warning', 'Work in progress');
      // // On demande au composant enfant de fournir le modèle mis à jour
      // let summaryComponent:any = this.$refs.summary;
      // summaryComponent.emitUpdatedTrip();
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
