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
  <b-navbar>
    <template v-slot:brand>
      <b-navbar-item tag="router-link" :to="{ path: '/' }">
        <img class="logo" src="/img/logo-ligne-positif.svg" alt="FISHOLA"/>
      </b-navbar-item>
    </template>
    <template v-slot:start>
      <b-navbar-dropdown label="Référentiels" v-if="loggedAdmin.isNationalAdmin">
        <b-navbar-item tag="router-link" :to="{ name: 'lakes' }">
          Plans d'eau
        </b-navbar-item>
        <b-navbar-item tag="router-link" :to="{ name: 'species' }">
          Espèces
        </b-navbar-item>
        <b-navbar-item tag="router-link" :to="{ name: 'weathers' }">
          Météo
        </b-navbar-item>
        <b-navbar-item tag="router-link" :to="{ name: 'techniques' }">
          Techniques de pêche
        </b-navbar-item>
      </b-navbar-dropdown>
      <b-navbar-dropdown label="Paramétrage">
        <b-navbar-item tag="router-link" :to="{ name: 'species-per-lake' }">
          Espèces par plan d'eau
        </b-navbar-item>
        <b-navbar-item tag="router-link" :to="{ name: 'authorized-samples' }">
          Maillages et tailles maximales
        </b-navbar-item>
      </b-navbar-dropdown>
      <b-navbar-dropdown label="Documentations" v-if="loggedAdmin.isNationalAdmin">
        <b-navbar-item tag="router-link" :to="{ name: 'editorial-pages' }">
          Pages éditoriales
        </b-navbar-item>
        <b-navbar-item tag="router-link" :to="{ name: 'documentation' }">
          En téléchargement
        </b-navbar-item>
        <b-navbar-item tag="router-link" :to="{ name: 'news' }">
          Communications
        </b-navbar-item>
      </b-navbar-dropdown>
      <b-navbar-item tag="router-link" :to="{ name: 'news' }" v-else>
        Communications
      </b-navbar-item>
      <b-navbar-item tag="router-link" :to="{ name: 'metrics' }">
        Chiffres Clés
      </b-navbar-item>
      <b-navbar-item tag="router-link" :to="{ name: 'trips' }" v-if="loggedAdmin.isNationalAdmin">
        Sorties
      </b-navbar-item>
      <b-navbar-item tag="router-link" :to="{ name: 'users' }" v-if="loggedAdmin.isNationalAdmin">
        Utilisateurs
      </b-navbar-item>
      <b-navbar-item tag="router-link" :to="{ name: 'admins' }" v-if="loggedAdmin.canCreateAdmins">
        Administrateurs
      </b-navbar-item>
    </template>

    <template v-slot:end>
      <b-navbar-item tag="router-link" :to="{ name: 'help' }" class="help-link">
        <b-icon icon="help-circle" size="is-medium"></b-icon>
        Aide
      </b-navbar-item>
      <b-navbar-item class="user-dropdown-wrapper">
        <b-dropdown
          class="is-right"
        >
          <template #trigger>
            <div class="logged-admin">
              <b-icon icon="account-circle" size="is-medium"></b-icon>
              <span class="username">{{ loggedAdmin.email.split('@')[0] }}</span>
              <b-icon icon="menu-down" size="is-small"></b-icon>
            </div>
          </template>

          <!-- Items -->
          <b-dropdown-item>
            <div class="logout-item">

              <b v-if="loggedAdmin.isNationalAdmin">Admninistrateur National</b>
              <b v-else-if="lakes.length == 1">
                Admnistateur du {{ lakes[0].name }}
              </b>
              <span v-else>
                  <b>Administrateur  des plans d'eau : </b><br/>
                  <p v-for="l in lakes" :id="l.id">
                    - {{ l.name }}
                  </p>
                </span>
            </div>
          </b-dropdown-item>
          <b-dropdown-item has-link>
            <router-link :to="{ name: 'change-password' }">
              <b-icon icon="lock-reset" size="is-small"></b-icon>&nbsp;Changer mon mot de passe
            </router-link>
          </b-dropdown-item>
          <b-dropdown-item>
            <div class="logout-item">
              <b-button
                class="logout-button"
                type="is-danger"
                size="is-small"
                outlined
                @click="doLogout()"
              >

                <b-icon icon="logout" size="is-small"></b-icon>
                Déconnexion
              </b-button>
            </div>
          </b-dropdown-item>
        </b-dropdown>
      </b-navbar-item>
      <div class="user-dropdown-wrapper-responsive">
        <b-button
          class="logout-button"
          type="is-danger"
          size="is-small"
          outlined
          @click="doLogout()"
        >

          <b-icon icon="logout" size="is-small"></b-icon>
          Se déconnecter du compte {{ loggedAdmin.email }}
        </b-button>
      </div>
    </template>
  </b-navbar>
</template>

<script setup lang="ts">
import router from "@/router";

import BackendService from "@/services/BackendService";
import {BButton, BDropdown, BDropdownItem, BIcon, BNavbar, BNavbarDropdown, BNavbarItem, useToast} from "buefy";
import {onMounted, ref, Ref} from "vue";

const loggedAdmin: Ref<Admin> = ref({ email: "" });
const lakes: Ref<Lake[]> = ref([]);

const Toast = useToast();

onMounted(async () => {
  lakes.value = await BackendService.backendGet("/v1/referential/waterEntities");

  try {
    loggedAdmin.value = await BackendService.backendGet("/v1/admin/check");
  } catch (error) {
    Toast.open({
      message: "Vous n'êtes plus connecté\u00B7e",
      type: "is-danger"
    });
    router.push("/login");
  }
});

async function doLogout() {
  await BackendService.backendPost("/v1/admin/logout");
  router.push("/login");
}
</script>

<style lang="less">

.logo {
  height: 52px;
}

.navbar {
  position: sticky;
  top: 0;
  box-shadow: 0 1px 20px 3px #1e9bc411;
}

a.navbar-item:focus,
a.navbar-item:focus-within,
a.navbar-item:hover,
a.navbar-item.is-active,
.navbar-link:focus,
.navbar-link:focus-within,
.navbar-link:hover,
.navbar-link.is-active {
  color: @pelorous !important;
}

.navbar-link:not(.is-arrowless)::after {
  border-color: @pelorous !important;
}

.buttons {
  margin-right: 10px;
}

.logged-admin {
  align-items: center;
  justify-content: center;
  display: flex;
  gap: 10px;
  cursor: pointer;

  &:hover {
    color: @pelorous;
  }
}

.user-dropdown-wrapper-responsive {
  display: none;
}

.help-link {
  display: flex;
  gap: 10px;
}

@media (max-width: 1024px) {
  .user-dropdown-wrapper,
  .help-link .icon {
    display: none;
  }

  .user-dropdown-wrapper-responsive {
    display: block;
  }
}
</style>
