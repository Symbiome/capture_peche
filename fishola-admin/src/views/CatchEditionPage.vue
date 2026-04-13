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
            <b-field
              label="Espèce corrigée"
              class="column"
            >
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

            <b-field
              label="Espèce renseignée par le pêcheur"
              class="column"
            >
              <b-select
                v-model="aCatch.speciesId"
                disabled
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
          </div>

          <div class="columns">
            <b-field
              label="Taille corrigée (mm)"
              class="column"
            >
              <b-numberinput
                v-model="aCatch.editedSize"
                type="numeric"
                class="number-input"
              />
            </b-field>
            <b-field
              label="Taille renseignée par le pêcheur (mm)"
              class="column"
            >
              <b-numberinput
                v-model="aCatch.sizeInMm"
                disabled
                type="numeric"
                class="number-input"
              />
            </b-field>
          </div>
          <div class="columns">
            <b-field
              label="Poids corrigé (g)"
              class="column"
            >
              <b-numberinput
                v-model="aCatch.editedWeight"
                type="numeric"
                class="number-input"
              />
            </b-field>
            <b-field
              label="Poids renseigné par le pêcheur (g)"
              class="column"
            >
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
            <b-field label="Plan d'eau"><span v-if="trip.lakeId"> {{ lakesIdMap.get(trip.lakeId) }}</span><span
                v-else>Non renseigné</span>
            </b-field>
            <b-field label="Date de la prise">
              <span v-if="trip.createdOn">{{ formatDate(trip.createdOn) }} </span><span v-else>Aucune</span>
            </b-field>
            <b-field label="Prise conservée"><span v-if="aCatch.keep">Oui</span><span v-else>Non</span>
            </b-field>
          </b-field>
          <b-field grouped>
            <b-field label="Mesure automatique"><span v-if="aCatch.automaticMeasure">{{
              aCatch.automaticMeasure
                }}</span><span v-else>Aucune</span>
            </b-field>
            <b-field label="Technique"><span v-if="aCatch.techniqueId">
                {{ techniquesIdMap.get(aCatch.techniqueId) }}</span><span v-else>Non renseignée</span>
            </b-field>
          </b-field>
          <hr />
          <b-field
            label="Photo de mesure automatique"
            v-if="measurementPicURL"
          >
            <a
              :href="measurementPicURL"
              target="blank"
            >
              <img
                alt="mesure"
                class="bo-detail-pic"
                :src="measurementPicURL"
              />
            </a>
          </b-field>

          <b-field
            label="Autres photos"
            v-if="otherPicsUrls.length"
          > </b-field>

          <a
            v-for="otherPicURL in otherPicsUrls"
            :key="otherPicURL"
            class="bo-detail-pic"
            :href="otherPicURL"
            target="blank"
          >
            <img
              class="bo-detail-pic"
              alt="prise"
              :src="otherPicURL"
            />
          </a>
        </section>
      </div>
    </div>

    <div class="buttons">
      <button
        v-if="aCatch.id"
        class="button is-primary"
        @click="save()"
      >
        Enregistrer
      </button>
      <button
        class="button"
        type="button"
        @click="cancel()"
      >
        Annuler
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import Constants from "@/services/Constants";
import UtilityServices from "@/services/UtilityServices";

import router from "@/router";
import { onMounted, reactive, ref, Ref } from "vue";
import { useToast } from "buefy";

const Toast = useToast();

const { catchId } = defineProps<{
  catchId: string,
}>();

const emit = defineEmits<{
  (e: "referentialUpdated"): void,
}>();

const aCatch: Ref<any> = ref({});
const trip: Ref<any> = ref({});
const speciesIdMap = reactive(new Map<string, string>());
const techniquesIdMap = reactive(new Map<string, string>());
const lakesIdMap = reactive(new Map<string, string>());
const speciesNamesMap = reactive(new Map<string, string>());
const sortedSpeciesNames: Ref<Array<string>> = ref([]);
const measurementPicURL = ref("");
const otherPicsUrls: Ref<Array<string>> = ref([]);

onMounted(() => {
  loadCatch();
});

async function loadCatch() {
  if (speciesIdMap.size == 0) {
    const species = await BackendService.backendGet(
      "/v1/referential/raw-species"
    );
    species.forEach((specie: { id: string; name: string }) => {
      speciesIdMap.set(specie.id, specie.name);
      speciesNamesMap.set(specie.name, specie.id);
      sortedSpeciesNames.value.push(specie.name);
    });
    sortedSpeciesNames.value = sortedSpeciesNames.value.sort();

    const techniques = await BackendService.backendGet(
      "/v1/referential/techniques"
    );
    techniques.forEach((technique: { id: string; name: string }) => {
      techniquesIdMap.set(technique.id, technique.name);
    });
    const lakes = await BackendService.backendGet("/v1/referential/lakes");
    lakes.forEach((lake: { id: string; name: string }) => {
      lakesIdMap.set(lake.id, lake.name);
    });
  }
  measurementPicURL.value = "";
  otherPicsUrls.value = [];
  trip.value = await BackendService.backendGet(
    "/v1/trips/catches/" + catchId
  );
  aCatch.value = trip.value.catchs.find((c: any) => c.id == catchId);
  if (aCatch.value.hasMeasurementPicture) {
    measurementPicURL.value = Constants.apiUrl(
      `/v1/pictures/measure/${aCatch.value.id}/preview`
    );
  }
  aCatch.value.pictureOrders.forEach((picOrder: string) => {
    const otherPicURL = Constants.apiUrl(
      `/v1/pictures/${catchId}/${picOrder}`
    );
    otherPicsUrls.value.push(otherPicURL);
  });
  aCatch.value.sizeInMm = aCatch.value.size * 10;
}

async function save() {
  const url = "/v1/trips/catches/" + catchId;
  try {
    await BackendService.backendPut(url, aCatch.value);
    emit("referentialUpdated");
    Toast.open({
      message: "Informations modifiée avec succès.",
      type: "is-success"
    });
    loadCatch();
  } catch (e) {
    console.error(e);
    Toast.open({
      message:
        "Erreur lors de la modification de la prise. Veuillez vérifier vos modifications.",
      type: "is-danger"
    });
  }
}

function cancel() {
  router.go(-1);
}

function formatDate(date: number[]): string {
  return UtilityServices.formatDate(date);
}
</script>

<style lang="less">
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
