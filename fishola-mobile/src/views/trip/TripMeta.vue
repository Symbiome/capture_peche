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
  <div class="edit-trip-meta page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-meta-page page">
      <SomeTripHeader v-bind:trip="trip" class="hide-on-desktop" />
      <div class="pane">
        <div class="pane-content rounded">
          <h1>
            <BackButton class="hide-on-mobile" />
            Information de pêche
          </h1>
          <div class="edit-trip-meta-form">
            <div class="form-block">
              <FormInput
                name="name"
                label="Nom de la sortie"
                placeholder="Nommez votre sortie"
                v-model="trip.name"
                v-bind:error="nameError"
              />
              <FormSelect
                name="lake"
                label="Lac"
                v-bind:options="lakes"
                orderBy="name"
                v-model="trip.lakeId"
                v-bind:error="lakeIdError"
              />

              <span v-if="hereIAmError" class="position-error">
                <i class="icon-warning" />
                {{ hereIAmError }}
              </span>

              <FormSelect
                name="type"
                label="Type de pêche"
                v-bind:options="types"
                orderBy="name"
                v-model="trip.type"
                v-bind:error="typeError"
              />
            </div>
            <div v-if="trip.mode == 'Afterwards'" class="form-block">
              <FormInput
                name="date"
                label="Date"
                type="date"
                v-model="date"
                v-bind:error="dateError"
              />
              <FormInput
                name="startAt"
                label="Heure de début"
                type="time"
                v-model="startedAt"
                v-bind:error="startedAtError"
              />
              <FormInput
                name="finishedat"
                label="Heure de fin"
                type="time"
                v-model="finishedAt"
                v-bind:error="finishedAtError"
              />
            </div>
          </div>

          <div class="buttons-bar hide-on-mobile">
            <div class="button button-primary">
              <button v-on:click="next">Suivant</button>
            </div>
            <div class="button button-secondary">
              <button v-on:click="giveup">Abandon</button>
            </div>
          </div>

          <div class="bottom-page-spacer keyboardSensitive"></div>
        </div>
      </div>
    </div>
    <FisholaFooter
      button-text="Suivant"
      v-on:buttonClicked="next"
      shortcuts="back,step-1-4,giveup"
    />
  </div>
</template>

<script lang="ts">
import TripMeta from "@/pojos/TripMeta";

import { Lake } from "@/pojos/BackendPojos";
import Constants from "@/services/Constants";
import Helpers from "@/services/Helpers";
import TripsService from "@/services/TripsService";
import { LakesAndTripTypes } from "@/services/ReferentialService";
import ReferentialService from "@/services/ReferentialService";
import GeolocationService from "@/services/GeolocationService";

import BackButton from "@/components/common/BackButton.vue";
import FormInput from "@/components/common/FormInput.vue";
import FormSelect from "@/components/common/FormSelect.vue";

import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import SomeTripHeader from "@/components/trip/SomeTripHeader.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import { Component, Prop, Vue } from "vue-property-decorator";
import router from "../../router";
import { RouterUtils } from "@/router/RouterUtils";

@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    BackButton,
    FormInput,
    FormSelect,
    FisholaFooter,
  },
})
export default class TripMetaView extends Vue {
  @Prop() id!: string;

  trip: TripMeta = { id: "", mode: "Live", date: new Date(), startedAt: "" };

  date: string = "";
  startedAt: string = "";
  finishedAt: string = "";

  hereIAmError: string = "";

  dateError: string = "";
  startedAtError: string = "";
  finishedAtError: string = "";
  nameError: string = "";
  lakeIdError: string = "";
  typeError: string = "";

  lakes: Lake[] = [];
  types: any[] = [];

  created() {
    ReferentialService.getLakesAndTripTypes().then(this.referentialsLoaded);
  }

  tripLoaded(someTrip: TripMeta) {
    console.debug("Trip chargé", someTrip);
    this.trip = someTrip;
    if (someTrip.mode == "Afterwards") {
      if (someTrip.date) {
        this.date = Helpers.formatToDate(someTrip.date);
      }
      if (someTrip.startedAt) {
        this.startedAt = someTrip.startedAt;
      }
      if (someTrip.finishedAt) {
        this.finishedAt = someTrip.finishedAt;
      }
    }

    if (this.id == Constants.NEW_TRIP_ID) {
      GeolocationService.getClosestLake().then(
        (lake: Lake) => {
          console.debug("Le lac le plus proche est ", lake);
          this.trip.lakeId = lake.id;
          // Les lignes suivantes sont une bidouille pour que le Select s'affiche .......
          this.lakeIdError = lake.id;
          this.lakeIdError = "";
        },
        (e) => {
          console.error(
            "Impossible de récupérer les coordonnées",
            JSON.stringify(e)
          );
          if (JSON.stringify(e).indexOf("location unavailable") != -1) {
            this.hereIAmError =
              "La position n'est pas activée, il n'est pas possible de pré-sélectionner le lac";
            if (!GeolocationService.notifiedPositionDisabled) {
              GeolocationService.notifiedPositionDisabled = true;
              Helpers.alert(
                this.$modal,
                "Vous devez activer la localisation de l'appareil pour que FISHOLA puisse acquérir votre position",
                "La position n'est pas activée"
              );
            }
          } else if (JSON.stringify(e).indexOf("User denied") != -1) {
            this.hereIAmError =
              "Partage de position refusé, il n'est pas possible de pré-sélectionner le lac";
          }
        }
      );
    }
  }

  referentialsLoaded(data: LakesAndTripTypes) {
    data.lakes.forEach((lake: Lake) => this.lakes.push(lake));
    data.tripTypes.forEach((type: any) => this.types.push(type));
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  next() {
    let hasError = false;
    if (this.trip.lakeId) {
      this.lakeIdError = "";
    } else {
      hasError = true;
      this.lakeIdError = "Vous devez sélectionner le lac";
    }
    if (this.trip.name) {
      this.nameError = "";
    } else {
      hasError = true;
      this.nameError = "Vous devez nommer la sortie";
    }
    if (this.trip.type) {
      this.typeError = "";
    } else {
      hasError = true;
      this.typeError = "Vous devez spécifier le type de pêche";
    }

    if (this.trip!.mode == "Afterwards") {
      if (this.date) {
        this.dateError = "";
        const newDate = new Date(this.date);
        this.trip!.date = newDate;

        if (this.startedAt) {
          this.startedAtError = "";

          // let startedAt = Helpers.parseDateTime(newDate, this.startedAt);
          this.trip!.startedAt = this.startedAt;
        } else {
          this.startedAtError = "Vous devez renseigner l'heure de début";
          hasError = true;
        }

        if (this.finishedAt) {
          this.finishedAtError = "";

          // let finishedAt = Helpers.parseDateTime(newDate, this.finishedAt);
          this.trip!.finishedAt = this.finishedAt;
        } else {
          this.finishedAtError = "Vous devez renseigner l'heure de fin";
          hasError = true;
        }
      } else {
        this.dateError = "Vous devez renseigner la date";
        hasError = true;
      }
    }

    if (hasError) {
      this.$root.$emit(
        "toaster-error",
        "Vous devez renseigner les champs obligatoires"
      );
    } else {
      // this.trip!.name = this.name;
      // this.trip!.lakeId = this.lakeId;
      // this.trip!.type = this.type;

      TripsService.saveTripMeta(this.trip!, this.tripSaved);
    }
  }

  tripSaved() {
    RouterUtils.pushRouteNoDuplicate(router, {
      name: "trip-species",
      params: { id: this.id },
    });
  }

  giveup() {
    Helpers.confirm(
      this.$modal,
      "Voulez-vous vraiment abandonner cette sortie ?"
    ).then(this.giveupConfirmed);
  }

  giveupConfirmed() {
    TripsService.cancelCreations();
    RouterUtils.pushRouteNoDuplicate(router, "/trips");
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../../less/main";

.position-error {
  font-size: 14px;
  color: @carrot-orange;
  font-style: italic;
}

.edit-trip-meta-form {
  display: flex;
  flex-direction: column;
  div.form-block {
    width: 100%;
  }
}

@media screen and (min-width: @desktop-min-width) {
  .edit-trip-meta-form {
    flex-direction: row;
    div.form-block {
      &:nth-child(odd) {
        margin-right: @margin-medium;
      }
      &:nth-child(even) {
        margin-left: @margin-medium;
      }
    }
  }
}
</style>
