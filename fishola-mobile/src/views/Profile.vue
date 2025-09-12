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
  <div class="profile-page ">
    <div class="rounded">
      <h1> Mes cartes de pêches </h1>
      <FishingLicencesView />

      <h1>Mes lacs favoris</h1>
      <LakeSelection
        :selectedLakes="favoriteLakes"
        :favoriteLakes="favoriteLakes"
        :allowMultipleSelection="true"
        v-on:updated="toggleLakeFavorite"
        @favoriteLakesChanged="favoriteLakesChanged"
      />

      <h1>Mon profil</h1>
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
      <div class="form-checkbox">
        <input type="checkbox" id="show-trips" class="pelorous-checkbox" v-model="profile.acceptsShareTrips" />
        <label for="show-trips"></label>
        <label for="show-trips" class="real-label">
          Je souhaite partager mes sorties avec les utilisateurs Fishola qui pêchent sur les mêmes lacs que moi.
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
      <BottomInducementView icon="/img/fish-yellow.svg" title="Devenez ambassadeur FISHOLA"
        text="Vous pouvez vous inscrire à notre nouveau programme d'ambassadeur." actionText="Je m'inscris"
        @click="becomeAmbassador" />

      <div class="bottom-page-spacer">
      </div>
    </div>

    <FisholaFooter button-text="Modifier" v-on:buttonClicked="saveProfile" shortcuts="back,settings,profile"
      selected="profile" />
  </div>
</template>

<script lang="ts">

import FormInput from "@/components/common/FormInput.vue";
import FormSelect from "@/components/common/FormSelect.vue";
import FormMultiValues from "@/components/common/FormMultiValues.vue";
import LakeSelection from "@/components/common/LakeSelection.vue";

import UserProfile from "@/pojos/UserProfile";
import ProfileService from "@/services/ProfileService";

import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import { RouterUtils } from "@/router/RouterUtils";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import Helpers from "../services/Helpers";
import FishingLicencesView from "./FishingLicences.vue";
import BottomInducementView from "@/components/common/BottomInducement.vue";
import { Lake } from "@/pojos/BackendPojos";

@Component({
  components: {
    FormInput,
    FormSelect,
    FormMultiValues,
    FishingLicencesView,
    FisholaFooter,
    BottomInducementView,
    LakeSelection
  },
})
export default class ProfileView extends Vue {
  @Prop() profile: UserProfile
  favoriteChanged = false;
  favoriteLakes: Lake[] = [];

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

  favoriteLakesChanged(newFavoriteLakes: Lake[]) {
    this.favoriteChanged = true;
    this.favoriteLakes = newFavoriteLakes.sort((a,b) => {
      return a.name > b.name ? 1 : -1;
    })
  }

  toggleLakeFavorite(lake : Lake) {
    this.favoriteChanged = true;

    if (lake) {
      let filteredItem = this.favoriteLakes.filter((l) => {
        return l.id === lake.id;
      });
      if (filteredItem.length == 1) {
        this.favoriteLakes = this.favoriteLakes.filter(function(l) { return l.id != lake.id });
      } else {
        this.favoriteLakes.push(lake);
        this.favoriteLakes =  this.favoriteLakes.sort((a,b) => {
        return a.name > b.name ? 1 : -1;
      })
      }
    }
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
    if (!this.profile.lastNewsSeenDate) {
      this.profile.lastNewsSeenDate = new Date(2025,6,1);
    }
    ProfileService.saveProfile(this.profile).then(
      async () => {
        try {
          await ProfileService.updateFavoriteLakes(this.favoriteLakes);
          this.$emit("profile-updated")
          this.$root.$emit("profile-updated");
          this.$root.$emit("toaster-success", "Profil enregistré");
        } catch (e) {
          console.error(e);
          this.$root.$emit(
            "toaster-error",
            "Erreur technique, merci de réessayer plus tard"
          );
        }
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
        RouterUtils.pushRouteNoDuplicate(this.$router, "/login");
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

  becomeAmbassador() {
    this.$root.$emit("open-feedback", "ambassador");
  }

  editPassword() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/profile-password");
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
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
