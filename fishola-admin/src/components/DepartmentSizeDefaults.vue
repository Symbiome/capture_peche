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
<!--
  Maillages et tailles maximales par défaut d'un département (#246) : ils
  s'appliquent à toutes les entités hydrographiques du département, sauf
  valeur spécifique saisie sur l'entité (matrice d'AuthorizedSamples.vue).
  Émet `changed` avec les valeurs enregistrées, indexées par espèce.
  -->
<template>
  <div class="department-size-defaults box">
    <h2 class="subtitle">Valeurs par défaut du département</h2>
    <p class="help-text">
      Ces valeurs s'appliquent à tous les cours d'eau et plans d'eau du département.
      Une valeur saisie sur un milieu dans le tableau ci-dessous reste prioritaire.
    </p>

    <table class="table is-narrow" v-if="rows.length > 0">
      <thead>
        <tr>
          <th>Espèce</th>
          <th>Taille maximale (cm)</th>
          <th>Maillage (cm)</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.speciesId">
          <td>{{ speciesName(row.speciesId) }}</td>
          <td>
            <b-input type="number" size="is-small" min="1" v-model="row.maxSize" />
          </td>
          <td>
            <b-input type="number" size="is-small" placeholder="Non défini" v-model="row.meshSize" />
          </td>
          <td>
            <span v-if="rowError(row)" class="error">{{ rowError(row) }}</span>
            <b-button size="is-small" type="is-text" @click="removeRow(row.speciesId)">Retirer</b-button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else><i>Aucune valeur par défaut pour ce département.</i></p>

    <div class="add-row">
      <b-select v-model="speciesToAdd" placeholder="Ajouter une espèce" size="is-small">
        <option v-for="s in availableSpecies()" :key="s.id" :value="s.id">{{ s.name }}</option>
      </b-select>
      <b-button size="is-small" :disabled="!speciesToAdd" @click="addRow">Ajouter</b-button>
      <b-button size="is-small" type="is-primary" @click="save">Enregistrer les valeurs du département</b-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, Ref, watch } from "vue";
import { useToast } from "buefy";

import BackendService from "@/services/BackendService";

export interface DepartmentSizeDefault {
  speciesId: string;
  maxSize: number | string;
  meshSize: number | string | null;
}

const DEFAULT_MAX_SIZE = 60;
const DEFAULT_MESH_SIZE = 10;

const props = defineProps<{ department: string; species: Specie[] }>();
const emit = defineEmits<{ (e: "changed", defaults: Record<string, DepartmentSizeDefault>): void }>();

const Toast = useToast();
const rows: Ref<DepartmentSizeDefault[]> = ref([]);
const speciesToAdd: Ref<string | null> = ref(null);

watch(() => props.department, load, { immediate: true });

async function load() {
  rows.value = [];
  const requestedDepartment = props.department;
  const defaults: DepartmentSizeDefault[] = await BackendService.backendGet(
    "/v1/referential/authorized-samples/department/" + encodeURIComponent(requestedDepartment)
  );
  if (requestedDepartment !== props.department) {
    return;
  }
  rows.value = defaults.map(d => ({ ...d, meshSize: d.meshSize ?? "" }));
  emitChanged();
}

function emitChanged() {
  const bySpecies: Record<string, DepartmentSizeDefault> = {};
  rows.value.forEach(row => (bySpecies[row.speciesId] = { ...row }));
  emit("changed", bySpecies);
}

function speciesName(speciesId: string): string {
  return props.species.find(s => s.id === speciesId)?.name ?? speciesId;
}

function availableSpecies(): Specie[] {
  const used = new Set(rows.value.map(r => r.speciesId));
  return props.species.filter(s => !used.has(s.id));
}

function addRow() {
  if (!speciesToAdd.value) {
    return;
  }
  rows.value.push({ speciesId: speciesToAdd.value, maxSize: DEFAULT_MAX_SIZE, meshSize: DEFAULT_MESH_SIZE });
  speciesToAdd.value = null;
}

function removeRow(speciesId: string) {
  rows.value = rows.value.filter(r => r.speciesId !== speciesId);
}

function rowError(row: DepartmentSizeDefault): string | null {
  if (row.maxSize === "" || Number(row.maxSize) <= 0) {
    return "Taille maximale obligatoire.";
  }
  if (row.meshSize !== "" && row.meshSize !== null && Number(row.meshSize) < 0) {
    return "Maillage négatif.";
  }
  return null;
}

async function save() {
  if (rows.value.some(row => rowError(row) !== null)) {
    Toast.open({ message: "Corrigez les valeurs en erreur avant d'enregistrer.", type: "is-danger" });
    return;
  }
  const payload = rows.value.map(row => ({
    speciesId: row.speciesId,
    maxSize: Number(row.maxSize),
    meshSize: row.meshSize === "" || row.meshSize === null ? null : Number(row.meshSize)
  }));
  try {
    await BackendService.backendPut(
      "/v1/referential/authorized-samples/department/" + encodeURIComponent(props.department),
      payload
    );
    Toast.open({ message: "Valeurs du département enregistrées", type: "is-success" });
    emitChanged();
  } catch (error: any) {
    Toast.open({
      message: "Erreur lors de l'enregistrement des valeurs du département : " + error.message,
      type: "is-danger"
    });
  }
}
</script>

<style scoped lang="less">
.department-size-defaults {
  margin-bottom: 20px;

  .help-text {
    margin-bottom: 10px;
  }

  .add-row {
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .error {
    color: red;
    font-weight: bold;
    margin-right: 5px;
  }
}
</style>
