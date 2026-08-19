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
  <div class="edit-trip-summary page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-summary-page page">
      <SomeTripHeader v-bind:trip="trip" class="hide-on-desktop" />
      <div class="pane">
        <div class="pane-content rounded">
          <h1>
            <BackButton class="hide-on-mobile" back-event="onBackButton" v-on:onBackButton="backToGaleryOrTrips" />
            Récapitulatif
          </h1>
          <SomeTripSummary ref="summary" v-if="trip.lakeId" v-bind:trip="trip" v-on:trip-modified="onUpdatedTrip"
            v-on:goEditSpecies="goEditSpecies" v-on:goEditTechniques="goEditTechniques" />

          <div class="end-position-capture">
            <button
              type="button"
              class="end-position-capture-button"
              v-on:click="captureEndPosition"
              v-bind:disabled="capturingEndPosition"
            >
              <i class="icon-map" />
              {{ endPositionCaptured ? "Position de fin enregistrée" : "Enregistrer ma position de fin" }}
            </button>
            <span v-if="endPositionError" class="position-error">
              <i class="icon-warning" />
              {{ endPositionError }}
            </span>
          </div>

          <div class="buttons-bar hide-on-mobile">
            <div class="button button-primary">
              <button v-on:click="startSave">
                <i class="icon-send" /> Terminer
              </button>
            </div>
            <div class="button button-secondary">
              <button v-on:click="giveup">Abandon</button>
            </div>
          </div>

          <div class="bottom-page-spacer keyboardSensitive"></div>
        </div>
      </div>
    </div>
    <FisholaFooter button-text="Terminer" button-icon="icon-send" v-on:buttonClicked="startSave"
      back-event="onBackButton" v-on:onBackButton="backToGaleryOrTrips" shortcuts="back,step-4-4,giveup"
      :isWaitingForPositionBeforeGoingToNextPage="isWaitingForPositionBeforeGoingToNextPage
        " />
  </div>
</template>

<script lang="ts">
import { RouterUtils } from "@/router/RouterUtils";
import TripSummary from "@/pojos/TripSummary";
import { Technique } from "@/pojos/BackendPojos";

import TripsService from "@/services/TripsService";
import GeolocationService from "@/services/GeolocationService";

import Helpers from "@/services/Helpers";

import BackButton from "@/components/common/BackButton.vue";
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import SomeTripHeader from "@/components/trip/SomeTripHeader.vue";
import SomeTripSummary from "@/components/trip/SomeTripSummary.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import { Component, Prop, Vue } from "vue-property-decorator";
import router from "../../router";
import ReferentialService from "../../services/ReferentialService";

export type ActionType = "SendTrip" | "EditSpecies" | "EditTechniques";

@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    BackButton,
    SomeTripSummary,
    FisholaFooter,
  },
})
export default class TripSummaryView extends Vue {
  @Prop() id!: string;
  @Prop({ default: false }) fromGallery: boolean;
  @Prop({ default: "" }) lakeFilter: string;

  trip?: TripSummary = {
    id: "",
    name: "",
    mode: "Live",
    startedAt: "",
    lakeId: "",
    date: new Date(),
    type: "Craft",
    speciesIds: [],
    otherSpecies: "",
    techniqueIds: [],
  };
  techniquesIndex: Map<string, Technique>;

  actionRequested: ActionType = "SendTrip";

  isWaitingForPositionBeforeGoingToNextPage = false;

  capturingEndPosition = false;
  endPositionCaptured = false;
  endPositionError = "";

  created() {
    TripsService.getTrip(this.id, this.tripLoaded);
    ReferentialService.getTechniquesIndex().then(
      (index: Map<string, Technique>) => (this.techniquesIndex = index)
    );
  }

  tripLoaded(someTrip: TripSummary) {
    this.trip = someTrip;
    this.endPositionCaptured = !!(someTrip.endLatitude && someTrip.endLongitude);
  }

  // Position de fin optionnelle (#86) : capture manuelle, même mécanique que la
  // capture GPS automatique (finishTripBootstrap / sendTripAndCancelCreations),
  // mais explicite et non bloquante — utile notamment en mode "Afterwards", où
  // aucune capture automatique n'a lieu.
  captureEndPosition() {
    this.capturingEndPosition = true;
    this.endPositionError = "";
    GeolocationService.checkWatchAndGetPositionUntilTimeout().then(
      (position) => {
        this.capturingEndPosition = false;
        this.endPositionCaptured = true;
        this.trip!.endLatitude = position.coords.latitude;
        this.trip!.endLongitude = position.coords.longitude;
      },
      (e) => {
        this.capturingEndPosition = false;
        console.error("Pas de coordonnées GPS pour la position de fin", e);
        if (JSON.stringify(e).indexOf("location unavailable") != -1) {
          this.endPositionError =
            "La position n'est pas activée, impossible d'enregistrer la position de fin";
        } else if (JSON.stringify(e).indexOf("User denied") != -1) {
          this.endPositionError =
            "Partage de position refusé, impossible d'enregistrer la position de fin";
        } else {
          this.endPositionError = "Position indisponible pour le moment";
        }
      }
    );
  }

  startSave() {
    // On demande au composant enfant de fournir le modèle mis à jour
    const summaryComponent: any = this.$refs.summary;
    summaryComponent.emitUpdatedTrip();
  }

  giveup() {
    Helpers.confirm(
      this.$modal,
      "Voulez-vous vraiment abandonner cette sortie ?"
    ).then(this.giveupConfirmed);
  }

  giveupConfirmed() {
    TripsService.cancelCreations();
    RouterUtils.pushRouteNoDuplicate(this.$router, "/my-trips/list");
  }

  onUpdatedTrip(trip: any) {
    // On reçoit le modèle mis à jour, on le sauvegarde
    if (this.actionRequested == "SendTrip") {
      if (!trip.techniqueIds || trip.techniqueIds.length < 1) {
        this.$root.$emit(
          "toaster-error",
          "Vous devez définir les techniques de pêche utilisées"
        );
        this.goEditTechniques();
      } else {
        // On force l'utilisateur à vérifier les techniques via la popup
        let liList: string = "";
        trip.techniqueIds.forEach((techniqueId: string) => {
          const techniqueName = this.techniquesIndex!.get(techniqueId)!.name;
          liList += `<li>${techniqueName}</li>`;
        });
        const confirmText = `Les techniques suivantes ont été détectées. Est-ce correct ?<br/><ul>${liList}</ul>`;

        Helpers.confirm(
          this.$modal,
          confirmText,
          "Techniques de pêche",
          "Corriger",
          "C'est bon"
        ).then(
          () => {
            this.isWaitingForPositionBeforeGoingToNextPage = true;
            TripsService.sendTripAndCancelCreations(trip).then(
              this.tripSaved,
              (e) => {
                this.isWaitingForPositionBeforeGoingToNextPage = false;
                // Plafond de sorties non synchronisées atteint (#10) : message clair.
                if (e && e.offlineLimitReached) {
                  Helpers.alert(this.$modal, e.message, "Limite atteinte");
                } else {
                  console.error(
                    "Unexpected error during sendTripAndCancelCreations",
                    e
                  );
                }
              }
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
    this.isWaitingForPositionBeforeGoingToNextPage = false;
    if (this.actionRequested == "SendTrip") {
      RouterUtils.pushRouteNoDuplicate(this.$router, "/my-trips/list");
      this.$root.$emit("ask-for-sync-check");
    } else if (this.actionRequested == "EditSpecies") {
      RouterUtils.pushRouteNoDuplicate(this.$router, {
        name: "trip-species",
        params: { id: this.trip!.id },
      });
    } else if (this.actionRequested == "EditTechniques") {
      RouterUtils.pushRouteNoDuplicate(this.$router, {
        name: "trip-techniques",
        params: { id: this.trip!.id },
      });
    }
  }

  backToGaleryOrTrips() {
    if (this.fromGallery) {
      RouterUtils.pushRouteNoDuplicate(this.$router, {
        name: "galery",
        params: {
          selectedDefaultPic: "",
          selectedLakeIdProp: this.lakeFilter,
        },
      });
    } else {
      RouterUtils.pushRouteNoDuplicate(this.$router, "/my-trips/list");
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.end-position-capture {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: @vertical-margin-medium;

  .end-position-capture-button {
    height: 45px;
    border-radius: 38px;
    padding-left: @margin-medium;
    padding-right: @margin-medium;

    background-color: transparent;
    color: @pelorous;
    border: 1px solid @pelorous;

    font-weight: bold;
    font-size: @fontsize-button;

    i {
      margin-right: @margin-x-small;
    }

    &:disabled {
      opacity: 0.6;
    }
  }
}

.position-error {
  margin-top: @vertical-margin-x-small;
  font-size: 14px;
  color: @carrot-orange;
  font-style: italic;
}
</style>
