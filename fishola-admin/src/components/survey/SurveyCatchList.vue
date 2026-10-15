<!--
  #%L
  Fishola :: Admin
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
  <div>
    <h3 class="capture-title">Captures</h3>
    <div v-for="(c, j) in captures" :key="j" class="box capture-box">
      <div class="columns is-multiline is-vcentered">
        <b-field label="Espèce" class="column is-3">
          <ReferentialSelect v-model="c.speciesId" :referential="species" expanded />
        </b-field>
        <b-field label="Nombre" class="column is-2">
          <b-input type="number" min="1" v-model.number="c.quantity"></b-input>
        </b-field>

        <template v-if="isLot(c)">
          <b-field label="Taille min (cm)" class="column is-2">
            <b-input type="number" min="0" v-model.number="c.lotMinSize"></b-input>
          </b-field>
          <b-field label="Taille max (cm)" class="column is-2">
            <b-input type="number" min="0" v-model.number="c.lotMaxSize"></b-input>
          </b-field>
        </template>
        <b-field v-else label="Taille (cm)" class="column is-2">
          <b-input type="number" min="0" v-model.number="c.size"></b-input>
        </b-field>

        <b-field label="Conservé" class="column is-2">
          <b-switch v-model="c.kept"></b-switch>
        </b-field>
        <div class="column is-1">
          <b-button type="is-danger" icon-left="delete" :disabled="captures.length <= 1"
            @click="captures.splice(j, 1)"></b-button>
        </div>
      </div>
    </div>
    <!-- Mis en valeur (#206) ; contour seul pour rester distinct d'« Enregistrer ». -->
    <b-button type="is-primary" size="is-medium" outlined icon-left="plus" @click="captures.push(newCapture())">
      Ajouter une capture
    </b-button>
  </div>
</template>

<script setup lang="ts">
import ReferentialSelect from "@/components/ReferentialSelect.vue";
import { isLot, newCapture } from "@/components/survey/surveyForm";
import type { EntryReferential } from "@/utils/entryReferentials";

interface Props {
  /** Captures du pêcheur ou du souvenir, modifiées sur place. */
  captures: any[];
  species: EntryReferential;
}

defineProps<Props>();
</script>

<style scoped lang="less">
.capture-title {
  font-weight: 600;
  margin-bottom: 0.5rem;
}
.capture-box {
  box-shadow: none;
  border: 1px solid #eef1f4;
}
</style>
