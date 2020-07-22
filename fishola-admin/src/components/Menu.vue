<template>
  <b-navbar>
      <template slot="brand">
          <b-navbar-item tag="router-link" :to="{ path: '/' }">
              <img
                  class="logo"
                  src="/img/logo-ligne-positif.svg"
                  alt="FISHOLA"
              >
          </b-navbar-item>
      </template>
      <template slot="start">
          <b-navbar-dropdown label="Référentiels">
              <b-navbar-item tag="router-link" :to="{ name: 'lakes' }">
                  Lacs
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
              <!-- <b-navbar-item href="#">
                  État relâché
              </b-navbar-item> -->
              <!-- <b-navbar-item href="#">
                  Types de prélèvement
              </b-navbar-item> -->
          </b-navbar-dropdown>
          <b-navbar-dropdown label="Paramétrage">
              <b-navbar-item tag="router-link" :to="{ name: 'species-per-lake' }">
                  Espèces par lac
              </b-navbar-item>
              <b-navbar-item tag="router-link" :to="{ name: 'authorized-samples' }">
                  Autorisations de prélèvement
              </b-navbar-item>
          </b-navbar-dropdown>
          <b-navbar-dropdown label="Documentations">
              <b-navbar-item tag="router-link" :to="{ name: 'editorial-pages' }">
                  Pages éditoriales
              </b-navbar-item>
              <b-navbar-item tag="router-link" :to="{ name: 'documentation' }">
                  En téléchargement
              </b-navbar-item>
          </b-navbar-dropdown>
          <b-navbar-item tag="router-link" :to="{ name: 'trips' }">
              Sorties
          </b-navbar-item>
          <b-navbar-item tag="router-link" :to="{ name: 'users' }">
              Utilisateurs
          </b-navbar-item>
      </template>

      <template slot="end">
          <b-navbar-item tag="div">
              <div class="buttons">
                  <b-button type="is-danger" outlined @click="doLogout()">
                    Déconnexion
                  </b-button>
              </div>
          </b-navbar-item>
      </template>
  </b-navbar>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator'

import router from '@/router'

import BackendService from '@/services/BackendService.ts';

@Component
export default class Menu extends Vue {

    mounted() {
        BackendService.backendGet("/v1/security/admin-check")
            .then(
                () => {
                    // Rien à faire
                },
                (error) => {
                    this.$buefy.toast.open({
                        message: 'Vous n\'êtes plus connecté\u00B7e',
                        type: 'is-danger'
                    });
                    router.push("/login");
                });
    }
    doLogout() {
        BackendService.backendPost("/v1/security/admin-logout")
            .then(
                () => {
                    router.push("/login");
                });
    }
}
</script>

<style lang="less">

@import "../less/main";

  .logo {
    height: 52px;
  }

  a.navbar-item:focus, a.navbar-item:focus-within, a.navbar-item:hover, a.navbar-item.is-active, .navbar-link:focus, .navbar-link:focus-within, .navbar-link:hover, .navbar-link.is-active {
    color: @pelorous !important;
  }
  .navbar-link:not(.is-arrowless)::after {
      border-color: @pelorous !important;
  }

  .buttons {
      margin-right: 10px;
  }

</style>
