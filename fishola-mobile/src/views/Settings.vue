<!--
  #%L
  Fishola :: Mobile
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
  <div class="settings page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page settings-page">
      <div class="pane pane-only">

        <div class="spinner-wrapper" v-if="loading">
          <div class="spinner"></div>
        </div>

        <div class="pane-content offline" v-if="!loading && offline">
          <span>Les paramètres ne sont pas disponible sans connexion internet</span>
        </div>

        <div class="pane-content rounded" v-if="!loading && !offline">
          <h1 class="no-margin-pane">Paramètres</h1>

          <div class="settings-row" v-if="settings">
            <span>Renseigner le poids des captures</span>
            <FormToggle v-model="settings.promptWeight" />
          </div>

          <div class="settings-row" v-if="settings">
            <span>Effectuer des prélèvements</span>
            <FormToggle v-model="settings.promptSamples" />
          </div>
          <div class="info"
               v-if="samplesDocumentationUrl">
            Pour pouvoir effectuer des prélèvements, vous devez vous munir
            d'un kit dans un des points de collecte :
            <a :href="samplesDocumentationUrl" target="_blank">consulter la liste</a>
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

import { Component, Watch, Vue } from 'vue-property-decorator';
import DocumentationService from '../services/DocumentationService';

@Component({
  components: {
    FisholaHeader,
    FormToggle,
    FisholaFooter
  }
})
export default class SettingsView extends Vue {

  settings:UserSettings | null = null;
  loading:boolean = true;
  offline:boolean = false;

  samplesDocumentationUrl:string = '';

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
    this.samplesDocumentationUrl = DocumentationService.getSamplesDocumentationUrl();
  }

  loadSettings() {
    ProfileService.getSettings()
      .then(this.settingsLoaded, this.cannotLoadSettings);
  }

  cannotLoadSettings() {
    this.loading = false;
    this.offline = true;
  }

  hasOfflineMarker(input:any):boolean {
    return input.offlineMarker;
  }

  settingsLoaded(settings:UserSettings) {
    if (this.hasOfflineMarker(settings)) {
      this.cannotLoadSettings();
    } else {
      this.loading = false;
      this.settings = settings;
    }
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

  .spinner-wrapper {
    width: 100%;
    height: 100%;

    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;

    .spinner {
      height: 60px;
      width: 60px;
      border-radius: 50%;
      border-top: 3px solid @pelorous;
      border-left: 3px solid @pelorous;
      animation:spin 2s linear infinite;
    }
  }

  .pane .pane-content {
    padding-left: 0px;
    padding-right: 0px;

    &.offline {
      height: 100%;
      padding-left: @margin-xx-large;
      padding-right: @margin-xx-large;
      display: flex;
      flex-direction: column;
      justify-content: center;
      span {
        text-align: center;
        color: @carrot-orange;
        font-size: @fontsize-span-big;
        line-height: calc(@fontsize-span-big + @line-height-padding-x-large);
      }
    }
  }

  .settings-row {
    padding-left: @margin-x-large;
    padding-right: @margin-x-large;
    height: 56px;
    border-bottom: 1px solid @solitude;

    display: flex;
    justify-content: space-between;
    align-items: center;

    span {
      font-size: @fontsize-small-paragraph;
      line-height: calc(@fontsize-small-paragraph + @line-height-padding-medium);
      color: @gunmetal;
    }
  }

  .info {
    padding-left: @margin-x-large;
    padding-right: @margin-x-large;
    font-style: italic;
    font-weight: 300;
    font-size: @fontsize-info;
    line-height: calc(@fontsize-info + @line-height-padding-medium);
    color: @pale-sky;
    text-align: center;
  }


  @media screen and (min-width: @desktop-min-width) {
    .pane .pane-content {
      align-items: center;
    }
    .settings-row {
      padding-left: @margin-large-desktop;
      padding-right: @margin-large-desktop;

      span {
        font-size: @fontsize-paragraph;
        line-height: calc(@fontsize-paragraph + @line-height-padding-medium);
      }
    }

    .info {
      margin-top: 50px;
      margin-left: @margin-large-desktop;
      margin-right: @margin-large-desktop;
      padding-left: @margin-xx-large;
      padding-right: @margin-xx-large;
      font-size: @fontsize-paragraph;
      line-height: calc(@fontsize-paragraph + @line-height-padding-medium);
    }

  }

}

</style>
