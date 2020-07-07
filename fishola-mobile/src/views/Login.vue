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
  <div class="login page-with-header full-background">
    <FisholaHeader v-bind:title="false" 
                   v-bind:avatar="false"
                   v-bind:menu="false"/>
    <div class="page login-page">
      <div class="login-title keyboardSensitive hiddenWhenKeyboardShows_SmallScreensOnly">
        <div class="welcome keyboardSensitive">Bienvenue sur</div>
        <img class="logo keyboardSensitive" src="img/logo-big.svg" alt="FISHOLA"/>
      </div>
      <div class="login-form">
        <FormInput name="email"
                    type="email"
                    label="E-mail"
                    placeholder="Renseignez votre E-mail"
                    v-model="email"
                    v-bind:error="emailError"
                    />
        <FormInput name="password"
                    type="password"
                    label="Mot de passe"
                    placeholder="Renseignez votre mot de passe"
                    v-model="password"
                    v-bind:error="passwordError"
                    />
      </div>
      <div class="login-buttons keyboardSensitive">
        <div class="signin keyboardSensitive"><button v-on:click="signIn">Connexion</button></div>
        <div class="signup keyboardSensitive"><button v-on:click="signUp">Créer un compte</button></div>
        <ForgottenPassword
            v-bind:alreadTypedEmail="email"
            v-bind:tohideSelector="'.login-form,.signin,.signup'"/>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import Constants from '@/services/Constants'
import ProfileService from '@/services/ProfileService'
import FormInput from '@/components/common/FormInput.vue'
import ForgottenPassword from '@/components/common/ForgottenPassword.vue'
import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import router from '@/router'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    FormInput,
    ForgottenPassword
  }
})
export default class LoginView extends Vue {

  email = '';
  emailError = '';
  password = '';
  passwordError = '';

  constructor() {
    super();
  }

  mounted() {
    this.email = '';
    this.emailError = '';
    this.password = '';
    this.passwordError = '';
  }

  signIn() {
    this.emailError = '';
    this.passwordError = '';
    ProfileService
      .signin({"email": this.email, "password": this.password})
      .then(this.signInResult, () => {this.signInResult(404)}); 
  }

  signInResult(status:number) {
    switch(status) {
      case 200:
        this.$root.$emit('profile-updated');
        router.push('trips');

        // Après login, on tente de télécharger les settings
        ProfileService.prepareCaches()
          .then(
            () => console.debug("Préparation des caches du profil utilisateur terminée"),
            (error) => console.error("Erreur lors de la préparation des caches du profil utilisateur", error)
          );
        break;
      case 401:
        this.$root.$emit('toaster-error', 'E-mail ou mot de passe incorrect');
        this.passwordError = 'Mot de passe erroné';
        break;
      case 404:
        this.$root.$emit('toaster-error', 'E-mail ou mot de passe incorrect');
        this.emailError = 'E-mail inconnu';
        break;
      default:
        console.error("Unexpected status: " + status);
    }
  }

  signUp() {
    router.push('register');
  }

  autoLogin() {
    this.email = 'thimel@codelutin.com';
    this.password = 'sispea';
    this.signIn();
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../less/main";

.login-page {

  display: flex;
  flex-direction: column;
  justify-content: space-between;
  
  text-align:center;

  .login-title {
    height: 140px;
    &.keyboardShowing {
      margin-top: calc(2 * env(safe-area-inset-top));
      height: 81px;
    }
    display: flex;
    flex-direction: column;
    justify-content: center;

    .welcome {
      font-size: @fontsize-header-title;
      line-height: calc(@fontsize-header-title + @line-height-padding-xxx-large);
      &.keyboardShowing {
        font-size: @fontsize-title-keyboardshowing;
        line-height: calc(@fontsize-title-keyboardshowing + @line-height-padding-small);
      }
    }
    .logo {
      height: 100px;
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

    text-align:left;


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
    .signin {
      height: 45px;
      margin-left: @margin-large;
      margin-right: @margin-large;
      margin-bottom: @vertical-margin-medium;
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

    .signup {
      height: 45px;
      margin-left: @margin-large;
      margin-right: @margin-large;
      margin-bottom: @vertical-margin-small;
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

</style>
