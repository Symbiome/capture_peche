<template>
  <div class="register page-with-header">
    <FisholaHeader v-bind:title="true"
                   v-bind:avatar="false"
                   v-bind:menu="false"/>
    <div class="page register-page">
      <div class="register-form">

        <h1>Inscription</h1>

        <FormInput name="lastName"
                    label="Nom"
                    placeholder="Renseignez votre nom"
                    v-model="bean.lastName"
                    v-bind:error="validationErrors['lastName']"
                    />
        <FormInput name="firstName"
                    label="Prénom"
                    placeholder="Renseignez votre prénom"
                    v-model="bean.firstName"
                    v-bind:error="validationErrors['firstName']"
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
      </div>
      <div class="register-buttons">
        <div class="back">
          <button v-on:click="cancel">
            Retour
          </button>
        </div>
        <div class="register">
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

import FisholaHeader from '@/layout/FisholaHeader.vue'
import FormInput from '@/components/common/FormInput.vue'
import router from '@/router'


import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    FormInput
  }
})
export default class Register extends Vue {

  bean:UserRegister = new UserRegister();
  passwordConfirm:string = '';
  validationErrors:any = {
    passwordConfirm : ''
  };

  constructor() {
    super();
  }

  mounted() {
  }

  cancel() {
    router.push('/login');
  }

  register() {

    function httpCall(method: string, url:string, data:any, successCallback:()=>any, errorCallback:(validationErrors:any)=>any) {
      var xhr = new XMLHttpRequest();
      xhr.open(method, url, true);
      xhr.withCredentials = true;
      xhr.onload = function() {
        // console.log(this);
        if (this.status == 200) {
          successCallback();
        } else if (this.status == 400) {
          let responseText = this['responseText'] || '{}';
          console.log("responseText: " + responseText);
          let parsed = JSON.parse(responseText);
          errorCallback(parsed);
        } else {
          console.error("C'est la merde noire, façon " + this.status);
        }
      };
      if (data != null) {
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.send(JSON.stringify(data));
      } else {
        xhr.send();
      }
    }

    this.cleanValidationErros();

    if (this.passwordConfirm == this.bean.password) {
      let apiUrl = Constants.apiUrl("/v1/security/register");
      httpCall('PUT', apiUrl, this.bean, this.registrationOk, this.setValidationErrors);
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
    this.$root.$emit('toaster-success', 'Compte enregistré. Vous devez valider votre e-mail', 10000);
    router.push('/login');
  }

  setValidationErrors(validationErrors:any) {
    this.validationErrors = validationErrors;
    this.$root.$emit('toaster-error', 'Veuillez corriger les erreurs');
  }
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../less/main";

.register {
  background-image: url("/img/background.png");
  background-repeat: no-repeat;
  background-size: cover;
  background-position-y: -415px;
}

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

  h1 {
    margin-top: 0px;
    margin-bottom: 30px;
    height: 30px;
    font-style: normal;
    font-weight: normal;
    font-size: 22px;
    line-height: 30px;
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

  }

  .register-buttons {
    height: 100px;
    background-color: @zircon;
    padding-left: 30px;
    padding-right: 30px;

    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;

    .register {
      height: 45px;

      button {

          height: 100%;
          width: 100%;
          border-radius: 38px;

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

    .back {
      height: 45px;

      button {

          height: 100%;
          width: 100%;
          border-radius: 38px;

          font-style: normal;
          font-weight: bold;
          font-size: 18px;
          line-height: 25px;

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
