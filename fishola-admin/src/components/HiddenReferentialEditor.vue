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
  <div>
    <div class="toolbar">
      <b-input v-model="search" icon="magnify" placeholder="Rechercher…" class="search"></b-input>
      <div class="buttons">
        <b-button size="is-small" icon-left="eye" :disabled="!filtered.length" @click="setShown(filtered, true)">
          Tout afficher
        </b-button>
        <b-button size="is-small" icon-left="eye-off" :disabled="!filtered.length" @click="setShown(filtered, false)">
          Tout masquer
        </b-button>
      </div>
    </div>
    <p class="summary">
      {{ shownCount }} {{ itemLabel }} affichées sur {{ items.length }}<span v-if="search">
        — {{ filtered.length }} correspondant à la recherche, concernées par « Tout afficher » / « Tout masquer »</span>.
    </p>

    <b-table :data="filtered" :loading="loading" striped narrowed>
      <b-table-column v-slot="{ row }" label="Affiché" width="110">
        <b-switch :model-value="!hidden.has(row.id)" @update:model-value="setShown([row], $event)" />
      </b-table-column>
      <b-table-column v-slot="{ row }" label="Nom">
        <span :class="{ 'is-hidden-item': hidden.has(row.id) }">{{ row.name }}</span>
      </b-table-column>
      <template #empty>Aucun élément</template>
    </b-table>

    <div class="mt-4">
      <b-button type="is-primary" icon-left="content-save" :loading="saving" :disabled="!dirty" @click="save">
        Enregistrer
      </b-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import type { ReferentialItem } from "@/utils/entryReferentials";
import { normalizeSearchText } from "@/utils/utils";
import { useToast } from "buefy";
import { computed, onMounted, ref } from "vue";

interface Props {
  /** Référentiel masquable côté backend : « species » ou « technique ». */
  type: string;
  /** Listing national du référentiel. */
  uri: string;
  /** Libellé pluriel féminin des éléments, ex. « espèces ». */
  itemLabel: string;
}

const props = defineProps<Props>();

const Toast = useToast();

const items = ref<ReferentialItem[]>([]);
// Masqués du compte, y compris ceux absents de la liste (archivés) : renvoyés tels quels.
const hidden = ref<Set<string>>(new Set());
const savedHidden = ref<Set<string>>(new Set());
const search = ref("");
const loading = ref(true);
const saving = ref(false);

const hiddenUri = computed(() => "/v1/admin/hidden-referentials/" + props.type);

const filtered = computed(() => {
  const term = normalizeSearchText(search.value);
  return term ? items.value.filter((item) => normalizeSearchText(item.name).includes(term)) : items.value;
});

const shownCount = computed(() => items.value.filter((item) => !hidden.value.has(item.id)).length);

const dirty = computed(() => hidden.value.size !== savedHidden.value.size
  || [...hidden.value].some((id) => !savedHidden.value.has(id)));

onMounted(load);

/** Référentiel national (hors archivés) et éléments masqués du compte connecté. */
async function load() {
  loading.value = true;
  try {
    const [all, hiddenIds] = await Promise.all([
      BackendService.backendGet(props.uri),
      BackendService.backendGet(hiddenUri.value)
    ]);
    items.value = all
      .filter((item: any) => !item.archived)
      .sort((a: ReferentialItem, b: ReferentialItem) => a.name.localeCompare(b.name, "fr"));
    hidden.value = new Set(hiddenIds);
    savedHidden.value = new Set(hiddenIds);
  } finally {
    loading.value = false;
  }
}

/** Affiche ou masque des éléments, sans enregistrer. */
function setShown(targets: ReferentialItem[], shown: boolean) {
  const next = new Set(hidden.value);
  targets.forEach((item) => (shown ? next.delete(item.id) : next.add(item.id)));
  hidden.value = next;
}

/** Enregistre l'ensemble des éléments masqués du référentiel. */
async function save() {
  saving.value = true;
  try {
    await BackendService.backendPut(hiddenUri.value, [...hidden.value]);
    savedHidden.value = new Set(hidden.value);
    Toast.open({ message: "Choix d'affichage enregistrés", type: "is-success" });
  } catch (error: any) {
    Toast.open({ message: "Erreur lors de l'enregistrement : " + error.message, type: "is-danger" });
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped lang="less">
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}
.search {
  flex: 1 1 18rem;
  max-width: 28rem;
}
.summary {
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}
.is-hidden-item {
  opacity: 0.55;
}
</style>
