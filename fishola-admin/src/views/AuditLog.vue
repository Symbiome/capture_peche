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
  <div class="audit-log">
    <h1 class="title">Journal d'audit</h1>
    <p class="subtitle is-6">
      Traçabilité des actions de gestion réalisées dans le back-office (imputabilité RGPD).
    </p>

    <div class="filters">
      <b-field label="Type d'acteur" horizontal>
        <b-select v-model="actorType" @input="reload">
          <option value="">Tous</option>
          <option value="admin">Administrateur / opérateur</option>
          <option value="user">Pêcheur</option>
          <option value="system">Système</option>
        </b-select>
      </b-field>
      <b-field label="Action" horizontal>
        <b-input
          v-model="action"
          placeholder="ex. admin.update, operator.create, import.csv…"
          @keyup.native.enter="reload"
        />
        <p class="control">
          <b-button type="is-primary" icon-left="filter" @click="reload">Filtrer</b-button>
        </p>
      </b-field>
    </div>

    <b-table :data="entries" :loading="loading" hoverable narrowed>
      <b-table-column field="at" label="Date" v-slot="props">
        {{ formatDate(props.row.at) }}
      </b-table-column>
      <b-table-column field="action" label="Action" v-slot="props">
        <code>{{ props.row.action }}</code>
      </b-table-column>
      <b-table-column field="actor" label="Acteur" v-slot="props">
        <b-tag :type="actorTagType(props.row.actorType)">{{ actorLabel(props.row.actorType) }}</b-tag>
        <span class="mono" :title="props.row.actorId">{{ shortId(props.row.actorId) }}</span>
      </b-table-column>
      <b-table-column field="entity" label="Entité" v-slot="props">
        <span v-if="props.row.entityType">
          {{ props.row.entityType }}
          <span class="mono" :title="props.row.entityId">{{ shortId(props.row.entityId) }}</span>
        </span>
        <span v-else class="muted">—</span>
      </b-table-column>
      <b-table-column field="details" label="Requête" v-slot="props">
        <span v-if="props.row.details && props.row.details.method">
          <span class="mono">{{ props.row.details.method }} {{ props.row.details.path }}</span>
          <b-tag v-if="props.row.details.httpStatus" :type="statusTagType(props.row.details.httpStatus)">
            {{ props.row.details.httpStatus }}
          </b-tag>
        </span>
        <span v-else class="muted">—</span>
      </b-table-column>

      <template #empty>
        <div class="empty-state">
          {{ loading ? "Chargement…" : "Aucune entrée pour ces filtres." }}
        </div>
      </template>
    </b-table>

    <div class="pagination-bar">
      <b-button icon-left="chevron-left" :disabled="pageNumber === 0 || loading" @click="prev">
        Précédent
      </b-button>
      <span class="page-indicator">Page {{ pageNumber + 1 }}</span>
      <b-button
        icon-right="chevron-right"
        :disabled="entries.length < pageSize || loading"
        @click="next"
      >
        Suivant
      </b-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import { ref, onMounted } from "vue";

const entries = ref<any[]>([]);
const loading = ref(false);
const actorType = ref("");
const action = ref("");
const pageNumber = ref(0);
const pageSize = 50;

async function load() {
  loading.value = true;
  const params = new URLSearchParams();
  if (actorType.value) params.set("actorType", actorType.value);
  if (action.value.trim()) params.set("action", action.value.trim());
  params.set("pageNumber", String(pageNumber.value));
  params.set("pageSize", String(pageSize));
  try {
    entries.value = await BackendService.backendGet(`/v1/admin/audit-log?${params.toString()}`);
  } catch (e) {
    entries.value = [];
  } finally {
    loading.value = false;
  }
}

function reload() {
  pageNumber.value = 0;
  load();
}
function prev() {
  if (pageNumber.value > 0) {
    pageNumber.value--;
    load();
  }
}
function next() {
  pageNumber.value++;
  load();
}

function formatDate(at: number | string): string {
  if (at === null || at === undefined) return "";
  // Le backend sérialise `at` en secondes epoch (OffsetDateTime) — d'où le ×1000.
  const ms = typeof at === "number" ? at * 1000 : Date.parse(at);
  return new Date(ms).toLocaleString("fr-FR");
}
function actorLabel(t: string): string {
  return t === "admin" ? "Staff" : t === "user" ? "Pêcheur" : t === "system" ? "Système" : t;
}
function actorTagType(t: string): string {
  return t === "admin" ? "is-info" : t === "user" ? "is-success" : "is-light";
}
function statusTagType(status: number): string {
  if (status >= 500) return "is-danger";
  if (status >= 400) return "is-warning";
  return "is-success";
}
function shortId(id?: string): string {
  return id ? id.slice(0, 8) : "";
}

onMounted(load);
</script>

<style scoped lang="less">
.audit-log {
  .filters {
    margin-bottom: 1.2rem;
    max-width: 640px;
  }
  .mono {
    font-family: monospace;
    font-size: 0.85em;
    color: #666;
    margin-left: 0.4rem;
  }
  .muted {
    color: #aaa;
  }
  code {
    background: #f2f5f7;
    padding: 0.1rem 0.4rem;
    border-radius: 4px;
  }
  .empty-state {
    text-align: center;
    padding: 2rem;
    color: #888;
  }
  .pagination-bar {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-top: 1rem;
  }
  .page-indicator {
    color: #666;
  }
}
</style>
