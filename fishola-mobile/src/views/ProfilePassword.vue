<template>
  <div class="profile-pasword page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="profile-pasword-page page">
      <div class="pane">
        <h1>Mot de passe</h1>
        <div class="pane-content">
          Work in progress
          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter button-text="Enregistrer"
                   v-on:buttonClicked="save"
                   shortcuts="back,spacer,spacer"/>
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue';
import FisholaFooter from '@/components/layout/FisholaFooter.vue';

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../router';

@Component({
  components: {
    FisholaHeader,
    FisholaFooter
  }
})
export default class ProfilePasswordVue extends Vue {

  created() {
  }

  mounted() {
  }

  save() {
    this.$root.$emit('toaster-warning', 'Work in progress')
  }

}

</script>

<style lang="less">

@import "../less/main";

.profile-password-page {

}

</style>
