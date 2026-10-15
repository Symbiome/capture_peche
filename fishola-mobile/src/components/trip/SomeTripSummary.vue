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
  <div class="some-trip-summary" v-if="ready">
    <div class="two-columns-row-on-desktop">
      <div>
        <FormInput
          name="name"
          label="Nom de la sortie"
          placeholder="Nommez votre sortie"
          v-model="trip.name"
          v-bind:error="nameError"
          v-bind:readonly="readonly"
        />
        <FormInput
          v-if="readonly"
          name="lake"
          label="Plan d'eau"
          v-bind:value="selectedLakeName"
          v-bind:readonly="true"
        />
        <LakeSelection
          v-else
          v-bind:selectedLakes="selectedLakes"
          v-bind:favoriteLakes="noFavorites"
          v-bind:error="lakeIdError"
          v-on:updated="onLakeSelected"
        />
        <FormSelect
          class="hide-on-mobile"
          name="type"
          label="Type de pêche"
          v-bind:options="allTripTypes"
          orderBy="name"
          v-model="trip.type"
          v-bind:readonly="readonly"
        />
      </div>
      <div>
        <FormInput
          name="date"
          v-bind:label="readonly && !multiDay ? 'Date' : 'Date de début'"
          type="date"
          v-model="date"
          v-bind:error="dateError"
          v-bind:readonly="readonly"
        />
        <FormInput
          name="startAt"
          label="Heure de début"
          type="time"
          v-model="startedAt"
          v-bind:error="startedAtError"
          v-bind:readonly="readonly"
        />
        <FormInput
          v-if="!readonly || multiDay"
          name="endDate"
          label="Date de fin"
          type="date"
          v-model="endDate"
          v-bind:error="endDateError"
          v-bind:readonly="readonly"
        />
        <FormInput
          name="finishedat"
          label="Heure de fin"
          type="time"
          v-model="finishedAt"
          v-bind:error="finishedAtError"
          v-bind:readonly="readonly"
        />
      </div>
    </div>
    <div class="two-columns-row-on-desktop">
      <FormSelect
        name="weather"
        label="Météo (optionnelle)"
        v-bind:options="allWeathers"
        orderBy="name"
        v-model="trip.weatherId"
        v-bind:error="weatherIdError"
        v-bind:readonly="readonly"
      />
      <FormSelect
        class="hide-on-desktop"
        name="type"
        label="Type de pêche"
        v-bind:options="allTripTypes"
        orderBy="name"
        v-model="trip.type"
        v-bind:readonly="readonly"
      />
    </div>
    <div class="two-columns-row-on-desktop">
      <FormMultiValues
        name="species"
        v-bind:label="speciesLabel"
        v-bind:values="species"
        v-bind:readonly="readonly"
        v-on:clicked="$emit('goEditSpecies')"
      />
      <FormMultiValues
        name="techniques"
        v-bind:label="techniquesLabel"
        v-bind:values="techniques"
        v-bind:readonly="readonly"
        v-on:clicked="$emit('goEditTechniques')"
      />
    </div>
    <TripPositionsMap
      v-bind:beginLatitude="trip.beginLatitude"
      v-bind:beginLongitude="trip.beginLongitude"
      v-bind:endLatitude="trip.endLatitude"
      v-bind:endLongitude="trip.endLongitude"
      v-bind:editable="!readonly"
      v-bind:centerLat="currentLake ? currentLake.latitude : undefined"
      v-bind:centerLng="currentLake ? currentLake.longitude : undefined"
      v-on:end-position-picked="onEndPositionPicked"
    />
  </div>
</template>

<script lang="ts">
import TripSummary from "@/pojos/TripSummary";
import {
  WaterEntity as Lake,
  Weather,
  SpeciesWithAlias,
  Technique,
} from "@/pojos/BackendPojos";

import Helpers from "@/services/Helpers";
import TripDates from "@/services/TripDates";
import TripsService from "@/services/TripsService";
import { WeathersTripTypesSpeciesAndTechniques } from "@/services/ReferentialService";
import ReferentialService from "@/services/ReferentialService";

import FormInput from "@/components/common/FormInput.vue";
import FormSelect from "@/components/common/FormSelect.vue";
import FormMultiValues from "@/components/common/FormMultiValues.vue";
import LakeSelection from "@/components/common/LakeSelection.vue";
import TripPositionsMap from "@/components/trip/TripPositionsMap.vue";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";

@Component({
  components: {
    FormInput,
    FormSelect,
    FormMultiValues,
    LakeSelection,
    TripPositionsMap,
  },
})
export default class SomeTripSummary extends Vue {
  @Prop() trip!: TripSummary;
  @Prop() readonly!: boolean;

  // On est obligés de gérer un flag de ce genre, sinon les FormSelect
  // sont créés à vide et ne sélectionnent pas les bonnes valeurs
  ready: boolean = false;

  date: string = "";
  startedAt: string = "";
  endDate: string = "";
  finishedAt: string = "";
  multiDay: boolean = false;

  dateError: string = "";
  startedAtError: string = "";
  endDateError: string = "";
  finishedAtError: string = "";
  nameError: string = "";
  lakeIdError: string = "";
  weatherIdError: string = "";

  species: string[] = [];
  speciesLabel: string = "Espèce recherchée";
  techniques: string[] = [];
  techniquesLabel: string = "Technique utilisée";
  types: string[] = [];

  allSpecies: SpeciesWithAlias[] = [];
  allWeathers: Weather[] = [];
  allTripTypes: any[] = [];
  allTechniques: Technique[] = [];
  noFavorites: Lake[] = [];

  // Plan d'eau : autocomplete (recherche serveur, #7) au lieu d'un select de
  // toutes les entités (inutilisable à l'échelle France). Le lac déjà associé
  // à la sortie est résolu séparément (currentLake, cf. loadCurrentLake) au
  // lieu de charger le référentiel national complet pour un seul id (#128).
  currentLake: Lake | null = null;

  get selectedLakes(): Lake[] {
    return this.currentLake ? [this.currentLake] : [];
  }

  get selectedLakeName(): string {
    return this.currentLake ? this.currentLake.name : "";
  }

  onLakeSelected(lake: Lake) {
    // Mutation directe du modèle (comme l'ancien v-model) — la sauvegarde est
    // déclenchée par le parent au « Terminer », pas à la sélection.
    this.trip.lakeId = lake ? lake.id : "";
    this.currentLake = lake || null;
  }

  // Point de fin placé par l'utilisateur sur la carte (#86). On le projette
  // ensuite sur l'entité hydro la plus proche (point le plus proche de sa
  // géométrie), comme pour le point de début. On ne le contraint PAS à
  // l'entité de la sortie : le pêcheur a pu terminer sur une autre rivière /
  // mare / étang. Mutation directe du modèle ; la sauvegarde est déclenchée
  // par le parent au « Terminer ».
  onEndPositionPicked(coords: { lat: number; lng: number }) {
    this.$set(this.trip, "endLatitude", coords.lat);
    this.$set(this.trip, "endLongitude", coords.lng);
    this.snapEndPositionToNearestWaterEntity(coords);
  }

  private snapEndPositionToNearestWaterEntity(coords: { lat: number; lng: number }) {
    ReferentialService.getAttribution(coords.lat, coords.lng)
      .then((res) => {
        const snapped = res && res.proposal ? res.proposal.closestPoint : null;
        if (snapped) {
          this.$set(this.trip, "endLatitude", snapped.lat);
          this.$set(this.trip, "endLongitude", snapped.lng);
        }
      })
      // Hors ligne / erreur serveur : on conserve le point brut posé par
      // l'utilisateur (le point de fin reste optionnel et non bloquant).
      .catch(() => undefined);
  }

  created() {
    const currentLakePromise = this.trip.lakeId
      ? ReferentialService.getLakesIndex().then((index) => index.get(this.trip.lakeId) || null)
      : Promise.resolve(null);
    Promise.all([
      ReferentialService.getWeathersTripTypesSpeciesAndTechniques(),
      currentLakePromise,
    ]).then(([data, currentLake]) => {
      this.currentLake = currentLake;
      this.referentialsLoaded(data);
    });
  }

  referentialsLoaded(data: WeathersTripTypesSpeciesAndTechniques) {
    this.allWeathers.push({
      id: "__none__",
      name: "",
      exportAs: "",
    });
    data.weathers.forEach((weather) => this.allWeathers.push(weather));
    data.tripTypes.forEach((type) => this.allTripTypes.push(type));
    data.techniques.forEach((technique) => this.allTechniques.push(technique));
    this.allSpecies = data.species;
    this.tripLoaded(this.trip);
  }

  tripLoaded(someTrip: TripSummary) {
    if (!someTrip.weatherId || someTrip.weatherId == null) {
      someTrip.weatherId = "__none__";
    }

    this.multiDay = TripDates.isMultiDay(someTrip);
    const endIsoDate = TripDates.endIsoDate(someTrip);
    if (someTrip.date) {
      if (this.readonly) {
        this.date = Helpers.formatToLongDate(someTrip.date);
        this.endDate = endIsoDate ? Helpers.formatToLongDate(TripDates.parseIsoDate(endIsoDate)) : "";
      } else {
        this.date = Helpers.formatToDate(someTrip.date);
        this.endDate = endIsoDate || this.date;
      }
    }
    if (someTrip.startedAt) {
      this.startedAt = Helpers.truncateTimeToMinutes(someTrip.startedAt);
    }
    if (someTrip.finishedAt) {
      this.finishedAt = Helpers.truncateTimeToMinutes(someTrip.finishedAt);
    }

    someTrip.speciesIds.forEach((speciesId: string) => {
      this.allSpecies.forEach((s) => {
        if (s.id == speciesId) {
          const speciesDisplayValue = s.alias
            ? `${s.alias} (${s.name})`
            : s.name;
          this.species.push(speciesDisplayValue);
        }
      });
    });
    if (someTrip.otherSpecies) {
      this.species.push(someTrip.otherSpecies);
    }
    if (this.species.length > 1) {
      this.speciesLabel = "Espèces recherchées";
    }

    if (someTrip.techniqueIds) {
      this.allTechniques.forEach((technique: Technique) => {
        someTrip.techniqueIds.forEach((techniqueId) => {
          if (techniqueId == technique.id) {
            this.techniques.push(technique.name);
          }
        });
      });
    }
    if (this.techniques.length > 1) {
      this.techniquesLabel = "Techniques utilisées";
    }

    this.allTripTypes.forEach((tt) => {
      if (tt.id == someTrip.type) {
        this.types.push(tt.name);
      }
    });

    this.ready = true;
  }

  // Date de fin pré-remplie avec la date de début (#237), qu'elle suit tant qu'elle lui est égale.
  @Watch("date")
  onDateChanged(newDate: string, oldDate: string) {
    if (!this.readonly && (!this.endDate || this.endDate == oldDate)) {
      this.endDate = newDate;
    }
  }

  emitUpdatedTrip() {
    let hasError = false;

    if (this.trip!.name) {
      this.nameError = "";
    } else {
      hasError = true;
      this.nameError = "Vous devez nommer la sortie";
    }

    hasError = this.verifyDate(hasError);

    if (hasError) {
      this.$root.$emit(
        "toaster-error",
        "Vous devez renseigner les champs obligatoires"
      );
      return;
    }
    TripsService.confirmPlausibleTripDuration(this.$modal, this.trip!).then((confirmed) => {
      if (!confirmed) {
        return;
      }
      if (this.trip!.weatherId == "__none__") {
        delete this.trip!.weatherId;
      }
      // On émet au parent le modèle mis à jour
      this.$emit("trip-modified", this.trip!);
    });
  }

  private verifyDate(hasError: boolean) {
    const period = {
      date: this.date ? TripDates.parseIsoDate(this.date) : undefined,
      startedAt: this.startedAt,
      endDate: this.endDate,
      finishedAt: this.finishedAt,
    };
    const errors = TripDates.validatePeriod(period, (this.trip as any).catchs);
    this.dateError = errors.dateError;
    this.startedAtError = errors.startedAtError;
    this.endDateError = errors.endDateError;
    this.finishedAtError = errors.finishedAtError;
    Object.assign(this.trip!, period);
    return hasError || Object.values(errors).some((error) => !!error);
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

</style>
