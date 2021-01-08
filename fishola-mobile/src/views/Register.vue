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
  <div class="register page-with-header shifted-background">
    <FisholaHeader v-bind:title="true"
                   v-bind:avatar="false"
                   v-bind:menu="false"/>
    <div class="page register-page keyboardSensitive">
      <div class="register-form keyboardSensitive">

        <h1 class="keyboardSensitive">Inscription</h1>

        <FormInput name="firstName"
                    label="Prénom"
                    placeholder="Renseignez votre prénom"
                    v-model="bean.firstName"
                    v-bind:error="validationErrors['firstName']"
                    />
        <FormInput name="lastName"
                    label="Nom (optionnel)"
                    placeholder="Renseignez votre nom"
                    v-model="bean.lastName"
                    v-bind:error="validationErrors['lastName']"
                    />
        <FormInput name="email"
                    label="E-mail"
                    placeholder="Renseignez votre E-mail"
                    v-model="bean.email"
                    v-bind:error="validationErrors['email']"
                    />
        <FormInput name="password"
                    type="password"
                    label="Mot de passe"
                    placeholder="Choisissez un mot de passe"
                    v-model="bean.password"
                    v-bind:error="validationErrors['password']"
                    />
        <FormInput name="passwordConfirm"
                    type="password"
                    label="Confirmation du mot de passe"
                    placeholder="Confirmez votre mot de passe"
                    v-model="passwordConfirm"
                    v-bind:error="validationErrors['passwordConfirm']"
                    />

        <div class="form-checkbox">
          <input type="checkbox"
                  id="register-cgu"
                  class="pelorous-checkbox"
                  v-model="cgu" />
          <label for="register-cgu"></label>
          <label for="register-cgu" class="register-cgu-label">J'ai lu et j'accepte les <a :href="cguUrl">Conditions Générales d'Utilisation</a></label>
        </div>
        <div class="bottom-page-spacer"></div>
      </div>
      <div class="register-buttons">
        <div class="register-button back">
          <button v-on:click="cancel">
            Retour
          </button>
        </div>
        <div class="register-button register">
          <button v-on:click="register">
            S'enregistrer
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import Constants from '@/services/Constants';
import UserRegister from '@/pojos/UserRegister';

import Helpers from '@/services/Helpers';

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import FormInput from '@/components/common/FormInput.vue'
import router from '@/router'

import DocumentationService from '@/services/DocumentationService';
import ProfileService from '@/services/ProfileService';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    FormInput
  }
})
export default class RegisterView extends Vue {

  bean:UserRegister = new UserRegister();
  passwordConfirm:string = '';
  validationErrors:any = {
    passwordConfirm : ''
  };
  cgu:boolean = false;
  cguUrl:string = '';

  constructor() {
    super();
  }

  mounted() {
    this.cguUrl = DocumentationService.getCGUUrl();
  }

  cancel() {
    router.push('/login');
  }

  register() {
    this.cleanValidationErros();

    if (this.passwordConfirm == this.bean.password && this.cgu) {
      ProfileService.register(this.bean, this.registrationOk, this.setValidationErrors, this.technicalError);
    } else if (!this.cgu) {
      this.$root.$emit('toaster-error', 'Vous devez accepter les CGU');
    } else {
      this.validationErrors['passwordConfirm'] = 'Les mots de passe ne correspondent pas';
    }
  }

  cleanValidationErros() {
    if (this.validationErrors) {
      const keys = Object.keys(this.validationErrors);
      keys.forEach(key => this.validationErrors[key] = '');
    }
  }

  registrationOk() {

    Helpers.alert(this.$modal, 'Vous devez confirmer votre adresse e-mail avant d\'utiliser FISHOLA. Merci de vérifier votre boîte e-mail', 'Compte enregistré')
      .then(() => {
        this.$root.$emit('toaster-success', 'Compte enregistré. Vous devez valider votre e-mail', 10000);
        router.push('/login');
      });

  }

  setValidationErrors(validationErrors:any) {
    this.validationErrors = validationErrors;
    this.$root.$emit('toaster-error', 'Veuillez corriger les erreurs');
  }

  technicalError(status:number) {
    this.$root.$emit('toaster-error', "Erreur technique, merci de réessayer plus tard");
  }
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../less/main";

.register.page-with-header {
  .page.register-page  {
    height: calc(100% - @header-height - @vertical-margin-xx-large);

    &.keyboardShowing {
      height: calc(100% - @vertical-margin-xx-large);
    }

    @media screen and (min-width: @desktop-min-width) {
      height: 100%;
    }

  }
}

.register-page {

  display: flex;
  flex-direction: column;
  justify-content: space-between;

  text-align:center;

  background-color: @white-smoke;
  border-top-left-radius: 30px;
  border-top-right-radius: 30px;
  margin-top: @vertical-margin-xx-large;

  &.keyboardShowing {
    margin-top: calc(5px + env(safe-area-inset-top));
    padding-top: 0px;
    padding-bottom:2px;
    height: calc(100% - 5px);
  }

  h1 {
    margin-top: @margin-large;
    margin-bottom: @margin-large;
    height: calc(@fontsize-title + @line-height-padding-xx-large);
    font-style: normal;
    font-weight: normal;
    font-size: @fontsize-title;
    line-height: calc(@fontsize-title + @line-height-padding-xx-large);
    color: @pelorous;
    text-align: center;

    // &.keyboardShowing {
    //   margin-top: calc(@fontsize-title * 1.1);
    //   margin-bottom: calc(@fontsize-title * 1.1);
    //   line-height: calc(@fontsize-title + @line-height-padding-large);
    //   padding-top: 0px;
    //   padding-bottom:2px;
    //   height: calc(100% - 5px);
    // }

    @media(max-height:579px) {
      margin-top: @margin-medium;
      margin-bottom: @margin-medium;
    }

    @media(max-height:450px) {
      margin-top: @margin-small;
      margin-bottom: @margin-small;
    }

  }

  .register-form {
    height: calc(100% - 55px);
    padding-left: @margin-large;
    padding-right: @margin-large;

    &.keyboardShowing {
      height: calc(100%);
    }

    display: flex;
    flex-direction: column;;

    text-align:left;
    overflow: auto;

    .form-input label {
      color: @black;
    }

    .form-input input {
      background: transparent;
      border: 1px solid @pale-sky;
      color: @pale-sky;
    }

    // .form-input input:focus {
    //   color: @pale-sky;
    // }

    .register-cgu-label {
      margin-left: @margin-small;
      color: @gunmetal;
      font-size: @fontsize-small-paragraph;
      line-height: calc(@fontsize-small-paragraph + @line-height-padding-medium);
      font-weight: 300;

      a {
        color: @pelorous;
        font-weight: bold;
        text-decoration: none;
      }
    }
  }

  .register-buttons {
    position: absolute;
    width: 100vw;
    bottom: 0px;
    height: 55px;
    background-color: @zircon;
    padding-left: @margin-large;
    padding-right: @margin-large;

    // &.keyboardShowing {
    //   padding-top: 5px;
    // }

    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;

    .register {
      height: 45px;
      // &.keyboardShowing {
      //   margin:auto;
      //   // margin-bottom: -30px;
      //   // margin-top: -22px;
      // }
      button {

          height: 100%;
          width: 100%;
          border-radius: 38px;

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

    .back {
      height: 45px;

      button {

          height: 100%;
          width: 100%;
          border-radius: 38px;

          font-style: normal;
          font-weight: bold;
          font-size: @fontsize-button;
          line-height: calc(@fontsize-button + @line-height-padding-x-large);

          border: 1px solid @pelorous;
          padding-left: @margin-medium;
          padding-right: @margin-medium;

          background-color: transparent;
          color: @pelorous;

      }
    }

  }


  @media screen and (min-width: @desktop-min-width) {

    justify-content: flex-start;

    border-top-left-radius: unset;
    border-top-right-radius: unset;
    padding-top: 0px;
    margin-top: 0px;

    padding-left: @margin-large-desktop;
    padding-right: @margin-large-desktop;

    h1 {
      margin-top: @margin-medium;
      margin-bottom: @margin-xx-large;
      font-size: @fontsize-title-desktop;
      height: calc(@fontsize-title-desktop + @line-height-padding-xx-large);
      line-height: calc(@fontsize-title-desktop + @line-height-padding-xx-large);
      text-align: left;
    }

    .register-form {
      height: unset;
      padding-left: 0px;
      padding-right: 0px;
    }

    .register-buttons {
      position: unset;
      width: 100%;
      bottom: unset;
      margin-top: @margin-large;
      background-color: transparent;
      flex-direction: row-reverse;
      justify-content: center;

      .register-button {
        width: @desktop-button-width;
        margin-left: @margin-small;
        margin-right: @margin-small;
      }
    }

  }


}

</style>
