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
        <div class="signup hiddenWhenKeyboardShows"><button v-on:click="signUp">Créer un compte</button></div>
        <div class="forgotten-password hiddenWhenKeyboardShows"><a v-on:click="forgottenPassword">Mot de passe oublié ?</a></div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import Constants from '@/services/Constants';

import FormInput from '@/components/common/FormInput.vue'
import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import router from '@/router'


import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    FormInput
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

    function httpCall(method: string, url:string, data:any, callback:(status:any)=>any) {
        var xhr = new XMLHttpRequest();
        xhr.open(method, url, true);
        xhr.withCredentials = true;
        if (callback) {
            xhr.onload = function() {
              // console.debug(this);
              if (this.status == 200 || this.status == 401 || this.status == 404) {
                // let responseText = this['responseText'];
                // console.debug("responseText: " + responseText);
                // let parsed = JSON.parse(responseText);
                callback(this.status);
              } else {
                console.error("Error during httpcall " + url + " " + this.status);
              }
          };
        }
        if (data != null) {
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.send(JSON.stringify(data));
        }
        else xhr.send();
    }

    this.emailError = '';
    this.passwordError = '';

    let loginBean =  {email: this.email, password: this.password};

    let apiUrl = Constants.apiUrl("/v1/security/login");
    httpCall('POST', apiUrl, loginBean, this.signInResult);

  }

  signInResult(status:number) {
    switch(status) {
      case 200:
        this.$root.$emit('profile-updated');
        router.push('trips');
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

  forgottenPassword() {
    this.$root.$emit('toaster-warning', 'Work in progress');
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
    padding-top: 30px;

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
          margin-bottom: 5px; 
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
      margin-bottom: 30px;

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

    .forgotten-password {
      font-size: 16px;
      line-height: 22px;
      margin-bottom: 30px;
    }

  }
}

</style>
