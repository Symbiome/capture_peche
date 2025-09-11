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
  <div class="login page-with-header full-background">
    <FisholaHeader v-bind:title="false" v-bind:avatar="false" v-bind:menu="true" />
    <div class="page login-page">
      <div class="login-pane">
        <span @click="goHome" class="icon-home home hide-on-mobile">
          <span style="padding-left: 10px">Accueil</span></span>
        <div class="login-title keyboardSensitive">
          <div class="welcome keyboardSensitive hiddenWhenKeyboardShows_SmallScreensOnly">
            Bienvenue sur
          </div>
          <img class="logo keyboardSensitive hiddenWhenKeyboardShows_SmallScreensOnly hide-on-desktop"
            src="img/logo-big.svg" alt="FISHOLA" />
          <img class="logo keyboardSensitive hiddenWhenKeyboardShows_SmallScreensOnly hide-on-mobile"
            src="img/logo-big-positif.svg" alt="FISHOLA" />
        </div>
        <div class="login-form">
          <FormInput name="email" type="email" label="E-mail" placeholder="Renseignez votre E-mail" v-model="email"
            v-bind:error="emailError" v-on:keyupEnter="signIn" />
          <FormInput name="password" type="password" label="Mot de passe" placeholder="Renseignez votre mot de passe"
            v-model="password" v-bind:error="passwordError" v-on:keyupEnter="signIn" has-white-background />
          <ForgottenPassword v-bind:alreadTypedEmail="email"
            v-bind:tohideSelector="'.login-form .form-input,.login-buttons'" class="hide-on-mobile" />
        </div>
        <div class="login-buttons keyboardSensitive">
          <div class="login-button signin keyboardSensitive">
            <button v-on:click="signIn">Connexion</button>
          </div>
          <div class="login-button signup keyboardSensitive">
            <button v-on:click="signUp">Créer un compte</button>
          </div>
          <ForgottenPassword v-bind:alreadTypedEmail="email" v-bind:tohideSelector="'.login-form,.signin,.signup'"
            class="hide-on-desktop" />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import ProfileService from "@/services/ProfileService";
import FormInput from "@/components/common/FormInput.vue";
import ForgottenPassword from "@/components/common/ForgottenPassword.vue";
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import { RouterUtils } from "@/router/RouterUtils";

import { Component, Vue } from "vue-property-decorator";

@Component({
  components: {
    FisholaHeader,
    FormInput,
    ForgottenPassword,
  },
})
export default class LoginView extends Vue {
  email = "";
  emailError = "";
  password = "";
  passwordError = "";

  constructor() {
    super();
  }

  mounted() {
    this.email = "";
    this.emailError = "";
    this.password = "";
    this.passwordError = "";
    if (localStorage && localStorage.latestEmail) {
      this.email = localStorage.latestEmail;
    }
    if (localStorage && localStorage.latestPassword) {
      this.password = localStorage.latestPassword;
    }
  }

  signIn() {
    this.emailError = "";
    this.passwordError = "";
    ProfileService.signin({ email: this.email, password: this.password }).then(
      this.signInResult,
      () => {
        this.signInResult(404);
      }
    );
  }

  signInResult(status: number) {
    switch (status) {
      case 200:
        if (localStorage) {
          localStorage.latestEmail = this.email;
          localStorage.latestPassword = this.password
        }

        this.$root.$emit("profile-updated");
        RouterUtils.pushRouteNoDuplicate(this.$router, RouterUtils.homeRoute());

        // Après login, on tente de télécharger les settings
        ProfileService.prepareCaches().then(
          () =>
            console.debug(
              "Préparation des caches du profil utilisateur terminée"
            ),
          (error) =>
            console.error(
              "Erreur lors de la préparation des caches du profil utilisateur",
              error
            )
        );
        break;
      case 401:
        this.$root.$emit("toaster-error", "E-mail ou mot de passe incorrect");
        this.passwordError = "Mot de passe erroné";
        break;
      case 404:
        this.$root.$emit("toaster-error", "E-mail ou mot de passe incorrect");
        this.emailError = "E-mail inconnu";
        break;
      default:
        console.error("Unexpected status: " + status);
    }
  }

  signUp() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "register");
  }

  autoLogin() {
    this.email = "thimel@codelutin.com";
    this.password = "sispea";
    this.signIn();
  }

  goHome() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/");
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@media screen and (min-width: @desktop-min-width) {
  div.login {
    &.full-background {
      background-position: left;
      background-position-x: 640px;
      background-position-y: 60%;
    }
  }
}

.login-page {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
}

.login-pane {
  height: 100%;
  width: 100%;

  display: flex;
  flex-direction: column;
  justify-content: space-between;

  text-align: center;

  .login-title {
    height: 140px;

    &.keyboardShowing {
      margin-top: calc(2 * env(safe-area-inset-top));
      height: 81px;

      @media (max-height: 500px) {
        margin-top: 0px;
        height: 0px;
      }
    }

    display: flex;
    flex-direction: column;
    justify-content: center;

    .welcome {
      font-size: @fontsize-header-title;
      line-height: calc(@fontsize-header-title + @line-height-padding-xxx-large );

      @media (max-height: 579px) {
        font-size: @fontsize-header-title-small;
        line-height: calc(@fontsize-header-title-small + @line-height-padding-xxx-large );
      }

      &.keyboardShowing {
        font-size: @fontsize-title-keyboardshowing;
        line-height: calc(@fontsize-title-keyboardshowing + @line-height-padding-small );
      }
    }

    .logo {
      height: 100px;

      @media (max-height: 579px) {
        height: 80px;
      }

      &.keyboardShowing {
        height: 65px;
      }
    }
  }

  .login-form {
    margin-left: @margin-large;
    margin-right: @margin-large;

    display: flex;
    flex-direction: column;

    text-align: right;

    .form-input label {
      color: @white;
    }

    .form-input input {
      background-color: @black-alpha-50;
      border: 1px solid @transparent;
      color: @white;

      &:focus {
        color: @white;
      }

      &::placeholder {
        color: @white;
      }
    }

    .form-input div.field-error {
      background-color: @cardinal;
      color: @white;
      padding-left: @margin-x-small;
      padding-right: @margin-x-small;
    }
  }

  .login-buttons {
    background-color: @white-smoke;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    color: @gunmetal;
    padding-top: @vertical-margin-medium;

    display: flex;
    flex-direction: column;
    justify-content: flex-start;

    &.keyboardShowing {
      padding-top: 5px;
      background-color: @transparent;
    }

    .login-button {
      height: 45px;
      margin-left: @margin-large;
      margin-right: @margin-large;
      margin-bottom: @vertical-margin-medium;

      &.signin {
        &.keyboardShowing {
          margin-bottom: -30px;
        }

        button {
          height: 100%;
          width: 100%;
          border-radius: 50px;

          font-style: normal;
          font-weight: bold;
          font-size: @fontsize-button;
          line-height: calc(@fontsize-button + @line-height-padding-x-large);

          border: 0px;
          padding-left: @margin-medium;
          padding-right: @margin-medium;

          background-color: @terra-cotta;
          color: @white;
        }
      }

      &.signup {
        &.keyboardShowing {
          display: none;
        }

        button {
          height: 100%;
          width: 100%;
          border-radius: 50px;

          font-style: normal;
          font-weight: bold;
          font-size: @fontsize-button;
          line-height: calc(@fontsize-button + @line-height-padding-x-large);

          border: 1px solid @pelorous;
          padding-left: @margin-medium;
          padding-right: @margin-medium;

          background-color: @white-smoke;
          color: @pelorous;
        }
      }
    }
  }

  @media screen and (min-width: @desktop-min-width) {
    width: 640px;
    background-color: @white-smoke;
    justify-content: space-evenly;

    .login-title {
      color: @pelorous;

      .welcome {
        font-size: @fontsize-header-title-desktop;
        line-height: calc(@fontsize-header-title-desktop + @line-height-padding-xxx-large );
        margin-bottom: @margin-small;
      }
    }

    .login-form {
      margin-left: @margin-xx-large-desktop;
      margin-right: @margin-xx-large-desktop;
      color: @pelorous;

      .form-input label {
        color: @black;
      }
    }

    .login-buttons {
      margin-left: @margin-xx-large-desktop;
      margin-right: @margin-xx-large-desktop;
      background-color: @white-smoke;

      display: flex;
      flex-direction: row;
      justify-content: space-between;

      .login-button {
        height: 45px;
        margin-left: 0px;
        margin-right: 0px;
        margin-bottom: @vertical-margin-medium;
      }
    }
  }

  .home {
    position: absolute;
    cursor: pointer;
    top: 20px;
    left: 20px;
    color: @pelorous;
    font-size: 30px;
    font-size: @fontsize-header-title;
  }
}
</style>
