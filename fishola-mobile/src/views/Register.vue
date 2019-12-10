<template>
  <div class="register">
    <FisholaHeader v-bind:title="true"
                   v-bind:avatar="false"
                   v-bind:menu="false"/>
    <div class="register-page">
      <div class="register-form">

        <h1>Inscription</h1>

        <div class="form-group">
          <span>Nom</span>
          <input type="text" name="lastName" v-model="bean.lastName" placeholder="Renseignez votre nom" v-bind:class="validationErrors['lastName']?'field-error':''"/>
          <div class="field-error" v-if="validationErrors['lastName']">{{validationErrors['lastName']}}</div>
        </div>
        <div class="form-group">
          <span>Prénom</span>
          <input type="text" name="firstName" v-model="bean.firstName" placeholder="Renseignez votre prénom" v-bind:class="validationErrors['firstName']?'field-error':''"/>
          <div class="field-error" v-if="validationErrors['firstName']">{{validationErrors['firstName']}}</div>
        </div>
        <div class="form-group">
          <span>E-mail</span>
          <input type="text" name="email" v-model="bean.email" placeholder="Renseignez votre E-mail" v-bind:class="validationErrors['email']?'field-error':''"/>
          <div class="field-error" v-if="validationErrors['email']">{{validationErrors['email']}}</div>
        </div>
        <div class="form-group">
          <span>Mot de passe</span>
          <input type="password" name="password" v-model="bean.password" placeholder="Choisissez un mot de passe" v-bind:class="validationErrors['password']?'field-error':''"/>
          <div class="field-error" v-if="validationErrors['password']">{{validationErrors['password']}}</div>
        </div>
        <div class="form-group">
          <span>Confirmation de mot de passe</span>
          <input type="password" name="passwordConfirm" v-model="passwordConfirm" placeholder="Confirmez votre mot de passe" v-bind:class="validationErrors['passwordConfirm']?'field-error':''"/>
          <div class="field-error" v-if="validationErrors['passwordConfirm']">{{validationErrors['passwordConfirm']}}</div>
        </div>
      </div>
      <div class="register-buttons">
        <div class="back"><button v-on:click="cancel">Retour</button></div>
        <div class="register"><button v-on:click="register">S'enregistrer</button></div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import Constants from '@/services/Constants';
import UserRegister from '@/pojos/UserRegister';

import FisholaHeader from '@/layout/FisholaHeader.vue'
import router from '@/router'


import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader
  }
})
export default class Register extends Vue {

  bean:UserRegister = new UserRegister();
  passwordConfirm:string = '';
  validationErrors:any = {};

  constructor() {
    super();
  }

  mounted() {
  }

  cancel() {
    router.push('/');
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

    if (this.passwordConfirm == this.bean.password) {

      this.validationErrors = {};

      let apiUrl = Constants.apiUrl("/v1/security/register");
      httpCall('PUT', apiUrl, this.bean, this.registrationOk, this.setValidationErrors);
    } else {
      this.validationErrors["passwordConfirm"] = "Les mots de passe ne correspondent pas";
    }


  }

  registrationOk() {
    this.$root.$emit('toaster-success', 'Compte enregistré. Vous devez valider votre e-mail', 10000);
    router.push('/');
  }

  setValidationErrors(validationErrors:any) {
    this.validationErrors = validationErrors;
    this.$root.$emit('toaster-error', 'Veuillez corriger les erreurs');
  }
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.register {
  background-image: url("/img/background.png");
  background-repeat: no-repeat;
  background-size: cover;
  background-position-y: -315px;
  height: calc(100vh - 85px);
}

.register-page {

  display: flex;
  flex-direction: column;
  justify-content: space-between;

  text-align:center;

  height: 100%;
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
    // justify-content: space-between;

    text-align:left;
    overflow: auto;

    .form-group {
      // margin-top: 10px;
      margin-bottom: 20px;

      font-size: 12px;
      line-height: 16px;

      display: flex;
      flex-direction: column;
      align-items: flex-start;

      span {
        font-weight: 300;
        color: @black;
      }

      input {
        background: transparent;
        // opacity: 0.5;
        border-radius: 4px;
        height: 38px;
        border: 1px solid @pale-sky;
        box-sizing: border-box;
        color: @pale-sky;
        padding-left: 10px;
        padding-right: 10px;
        margin-top: 5px;
        width: 100%;
      }

      input.field-error {
        border: 1px solid @cardinal;
      }

      input:focus {
        color: @pale-sky;
      }

      input::placeholder {
        font-style: italic;
        font-weight: normal;
        font-size: 12px;
      }

      div.field-error {
        background-color: @cardinal;
        color: @white;
        font-size: 10px;
        line-height: 14px;
      }
    }

  }

  .register-buttons {
    height: 100px;
    background-color: @zircon;
    padding-left: 30px;
    padding-right: 30px;
// border: 1px solid red;

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
