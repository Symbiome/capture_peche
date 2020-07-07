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
  <div class="register page-with-header shifted-background">
    <FisholaHeader v-bind:title="true"
                   v-bind:avatar="false"
                   v-bind:menu="false"/>
    <div class="page register-page">
      <div class="register-form">

        <h1>Inscription</h1>

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
      </div>
      <div class="register-buttons keyboardSensitive">
        <div class="back hiddenWhenKeyboardShows">
          <button v-on:click="cancel">
            Retour
          </button>
        </div>
        <div class="register keyboardSensitive">
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
      let keys = Object.keys(this.validationErrors);
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

.register-page {

  display: flex;
  flex-direction: column;
  justify-content: space-between;

  text-align:center;

  background-color: @white-smoke;
  border-top-left-radius: 30px;
  border-top-right-radius: 30px;
  margin-top: 50px;
  padding-top: 30px;
  padding-bottom: 90px;

  &.keyboardShowing {
    margin-top: calc(5px + env(safe-area-inset-top));
    padding-top: 0px;
    padding-bottom:2px;
    height: calc(100% - 5px);
  }

  h1 {
    margin-top: 0px;
    margin-bottom: 30px;
    height: 30px;
    font-style: normal;
    font-weight: normal;
    font-size: @fontsize-title;
    line-height: calc(@fontsize-title + @line-height-padding-xx-large);
    color: @pelorous;
    text-align: center;
  }

  .register-form {
    padding-left: 30px;
    padding-right: 30px;

    display: flex;
    flex-direction: column;

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
      margin-left: 10px;
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
    padding-left: 30px;
    padding-right: 30px;

    &.keyboardShowing {
      height:35px;
    }

    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;

    .register {
      height: 45px;
      &.keyboardShowing {
        margin:auto;
        margin-top: -10px;
      }
      button {

          height: 100%;
          width: 100%;
          border-radius: 38px;

          font-style: normal;
          font-weight: bold;
          font-size: @fontsize-button;
          line-height: calc(@fontsize-button + @line-height-padding-x-large);

          border: 0px;
          padding-left: 20px;
          padding-right: 20px;

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
          padding-left: 20px;
          padding-right: 20px;

          background-color: transparent;
          color: @pelorous;

      }
    }

  }


}

</style>
