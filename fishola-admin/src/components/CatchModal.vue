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
      <section class="section">
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
          <b-field label="Taille corrigée (cm)" class="column">
            <b-numberinput
              v-model="aCatch.editedSize"
              type="numeric"
              class="number-input"
            />
          </b-field>
          <b-field label="Taille renseignée par le pêcheur" class="column">
            <b-numberinput
              v-model="aCatch.size"
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
          <b-field label="Poids renseigné par le pêcheur" class="column">
            <b-numberinput
              v-model="aCatch.weight"
              disabled
              type="numeric"
              class="number-input"
            />
          </b-field>
        </div>
      </section>
      <b-field label="Image">
        <img
          class="miniture-pic"
          v-if="aCatch['miniatureURL']"
          :src="aCatch['miniatureURL']"
        />
      </b-field>

      <b-field label="Bool">
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
      </b-field>
      <div class="spacer" />
    </div>

    {{ aCatch }}
    <hr />
    {{ trip }}
    <div class="buttons">
      <button
        v-if="aCatch.id"
        class="button is-primary"
        @click="save($parent.close)"
      >
        Enregistrer
      </button>
      <button
        class="button"
        type="button"
        @click="cancelAndClose($parent.close)"
      >
        Annuler
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";

import BackendService from "@/services/BackendService";

@Component({
  components: {}
})
export default class CatchModal extends Vue {
  @Prop() catchId: string;
  aCatch: any = {};
  trip: any = {};
  speciesIdMap = new Map<string, string>();
  speciesNamesMap = new Map<string, string>();
  sortedSpeciesNames: Array<string> = [];

  mounted(): void {
    this.loadCatch();
  }

  async loadCatch() {
    let species = await BackendService.backendGet(
      "/v1/referential/raw-species"
    );
    species.forEach((specie: { id: string; name: string }) => {
      this.speciesIdMap.set(specie.id, specie.name);
      this.speciesNamesMap.set(specie.name, specie.id);
      this.sortedSpeciesNames.push(specie.name);
    });
    this.sortedSpeciesNames = this.sortedSpeciesNames.sort();
    this.trip = await BackendService.backendGet(
      "/v1/trips/catches/" + this.catchId
    );
    this.aCatch = this.trip.catchs.find((c: any) => c.id == this.catchId);
  }

  async save(closeModal: () => void) {
    let url = "v1/trips/catches/" + this.catchId;
    try {
      await BackendService.backendPut(url, this.aCatch);
      this.$emit("referential-updated");
      if (closeModal) {
        closeModal();
      }
    } catch (e) {
      console.error(e);
      this.$buefy.toast.open({
        message:
          "Erreur lors de la modification de la prise. Veuillez vérifier vos modifications.",
        type: "is-danger"
      });
    }
  }

  cancelAndClose(closeModal: () => void) {
    this.$emit("referential-updated");
    if (closeModal) {
      closeModal();
    }
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
}
</style>
