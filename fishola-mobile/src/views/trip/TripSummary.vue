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
  <div class="edit-trip-summary page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-summary-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="pane">
        <div class="pane-content rounded">
          <h1>Récapitulatif</h1>
          <SomeTripSummary ref="summary"
                           v-if="trip.lakeId"
                           v-bind:trip="trip"
                           v-on:trip-modified="onUpdatedTrip"
                           v-on:goEditSpecies="goEditSpecies"
                           v-on:goEditTechniques="goEditTechniques"
                           />
          <div class="bottom-page-spacer keyboardSensitive"></div>
        </div>
      </div>
    </div>
    <FisholaFooter button-text="Terminer"
                   button-icon="icon-send"
                   v-on:buttonClicked="startSave"
                   shortcuts="back,step-4-4,giveup"/>
  </div>
</template>

<script lang="ts">
import TripSummary from '@/pojos/TripSummary';
import { Technique } from '@/pojos/BackendPojos';

import TripsService from '@/services/TripsService';

import Helpers from '@/services/Helpers';

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import SomeTripSummary from '@/components/trip/SomeTripSummary.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';
import ReferentialService from '../../services/ReferentialService';

export type ActionType = "SendTrip" | "EditSpecies" | "EditTechniques";

@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    SomeTripSummary,
    FisholaFooter
  }
})
export default class TripSummaryView extends Vue {

  @Prop() id!:string;

  trip?:TripSummary = { id:'', name:'',  mode:'Live', startedAt: '', lakeId:'', date: new Date(), type:'Craft', speciesIds:[], otherSpecies:'', techniqueIds:[] };
  techniquesIndex:Map<string, Technique>;

  actionRequested:ActionType = "SendTrip";

  created() {
    TripsService.getTrip(this.id, this.tripLoaded);
    ReferentialService.getTechniquesIndex().then((index:Map<string, Technique>) => this.techniquesIndex = index);
  }

  mounted() {
  }

  tripLoaded(someTrip:TripSummary) {
    this.trip = someTrip;
  }

  startSave() {
    // On demande au composant enfant de fournir le modèle mis à jour
    const summaryComponent:any = this.$refs.summary;
    summaryComponent.emitUpdatedTrip();
  }

  onUpdatedTrip(trip:any) {
    // On reçoit le modèle mis à jour, on le sauvegarde
    if (this.actionRequested == "SendTrip") {
      if (!trip.techniqueIds || trip.techniqueIds.length < 1) {
        this.$root.$emit('toaster-error', 'Vous devez définir les techniques de pêche utilisées');
        this.goEditTechniques();
      } else {
        // On force l'utilisateur à vérifier les techniques via la popup
        let liList:string = '';
        trip.techniqueIds.forEach((techniqueId:string) => {
          const techniqueName = this.techniquesIndex!.get(techniqueId)!.name;
          liList += `<li>${techniqueName}</li>`;
        });
        const confirmText = `Les techniques suivantes ont été détectées. Est-ce correct ?<br/><ul>${liList}</ul>`;

        Helpers.confirm(this.$modal, confirmText, 'Techniques de pêche', 'Corriger', 'C\'est bon')
          .then(
            () => {
              TripsService.sendTripAndCancelCreations(trip)
                .then(
                  this.tripSaved,
                  (e) => console.error("Unexpected error during sendTripAndCancelCreations", e)
                );
            },
            () => {
              this.goEditTechniques();
            }
          );

      }
    } else {
      TripsService.saveTrip(trip, this.tripSaved);
    }
  }

  goEditSpecies() {
    this.actionRequested = "EditSpecies";
    this.startSave();
  }

  goEditTechniques() {
    this.actionRequested = "EditTechniques";
    this.startSave();
  }

  tripSaved() {
    if (this.actionRequested == "SendTrip") {
      router.push('/trips');
      this.$root.$emit('ask-for-sync-check');
    } else if (this.actionRequested == "EditSpecies") {
      router.push({name:'trip-species', params: {id: this.trip!.id}});
    } else if (this.actionRequested == "EditTechniques") {
      router.push({name:'trip-techniques', params: {id: this.trip!.id}});
    }
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

</style>
