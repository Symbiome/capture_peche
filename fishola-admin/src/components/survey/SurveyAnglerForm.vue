<!--
  #%L
  Carnet de pêche :: Admin
  %%
  Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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
  <div class="box angler-box">
    <div class="angler-header">
      <strong>Pêcheur {{ index + 1 }}</strong>
      <b-button type="is-danger" size="is-small" icon-left="delete" :disabled="!removable" @click="emit('remove')">
        Retirer
      </b-button>
    </div>

    <div class="columns is-multiline">
      <b-field label="Département / pays d'origine" class="column is-4">
        <b-input v-model="angler.origin" placeholder="ex. 69, Suisse…"></b-input>
      </b-field>
      <b-field label="Mode de pêche" class="column is-4">
        <b-select v-model="angler.fishingMode" expanded>
          <option v-for="m in FISHING_MODES" :key="m" :value="m">{{ m }}</option>
        </b-select>
      </b-field>
      <b-field label="Technique" class="column is-4">
        <ReferentialSelect v-model="angler.techniqueId" :referential="techniques" expanded />
      </b-field>

      <b-field label="Nombre de lignes" class="column is-3">
        <b-input type="number" min="1" v-model.number="angler.rodCount"></b-input>
      </b-field>
      <b-field label="Appât / type de leurre" class="column is-4">
        <b-input v-model="angler.baitOrLure"></b-input>
      </b-field>
      <b-field label="Espèce recherchée" class="column is-5">
        <ExpectedSpeciesSelect v-model="angler.expectedSpecies" :species="species" />
      </b-field>

      <b-field class="column is-12">
        <b-switch v-model="angler.bredouille">Bredouille (aucune capture)</b-switch>
      </b-field>
    </div>

    <SurveyCatchList v-if="!angler.bredouille" :captures="angler.captures" :species="species" />

    <div class="souvenir-toggle">
      <b-switch v-model="angler.hasSouvenir">Session souvenir (dernière sortie passée du pêcheur)</b-switch>
    </div>

    <div v-if="angler.hasSouvenir" class="box souvenir-box">
      <div class="columns is-multiline">
        <b-field label="Date de la sortie" class="column is-3">
          <input type="date" class="input" v-model="angler.souvenir.day" :max="todayIso" />
        </b-field>
        <b-field label="Période de la journée" class="column is-3">
          <b-select v-model="angler.souvenir.dayPeriod" expanded>
            <option v-for="p in DAY_PERIODS" :key="p" :value="p">{{ p }}</option>
          </b-select>
        </b-field>
        <b-field label="Site pêché" class="column is-6">
          <WaterEntitySearchSelect v-model="angler.souvenir.waterEntityId" v-model:position="angler.souvenir.position"
            with-map with-favorites />
        </b-field>

        <b-field label="Mode de pêche" class="column is-4">
          <b-select v-model="angler.souvenir.fishingMode" expanded>
            <option v-for="m in FISHING_MODES" :key="m" :value="m">{{ m }}</option>
          </b-select>
        </b-field>
        <b-field label="Technique" class="column is-4">
          <ReferentialSelect v-model="angler.souvenir.techniqueId" :referential="techniques" expanded />
        </b-field>
        <b-field label="Nombre de lignes" class="column is-4">
          <b-input type="number" min="1" v-model.number="angler.souvenir.rodCount"></b-input>
        </b-field>

        <b-field label="Appât / type de leurre" class="column is-6">
          <b-input v-model="angler.souvenir.baitOrLure"></b-input>
        </b-field>
        <b-field label="Espèce recherchée" class="column is-6">
          <ExpectedSpeciesSelect v-model="angler.souvenir.expectedSpecies" :species="species" />
        </b-field>

        <b-field class="column is-12">
          <b-switch v-model="angler.souvenir.bredouille">Bredouille (aucune capture)</b-switch>
        </b-field>
      </div>

      <SurveyCatchList v-if="!angler.souvenir.bredouille" :captures="angler.souvenir.captures" :species="species" />
    </div>
  </div>
</template>

<script setup lang="ts">
import ExpectedSpeciesSelect from "@/components/survey/ExpectedSpeciesSelect.vue";
import ReferentialSelect from "@/components/ReferentialSelect.vue";
import SurveyCatchList from "@/components/survey/SurveyCatchList.vue";
import WaterEntitySearchSelect from "@/components/WaterEntitySearchSelect.vue";
import { DAY_PERIODS, FISHING_MODES } from "@/components/survey/surveyForm";
import type { EntryReferential } from "@/utils/entryReferentials";

interface Props {
  /** Pêcheur interrogé (cf. newAngler), modifié sur place. */
  angler: any;
  index: number;
  removable: boolean;
  species: EntryReferential;
  techniques: EntryReferential;
  todayIso: string;
}

defineProps<Props>();

const emit = defineEmits<{
  (e: "remove"): void;
}>();
</script>

<style scoped lang="less">
.angler-box {
  border: 1px solid #e2e8ec;
}
.angler-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.souvenir-toggle {
  margin-top: 1rem;
  margin-bottom: 0.5rem;
}
.souvenir-box {
  box-shadow: none;
  border: 1px dashed #cbd5db;
}
</style>
