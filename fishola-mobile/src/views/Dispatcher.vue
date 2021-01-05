<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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
  <div class="dispatcher full-background">
    <div class="spinner">&nbsp;</div>
  </div>
</template>

<script lang="ts">

import Constants from '@/services/Constants';

import router from '@/router'

import ProfileService from '@/services/ProfileService';
import Helpers from '@/services/Helpers';

import { Component, Prop, Vue } from 'vue-property-decorator';
import { Plugins, AppState, StatusBarStyle } from '@capacitor/core';
const { SplashScreen, StatusBar } = Plugins;

@Component({
  components: {}
})
export default class DispatcherView extends Vue {

  constructor() {
    super();
  }

  mounted() {
    this.checkForActiveSession();
  }

  checkForActiveSession() {
    ProfileService.getProfile()
      .then(
        (profile) => {
          if (profile.offlineMarker) {
            this.$root.$emit('toaster-warning', 'Pas de connexion internet');
          } else {
            this.$root.$emit('toaster-success', 'Vous êtes toujours connecté\u00B7e');
          }
          router.push('trips');
          SplashScreen.hide();
          StatusBar.show();
        },
        (status) => {
          // Only push route if no route has already been pushed (typically when opening from external url)
          if (router.currentRoute.name == "dispatcher") {

            // En fonction de la plateforme on va rediriger vers la page d'accueil ou la page de login
            Helpers.getDeviceType()
              .then(type => {
                if (type == "web") {
                  router.push('/about');
                } else {
                  router.push('/login');
                }
              });

          }
          SplashScreen.hide();
          StatusBar.show();
        }
      );
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.dispatcher {

  height: 100%;

  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  @keyframes spin { 100% { -webkit-transform: rotate(360deg); transform:rotate(360deg); } }

  .spinner {
    height: 60px;
    width: 60px;
    border-radius: 50%;
    border-top: 3px solid @white;
    border-left: 3px solid @white;
    animation:spin 2s linear infinite;
  }

}

</style>
