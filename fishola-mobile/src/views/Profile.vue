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
  <div class=" profile-page ">
    <div class="pane-content rounded">
      <FormInput name="firstName" label="Prénom" placeholder="Renseignez votre prénom" v-model="profile.firstName"
        v-bind:error="validationErrors['firstName']" />
      <FormInput name="lastName" label="Nom (optionnel)" placeholder="Renseignez votre nom" v-model="profile.lastName"
        v-bind:error="validationErrors['lastName']" />
      <FormInput name="email" label="E-mail" placeholder="Renseignez votre E-mail" v-model="profile.email"
        v-bind:error="validationErrors['email']" />
      <FormSelect name="birthYear" label="Année de naissance (optionnelle)" v-bind:options="years"
        v-model="birthYear" />
      <FormSelect name="gender" label="Sexe (optionnel)" v-bind:options="genders" v-model="gender" />
      <FormMultiValues name="password" label="Mot de passe" v-bind:values="['********']" v-on:clicked="editPassword" />
      <div class="form-checkbox">
        <input type="checkbox" id="receive-mail" class="pelorous-checkbox" v-model="profile.acceptsMailNotifications" />
        <label for="receive-mail"></label>
        <label for="receive-mail" class="real-label">
          Je souhaite être informé des communications Fishola par mail
        </label>
      </div>
      <br />
      <a @click="safeDeleteAccount" class="safe-delete-button">Supprimer mon compte</a>
      <br />
      <br />
      <div class="buttons-bar hide-on-mobile">
        <div class="button button-primary modify-button">
          <button v-on:click="saveProfile">Modifier</button>
        </div>
      </div>

      <div class="bottom-page-spacer"></div>
    </div>
    <FisholaFooter button-text="Modifier" v-on:buttonClicked="saveProfile" shortcuts="back,settings,profile"
      selected="profile" />
  </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";

import FormInput from "@/components/common/FormInput.vue";
import FormSelect from "@/components/common/FormSelect.vue";
import FormMultiValues from "@/components/common/FormMultiValues.vue";

import UserProfile from "@/pojos/UserProfile";
import ProfileService from "@/services/ProfileService";

import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import router from "@/router";
import { RouterUtils } from "@/router/RouterUtils";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import Helpers from "../services/Helpers";

@Component({
  components: {
    FormInput,
    FormSelect,
    FormMultiValues,
    FisholaFooter,
  },
})
export default class ProfileView extends Vue {
  @Prop() profile: UserProfile

  birthYear: string = "0";
  gender: string = "EMPTY";

  validationErrors: any = {};

  genders: any[] = [
    { id: "EMPTY", name: "" },
    { id: "Female", name: "Femme" },
    { id: "Male", name: "Homme" },
    { id: "NonBinary", name: "Non binaire" },
  ];

  years: any[] = [{ id: "0", name: "" }];

  constructor() {
    super();
  }

  mounted() {
    const currentYear = new Date().getFullYear();
    const startYear = currentYear - 110;
    const endYear = currentYear - 10;
    for (let i = startYear; i <= endYear; i++) {
      this.years.push({ id: "" + i, name: i });
    }
  }



  @Watch("profile")
  profileLoaded(profile: UserProfile) {
    this.profile = profile;
    this.birthYear = "" + (profile.birthYear || 0);
    this.gender = profile.gender || "EMPTY";
  }

  saveProfile() {
    this.cleanValidationErros();

    if (this.birthYear == "0") {
      delete this.profile.birthYear;
    } else {
      this.profile.birthYear = parseInt(this.birthYear);
    }

    if (this.gender == "EMPTY") {
      delete this.profile.gender;
    } else {
      this.profile.gender = this.gender;
    }
    this.profile.lastNewsSeenDate = Helpers.parseLocalDateTime(
      // @ts-ignore
      this.profile.lastNewsSeenDate
    );
    ProfileService.saveProfile(this.profile).then(
      () => {
        this.$emit("profile-updated")
        this.$root.$emit("profile-updated");
        this.$root.$emit("toaster-success", "Profil enregistré");
      },
      (response) => {
        if (response.status == 400) {
          this.validationErrors = response.content;
          this.$root.$emit("toaster-error", "Veuillez corriger les erreurs");
        } else {
          this.$root.$emit(
            "toaster-error",
            "Erreur technique, merci de réessayer plus tard"
          );
        }
      }
    );
  }

  safeDeleteAccount() {
    Helpers.confirm(
      this.$modal,
      "Confirmez-vous vouloir supprimer définitivement votre compte ? Cette opération est irreversible",
      "Supprimer mon compte définitivement"
    ).then(() => {
      ProfileService.safeDeleteAccount(this.profile).then(() => {
        this.$root.$emit(
          "toaster-success",
          "Votre compte a été supprimé",
          5000
        );
        RouterUtils.pushRouteNoDuplicate(router, "/login");
        this.$root.$emit("loggued-out");
      });
    });
  }

  cleanValidationErros() {
    if (this.validationErrors) {
      const keys = Object.keys(this.validationErrors);
      keys.forEach((key) => (this.validationErrors[key] = ""));
    }
  }

  editPassword() {
    RouterUtils.pushRouteNoDuplicate(router, "/profile-password");
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../less/main";

.profile-page {
  height: 100%;

  .safe-delete-button {
    font-weight: bold;
    color: @pelorous;
    cursor: pointer;

    &:hover {
      color: @terra-cotta;
    }
  }

  .modify-button {
    position: absolute;
    bottom: 10px;
  }
}
</style>
