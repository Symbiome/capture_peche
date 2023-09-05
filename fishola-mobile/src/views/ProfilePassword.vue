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
  <div class="profile-pasword page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="profile-pasword-page page">
      <div class="pane">
        <div class="pane-content rounded">
          <h1>Mot de passe</h1>
          <FormInput
            name="currentPassword"
            type="password"
            label="Mot de passe actuel"
            placeholder="Tapez votre mot de passe"
            v-model="bean.currentPassword"
            v-bind:error="validationErrors['currentPassword']"
          />
          <FormInput
            name="newPassword"
            type="password"
            label="Nouveau mot de passe"
            placeholder="Choisissez un mot de passe"
            v-model="bean.newPassword"
            v-bind:error="validationErrors['newPassword']"
          />
          <FormInput
            name="confirm"
            type="password"
            label="Confirmation du mot de passe"
            placeholder="Confirmez votre mot de passe"
            v-model="confirm"
            v-bind:error="validationErrors['confirm']"
          />

          <div class="buttons-bar hide-on-mobile">
            <div class="button button-primary">
              <button v-on:click="save">Enregistrer</button>
            </div>
            <div class="button button-secondary">
              <button v-on:click="backToProfile">Annuler</button>
            </div>
          </div>

          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter
      button-text="Enregistrer"
      v-on:buttonClicked="save"
      shortcuts="back,spacer,spacer"
    />
  </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import FormInput from "@/components/common/FormInput.vue";

import { UpdatePasswordBean } from "@/pojos/BackendPojos";

import ProfileService from "../services/ProfileService";

import { Component, Vue } from "vue-property-decorator";
import { RouterUtils } from "@/router/RouterUtils";

import router from "../router";

@Component({
  components: {
    FisholaHeader,
    FormInput,
    FisholaFooter,
  },
})
export default class ProfilePasswordView extends Vue {
  validationErrors: any = {
    currentPassword: "",
    newPassword: "",
    confirm: "",
  };

  bean: UpdatePasswordBean = { currentPassword: "", newPassword: "" };
  confirm: string = "";

  created() {}

  mounted() {}

  cleanValidationErros() {
    if (this.validationErrors) {
      const keys = Object.keys(this.validationErrors);
      keys.forEach((key) => (this.validationErrors[key] = ""));
    }
  }

  save() {
    this.cleanValidationErros();

    if (this.confirm == this.bean.newPassword) {
      ProfileService.updatePassword(this.bean).then(this.saved, this.onError);
      // let apiUrl = Constants.apiUrl("/v1/security/password");
      // httpCall('PUT', apiUrl, this.bean, this.registrationOk, this.setValidationErrors, this.technicalError);
    } else {
      this.validationErrors["confirm"] =
        "Les mots de passe ne correspondent pas";
    }
  }

  saved() {
    this.$root.$emit("toaster-success", "Mot de passe mis à jour");
    this.backToProfile();
  }

  backToProfile() {
    RouterUtils.pushRouteNoDuplicate(router, "profile");
  }

  onError(response: any) {
    if (response.status == 400) {
      this.validationErrors = response.content;
    } else {
      this.$root.$emit(
        "toaster-error",
        "Erreur technique, merci de réessayer plus tard"
      );
    }
  }
}
</script>

<style lang="less">
@import "../less/main";
</style>
