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
  <div class="edit-trip page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-page page">
      <SomeTripHeader v-if="ready"
                      v-bind:trip="trip"
                      class="hide-on-desktop"/>
      <div class="pane">
        <div class="pane-content large">
          <div class="edit-trip-header">
            <h1 class="hide-on-desktop">
              {{duration}}
            </h1>
            <h1 class="no-margin-pane hide-on-mobile">
              <BackButton/>
              {{trip.name}}
            </h1>
            <div v-if="modifiable"
                class="edit-trip-modifiable-until">
              <i class="icon-edit"/>
              <div>
                Vous avez encore {{modifiableDuration}}<br/>
                pour modifier cette sortie
              </div>
            </div>
          </div>
          <div class="edit-trip-catchs catch-preview-list-scrollable"
               v-if="modifiable || trip.catchs.length > 0">
            <CatchPreviewList v-if="ready"
                              v-bind:modifiable="modifiable"
                              v-bind:lakeId="trip.lakeId"
                              v-bind:catchs="trip.catchs"
                              v-on:newCatch="newCatch()"
                              v-on:openCatchFromId="openCatch($event)"/>
          </div>
          <div class="edit-trip-no-catch"
               v-if="!modifiable && trip.catchs.length == 0">
            <i class="icon-fish"/>
            <span>Aucune capture</span>
          </div>
          <SomeTripSummary ref="summary"
                           v-if="ready"
                           v-bind:trip="trip"
                           v-bind:readonly="!modifiable"
                           v-on:trip-modified="onUpdatedTrip"
                           v-on:goEditSpecies="goEditSpecies"
                           v-on:goEditTechniques="goEditTechniques"
                           class="summary-pane"/>

          <div class="buttons-bar hide-on-mobile">
            <div class="button button-primary" v-if="modifiable">
              <button v-on:click="startSave">
                Enregistrer
              </button>
            </div>
            <div class="button button-danger">
              <button v-on:click="deleteTrip">
                Supprimer
              </button>
            </div>
          </div>

          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter v-if="ready && modifiable"
                   button-text="Enregistrer"
                   v-on:buttonClicked="startSave"
                   v-on:deleteClicked="deleteTrip"
                   back-event="onBackButton"
                   v-on:onBackButton="returnToTrips"
                   shortcuts="back,spacer,delete"/>
    <FisholaFooter v-if="ready && !modifiable"
                   v-on:deleteClicked="deleteTrip"
                   back-event="onBackButton"
                   v-on:onBackButton="returnToTrips"
                   shortcuts="back,spacer,delete"/>
  </div>
</template>

<script lang="ts">
import {TripBean} from '@/pojos/BackendPojos';

import TripsService from '@/services/TripsService';
import Constants from '@/services/Constants';
import Helpers from '@/services/Helpers';

import BackButton from '@/components/common/BackButton.vue'
import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import SomeTripSummary from '@/components/trip/SomeTripSummary.vue'
import CatchPreviewList from '@/components/trip/CatchPreviewList.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

export type ActionType = "UpdateTrip" | "EditSpecies" | "EditTechniques";

@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    BackButton,
    SomeTripSummary,
    CatchPreviewList,
    FisholaFooter
  }
})
export default class EditTripView extends Vue {

  @Prop() id!:string;

  actionRequested:ActionType = "UpdateTrip";

  // Ce flag permet de ne créer le composant SomeTripSummary que lorsque les données sont prêtes
  ready:boolean = false;

  trip:TripBean = {
    id: '',
    mode: 'Live',
    source: 'application',
    type: 'Craft',
    name: '',
    lakeId: '',
    speciesIds: [],
    date: new Date(),
    startedAt: '',
    finishedAt: '',
    catchs: [],
    otherSpecies: '',
    techniqueIds:[]
  };

  duration:string = '';
  modifiable:boolean = false;
  modifiableDuration:string = '';

  created() {
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  mounted() {
  }

  tripLoaded(someTrip:TripBean) {
    this.trip = someTrip;
    if (this.trip.modifiableUntil) {
      this.modifiable = true;
      this.modifiableDuration = Helpers.humanizeDurationFromDates(new Date(), this.trip.modifiableUntil);
    }
    this.ready = true;
    this.duration = Helpers.renderDuration(this.trip.startedAt, this.trip.finishedAt);
  }

  startSave() {
    // On demande au composant enfant de fournir le modèle mis à jour
    const summaryComponent:any = this.$refs.summary;
    summaryComponent.emitUpdatedTrip();
  }

  onUpdatedTrip(trip:TripBean) {
    // On reçoit le modèle mis à jour, on le sauvegarde
    if (this.actionRequested == "UpdateTrip") {
      TripsService.sendTrip(trip)
        .then(this.tripSaved);
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
    if (this.actionRequested == "UpdateTrip") {
      router.push('/trips');
      this.$root.$emit('ask-for-sync-check');
    } else if (this.actionRequested == "EditSpecies") {
      router.push({name:'trip-species', params: {id: this.id}});
    } else if (this.actionRequested == "EditTechniques") {
      router.push({name:'trip-techniques', params: {id: this.id}});
    }
  }

  newCatch() {
    if (this.modifiable) {
      router.push({name:'catch', params: {tripId: this.id, catchId:Constants.NEW_CATCH_ID}});
    }
  }

  openCatch(catchId:string) {
    router.push({name:'catch', params: {tripId: this.id, catchId:catchId}});
  }

  deleteTrip() {
    Helpers.confirm(this.$modal, "Voulez-vous supprimer la sortie ?").then(() => {
      TripsService.deleteTrip(this.id, this.tripDeleted);
    });
  }

  tripDeleted() {
    this.returnToTrips();
  }

  returnToTrips() {
    router.push('/trips');
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

.edit-trip-page {

  h1 {
    color: @gunmetal;
  }

  .edit-trip-no-catch {

    display: flex;
    flex-direction: row;
    justify-content: center;

    margin-bottom: @vertical-margin-medium;

    font-size: @fontsize-small-paragraph;
    line-height: calc(@fontsize-small-paragraph + @line-height-padding-medium);
    color: @pale-sky;

    span {
      margin-left: @margin-x-small;
      font-style: italic;
    }

  }

  .edit-trip-catchs {

    display: flex;
    flex-direction: row;
    align-items: center;
    overflow-x: auto;
    overflow-y: hidden;

    height: 200px;
    margin-bottom: @vertical-margin-medium;

  }

  .edit-trip-modifiable-until {

    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;

    color: @terra-cotta;
    margin-bottom: @vertical-margin-large;


    @media(max-height:579px) {
      margin-bottom: @vertical-margin-medium;
    }


    i {
      font-size: @fontsize-button-big;
    }
    div {
      margin-left: @margin-small;
      text-align: left;
    }

  }

  .summary-pane {
    padding-left: @margin-large;
    padding-right: @margin-large;
  }

  @media screen and (min-width: @desktop-min-width) {
    .edit-trip-header {
      display: flex;
      flex-direction: row;
      br {
        display: none;
      }
    }

    .summary-pane {
      padding-left: @margin-large-desktop;
      padding-right: @margin-large-desktop;
    }
  }
}

</style>
