<template>
  <div class="settings page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page settings-page">
      <div class="pane pane-only">
        <h1>Paramètres</h1>
        <div class="pane-content">

          <div class="settings-row" v-if="settings">
            <span>Renseigner le poids des captures</span>
            <FormToggle v-model="settings.promptWeight" />
          </div>

          <div class="settings-row" v-if="settings">
            <span>Effectuer des prélèvements</span>
            <FormToggle v-model="settings.promptSamples" />
          </div>

          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter shortcuts="back,settings,profile"
                   selected="settings" />
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import FormToggle from '@/components/common/FormToggle.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import {UserSettings} from '@/pojos/BackendPojos';
import ProfileService from '@/services/ProfileService';

import { Component, Prop, Watch, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    FormToggle,
    FisholaFooter
  }
})
export default class SettingsView extends Vue {

  settings:UserSettings | null = null;

  constructor() {
    super();
  }

  @Watch('settings', {deep: true})
  onTermChanged(value: UserSettings, oldValue: UserSettings) {
    if (value && value != null && oldValue && oldValue != null) {
      this.saveSettings();
    }
  }

  mounted() {
    this.loadSettings();
  }

  loadSettings() {
    ProfileService.getSettings()
      .then(this.settingsLoaded);
  }

  settingsLoaded(settings:UserSettings) {
    this.settings = settings;
  }

  saveSettings() {
    ProfileService.saveSettings(this.settings!)
      .then(this.settingsSaved);
  }

  settingsSaved() {
    this.$root.$emit('toaster-success', 'Paramètre enregistré');
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../less/main";

.settings-page {

  .pane .pane-content {
    padding-left: 0px;
    padding-right: 0px;
  }

  .settings-row {
    padding-left: 40px;
    padding-right: 40px;
    height: 56px;
    border-bottom: 1px solid @solitude;

    display: flex;
    justify-content: space-between;
    align-items: center;

    span {
      font-size: 12px;
      line-height: 16px;
      color: @gunmetal;
    }
  }

}

</style>
