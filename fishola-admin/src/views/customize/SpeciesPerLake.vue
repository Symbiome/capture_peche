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
  <div class="species-per-lake">
    <h1 id="table-title">Espèces par plan d'eau</h1>
    <div v-if="lakes.length > maxLakeBeforeShowingAutoComplete">
      Veuillez indiquer les plans d'eau à afficher
      <MultipleAutoComplete
        :defaultSelection="lastLakeSelection"
        :data="lakeSelectionOptions"
        @updated="(value) => changeLakeSelection(value)"
      />
    </div>

    <b-message type="is-info">
      Cochez les espèces présentes sur le plan d'eau. Vous pouvez préciser un nom spécifique si différent du nom
      d'espèce (e.g. le "corégone" est appelé "Lavaret" sur le lac d'Aiguebelette et "Féran" sur le Léman)
    </b-message>
    <table
      class="table is-striped"
      aria-describedby="table-title"
      v-if="selectedLakes.length > 0"
    >
      <thead>
        <tr>
          <th id="empty-lake-cell"></th>
          <th
            :id="l.name"
            v-for="l in selectedLakes"
            v-bind:key="l.id"
          >
            {{ l.name }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="s in species"
          v-bind:key="s.id"
        >
          <th :id="s.name">{{ s.name }}</th>
          <td
            v-for="l in selectedLakes"
            v-bind:key="l.id"
          >
            <div
              class="field"
              style="display:flex;gap:5px"
            >
              <input
                type="checkbox"
                :checked="!speciesPerLakeAbsent[l.id] || speciesPerLakeAbsent[l.id].indexOf(s.id) === -1"
                @click="toggleAbsent(l.id, s.id)"
              />
              <div v-if="speciesPerLakeAbsent[l.id] && speciesPerLakeAbsent[l.id].indexOf(s.id) > -1">
                Espèce non présente
              </div>
              <b-input
                v-else
                v-model="speciesPerLakeAliases[l.id][s.id]"
                placeholder="Nom spécifique"
              ></b-input>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="buttons">
      <button
        class="button is-primary"
        @click="save()"
      >Enregistrer</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, ref, Ref } from "vue";

import BackendService from "@/services/BackendService";
import MultipleAutoComplete from "@/components/MultipleAutoComplete.vue";
import { useToast } from "buefy";
import { useStorage } from "@vueuse/core";

const Toast = useToast();

const lakes: Ref<Lake[]> = ref([]);
const species: Ref<Specie[]> = ref([]);
const speciesPerLake: Ref<any> = ref({});
const speciesPerLakeAliases: Ref<any> = ref({});
const speciesPerLakeAbsent: Ref<any> = ref({});
const lakeSelectionOptions: Ref<any[]> = ref([]);
const lastLakeSelection = useStorage(
  "lastLakeSelection",
  [],
);
const selectedLakes: Ref<Lake[]> = ref([]);
const maxLakeBeforeShowingAutoComplete = 5;

Promise.all([
  BackendService.backendGet("/v1/referential/lakes"),
  BackendService.backendGet("/v1/referential/species"),
  BackendService.backendGet("/v1/referential/species-per-lake"),
]).then((data) => {
  lakes.value = data[0];
  species.value = data[1];
  speciesPerLake.value = data[2];
  lakeSelectionOptions.value = [];
  lakes.value.forEach((l) => {
    lakeSelectionOptions.value.push({ id: l.id, label: l.name });
  });
  if (lakes.value.length < maxLakeBeforeShowingAutoComplete) {
    selectedLakes.value = lakes.value;
  }
  referentialLoaded();
});

function referentialLoaded() {
  lakes.value.forEach((l) => {
    speciesPerLakeAliases.value[l.id] = {};
  });

  Object.keys(speciesPerLake.value).forEach((lakeId) => {
    const items = speciesPerLake.value[lakeId];
    items.forEach((spl) => {
      if (!spl.present) {
        if (!speciesPerLakeAbsent.value[lakeId]) {
          speciesPerLakeAbsent.value[lakeId] = [spl.id];
        } else {
          speciesPerLakeAbsent.value[lakeId].push(spl.id);
        }
      } else if (spl.alias) {
        speciesPerLakeAliases.value[lakeId][spl.id] = spl.alias;
      }
    });
  });
}

async function save() {
  try {
    await BackendService.backendPut("/v1/referential/species-aliases-per-lake", {
      targetLakes: selectedLakes.value.map((l) => l.id),
      speciesPerLakeAliases: speciesPerLakeAliases.value,
      speciesPerLakeAbsent: speciesPerLakeAbsent.value,
    });
    Toast.open({
      message: "Espèces par plan d'eau enregistrées",
      type: "is-success",
    });
  } catch (error: any) {
    Toast.open({
      message:
        "Erreur lors de l'enregistrement des espèces par plan d'eau : " +
        error.message,
      type: "is-danger",
    });
  }
}

function changeLakeSelection(newSelectedLakeIds: string[]) {
  selectedLakes.value = lakes.value.filter(
    (l) => newSelectedLakeIds.indexOf(l.id) > -1
  );
  localStorage.setItem(
    "lastLakeSelection",
    JSON.stringify(newSelectedLakeIds)
  );
}

function toggleAbsent(lakeId: string, specieId: string) {
  if (speciesPerLakeAbsent.value[lakeId]) {
    const index = speciesPerLakeAbsent.value[lakeId].indexOf(specieId);
    if (index === -1) {
      speciesPerLakeAbsent.value[lakeId].push(specieId);
    } else {
      speciesPerLakeAbsent.value[lakeId].splice(index, 1);
      speciesPerLakeAliases.value[lakeId][specieId] = undefined;
    }
  } else {
    speciesPerLakeAbsent.value[lakeId] = [specieId];
  }
  const instance = getCurrentInstance();
  instance?.proxy?.$forceUpdate();
}
</script>

<style scoped lang="less">
.species-per-lake {
  .table {
    width: 100%;
  }

  .buttons {
    width: 100%;
    display: flex;
    flex-direction: row-reverse;
    padding-right: 30px;
    padding-top: 10px;
  }
}
</style>
