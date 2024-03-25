<!--
  #%L
  Fishola :: Admin
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
  <div class="referential-aCatch">
    <h1 v-if="trip.name">
      {{ trip.name }} -
      <span>
        {{ speciesIdMap.get(aCatch.editedSpeciesId) }}
      </span>
    </h1>
    <div>
      <div class="columns">
        <section class="section column">
          <h2 class="title">Informations éditables</h2>
          <div class="columns">
            <b-field label="Espèce corrigée" class="column">
              <b-select
                placeholder="Sélectionnez une espèce"
                v-model="aCatch.editedSpeciesId"
              >
                <option
                  v-for="specie in sortedSpeciesNames"
                  :value="speciesNamesMap.get(specie)"
                  :key="specie"
                >
                  {{ specie }}
                </option>
              </b-select>
            </b-field>

            <b-field label="Espèce renseignée par le pêcheur" class="column">
              <b-select v-model="aCatch.speciesId" disabled>
                <option
                  v-for="specie in sortedSpeciesNames"
                  :value="speciesNamesMap.get(specie)"
                  :key="specie"
                >
                  {{ specie }}
                </option>
              </b-select>
            </b-field>
          </div>

          <div class="columns">
            <b-field label="Taille corrigée (mm)" class="column">
              <b-numberinput
                v-model="aCatch.editedSize"
                type="numeric"
                class="number-input"
              />
            </b-field>
            <b-field label="Taille renseignée par le pêcheur (mm)" class="column">
              <b-numberinput
                v-model="aCatch.sizeInMm"
                disabled
                type="numeric"
                class="number-input"
              />
            </b-field>
          </div>
          <div class="columns">
            <b-field label="Poids corrigé (g)" class="column">
              <b-numberinput
                v-model="aCatch.editedWeight"
                type="numeric"
                class="number-input"
              />
            </b-field>
            <b-field label="Poids renseigné par le pêcheur (g)" class="column">
              <b-numberinput
                v-model="aCatch.weight"
                disabled
                type="numeric"
                class="number-input"
              />
            </b-field>
          </div>
          <b-field label="Exclure des exports"> </b-field>

          <b-radio
            v-model="aCatch.excludeFromExport"
            name="excludeFromExport"
            :native-value="true"
          >
            Oui
          </b-radio>
          <b-radio
            v-model="aCatch.excludeFromExport"
            name="excludeFromExport"
            :native-value="false"
          >
            Non
          </b-radio>
        </section>

        <section class="section column">
          <h2 class="title">Autres informations</h2>
          <b-field grouped>
            <b-field label="Lac"
              ><span v-if="trip.lakeId"> {{ lakesIdMap.get(trip.lakeId) }}</span
              ><span v-else>Non renseigné</span>
            </b-field>
            <b-field label="Date de la prise">
              <span v-if="trip.createdOn"
                >{{ formatDate(trip.createdOn) }} </span
              ><span v-else>Aucune</span>
            </b-field>
            <b-field label="Prise conservée"
              ><span v-if="aCatch.keep">Oui</span><span v-else>Non</span>
            </b-field>
          </b-field>
          <b-field grouped>
            <b-field label="Mesure automatique"
              ><span v-if="aCatch.automaticMeasure">{{
                aCatch.automaticMeasure
              }}</span
              ><span v-else>Aucune</span>
            </b-field>
            <b-field label="Technique"
              ><span v-if="aCatch.techniqueId">
                {{ techniquesIdMap.get(aCatch.techniqueId) }}</span
              ><span v-else>Non renseignée</span>
            </b-field>
          </b-field>
          <hr />
          <b-field label="Photo de mesure automatique" v-if="measurementPicURL">
            <a :href="measurementPicURL" target="blank">
              <img
                alt="photo de mesure"
                class="bo-detail-pic"
                :src="measurementPicURL"
              />
            </a>
          </b-field>

          <b-field label="Autres photos" v-if="otherPicsUrls.length"> </b-field>

          <a
            v-for="otherPicURL in otherPicsUrls"
            :key="otherPicURL"
            class="bo-detail-pic"
            :href="otherPicURL"
            target="blank"
          >
            <img
              class="bo-detail-pic"
              alt="photo de la prise"
              :src="otherPicURL"
            />
          </a>
        </section>
      </div>
    </div>

    <div class="buttons">
      <button v-if="aCatch.id" class="button is-primary" @click="save()">
        Enregistrer
      </button>
      <button class="button" type="button" @click="cancel()">
        Annuler
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";

import BackendService from "@/services/BackendService";
import UtilityServices from "@/services/UtilityServices";
import Constants from "@/services/Constants";

@Component({
  components: {}
})
export default class CatchEditionPage extends Vue {
  @Prop() catchId: string;
  aCatch: any = {};
  trip: any = {};
  speciesIdMap = new Map<string, string>();
  techniquesIdMap = new Map<string, string>();
  lakesIdMap = new Map<string, string>();
  speciesNamesMap = new Map<string, string>();
  sortedSpeciesNames: Array<string> = [];
  measurementPicURL = "";
  otherPicsUrls: Array<string> = [];

  mounted(): void {
    this.loadCatch();
  }

  async loadCatch() {
    if (this.speciesIdMap.size == 0) {
      let species = await BackendService.backendGet(
        "/v1/referential/raw-species"
      );
      species.forEach((specie: { id: string; name: string }) => {
        this.speciesIdMap.set(specie.id, specie.name);
        this.speciesNamesMap.set(specie.name, specie.id);
        this.sortedSpeciesNames.push(specie.name);
      });
      this.sortedSpeciesNames = this.sortedSpeciesNames.sort();

      let techniques = await BackendService.backendGet(
        "/v1/referential/techniques"
      );
      techniques.forEach((technique: { id: string; name: string }) => {
        this.techniquesIdMap.set(technique.id, technique.name);
      });
      let lakes = await BackendService.backendGet("/v1/referential/lakes");
      lakes.forEach((lake: { id: string; name: string }) => {
        this.lakesIdMap.set(lake.id, lake.name);
      });
    }
    this.measurementPicURL = "";
    this.otherPicsUrls = [];
    this.trip = await BackendService.backendGet(
      "/v1/trips/catches/" + this.catchId
    );
    this.aCatch = this.trip.catchs.find((c: any) => c.id == this.catchId);
    if (this.aCatch.hasMeasurementPicture) {
      this.measurementPicURL = Constants.apiUrl(
        `/v1/pictures/measure/${this.aCatch.id}/preview`
      );
    }
    this.aCatch.pictureOrders.forEach((picOrder: string) => {
      const otherPicURL = Constants.apiUrl(
        `/v1/pictures/${this.catchId}/${picOrder}`
      );
      this.otherPicsUrls.push(otherPicURL);
    });
    this.aCatch.sizeInMm = this.aCatch.size * 10;
  }

  async save() {
    let url = "/v1/trips/catches/" + this.catchId;
    try {
      await BackendService.backendPut(url, this.aCatch);
      this.$emit("referential-updated");
      this.$buefy.toast.open({
        message: "Informations modifiée avec succès.",
        type: "is-success"
      });
      this.loadCatch();
    } catch (e) {
      console.error(e);
      this.$buefy.toast.open({
        message:
          "Erreur lors de la modification de la prise. Veuillez vérifier vos modifications.",
        type: "is-danger"
      });
    }
  }

  cancel() {
    this.$router.go(-1);
  }

  formatDate(date: number[]): string {
    return UtilityServices.formatDate(date);
  }
}
</script>

<style lang="less">
@import "../less/main";

.referential-aCatch {
  padding: 10px;

  h2 {
    font-size: 24px;
  }

  background-color: @white;

  .number-input {
    max-width: 250px;
  }
  .section {
    max-width: 900px;
  }
  .bo-detail-pic {
    max-height: 300px;
  }
}
</style>
