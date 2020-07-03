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

import router from '@/router'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
  }
})
export default class LoginView extends Vue {


  constructor() {
    super();
  }

  mounted() {
    console.log('Do something');
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
      font-size: 24px;
      line-height: 33px;
      &.keyboardShowing {
        font-size: 16px;
        line-height: 18px;
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
    margin-left: 30px;
    margin-right: 30px;

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
      padding-left: 5px;
      padding-right: 5px;
    }

  }

  .login-buttons {
    background-color: @white-smoke;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    color: @gunmetal;
    padding-top: 20px;

    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    
    &.keyboardShowing {
        padding-top: 5px;
        background-color: @transparent;
    }
    .signin {
      height: 45px;
      margin-left: 30px;
      margin-right: 30px;
      margin-bottom: 20px;
      &.keyboardShowing {
          margin-bottom: -30px; 
      }

      button {
          height: 100%;
          width: 100%;
          border-radius: 50px;

          font-style: normal;
          font-weight: bold;
          font-size: 18px;
          line-height: 25px;

          border: 0px;
          padding-left: 20px;
          padding-right: 20px;

          background-color: @terra-cotta;
          color: @white;
      }
    }

    .signup {
      height: 45px;
      margin-left: 30px;
      margin-right: 30px;
      margin-bottom: 10px;
      &.keyboardShowing {
         display: none;
      }
      button {

          height: 100%;
          width: 100%;
          border-radius: 50px;

          font-style: normal;
          font-weight: bold;
          font-size: 18px;
          line-height: 25px;

          border: 1px solid @pelorous;
          padding-left: 20px;
          padding-right: 20px;

          background-color: @white-smoke;
          color: @pelorous;

      }
    }
  }
}

</style>
