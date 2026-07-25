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
  <div class="home">
    <!-- Opérateur : accès direct à ses deux actions. -->
    <div v-if="loggedAdmin.isOperator" class="operator-home">
      <h1 class="title">Espace opérateur</h1>
      <p class="subtitle is-6" v-if="perimeterNames.length">
        Périmètre : <strong>{{ perimeterNames.join(', ') }}</strong>
      </p>

      <div class="action-cards">
        <router-link :to="{ name: 'operator-import' }" class="action-card">
          <b-icon icon="upload" size="is-large"></b-icon>
          <h2>Import CSV</h2>
          <p>Importer un fichier de sessions au format officiel.</p>
        </router-link>
        <router-link :to="{ name: 'operator-manual-entry' }" class="action-card">
          <b-icon icon="playlist-plus" size="is-large"></b-icon>
          <h2>Nouvelle saisie</h2>
          <p>Saisir une sortie et ses captures à la main.</p>
        </router-link>
      </div>
    </div>

    <!-- Administrateurs : accueil classique. -->
    <div v-else class="admin-home">
      <p>Bienvenue sur l'interface d'administration de FISHOLA.</p>
      <p>Vous pouvez utiliser le menu pour accéder aux différentes sections.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import { computed, onMounted, ref } from "vue";

defineOptions({ name: "Home" });

const loggedAdmin = ref<any>({});
const lakes = ref<any[]>([]);

const perimeterNames = computed(() => {
  const ids: string[] = loggedAdmin.value.waterEntityIds ?? [];
  const byId = new Map(lakes.value.map((l) => [l.id, l.name]));
  return ids.map((id) => byId.get(id)).filter((n): n is string => !!n);
});

onMounted(async () => {
  try {
    loggedAdmin.value = await BackendService.backendGet("/v1/admin/check");
    if (loggedAdmin.value.isOperator) {
      lakes.value = await BackendService.backendGet("/v1/referential/waterEntities");
    }
  } catch (e) {
    // Le dispatcher / le menu gèrent la redirection si la session est expirée.
  }
});
</script>

<style scoped lang="less">
.home {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.admin-home {
  text-align: center;
  p {
    font-size: 22px;
    margin: 5px;
  }
}

.operator-home {
  width: 100%;
  max-width: 780px;
  text-align: center;

  .title {
    margin-bottom: 0.25rem;
  }
  .subtitle {
    color: #666;
    margin-bottom: 2rem;
  }
}

.action-cards {
  display: flex;
  gap: 1.5rem;
  justify-content: center;
  flex-wrap: wrap;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  width: 260px;
  padding: 2rem 1.5rem;
  border: 1px solid #e2e8ec;
  border-radius: 12px;
  background: #fff;
  color: inherit;
  text-decoration: none;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: transform 0.12s ease, box-shadow 0.12s ease, border-color 0.12s ease;

  h2 {
    font-size: 1.25rem;
    font-weight: 600;
    margin: 0;
  }
  p {
    color: #777;
    font-size: 0.9rem;
    margin: 0;
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.1);
    border-color: #1fb6a8;
  }
}
</style>
