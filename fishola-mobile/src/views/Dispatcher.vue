<template>
  <div class="dispatcher full-background">
    <div class="spinner">&nbsp;</div>
  </div>
</template>

<script lang="ts">

import Constants from '@/services/Constants';

import router from '@/router'

import ProfileService from '@/services/ProfileService';
import KeyboardManager from '@/services/KeyboardManager';

import { Component, Prop, Vue } from 'vue-property-decorator';

import { Plugins, StatusBarStyle } from '@capacitor/core';
const { SplashScreen, StatusBar} = Plugins;

@Component({
  components: {}
})
export default class DispatcherView extends Vue {

  constructor() {
    super();
  }

  mounted() {
    this.checkForActiveSession();
    KeyboardManager.setupKeyboardConfiguration();
    StatusBar.setBackgroundColor({"color": "#1E9BC4"});
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
          // Reveal status bar on iOS
          StatusBar.show();
        },
        (status) => {
          router.push('/login');
          SplashScreen.hide();
          // Reveal status bar on iOS
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
