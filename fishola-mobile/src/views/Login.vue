<template>
  <div class="login">
    <FisholaHeader v-bind:title="false" 
                   v-bind:avatar="false"
                   v-bind:menu="false"/>
    <div class="login-page">
      <div class="login-title">
        <div class="welcome">Bienvenue sur</div>
        <div class="logo">Fishola</div>
      </div>
      <div class="login-form">
        <InputField label="E-mail"
                    placeholder="Renseignez votre E-mail"
                    name="email"
                    v-model="email"
                    v-bind:error="emailError"
                    />
        <InputField label="Mot de passe"
                    type="password"
                    placeholder="Renseignez votre mot de passe"
                    name="password"
                    v-model="password"
                    v-bind:error="passwordError"
                    />
      </div>
      <div class="login-buttons">
        <div class="remember">
          <input type="checkbox" id="remember-me" class="pelorous-checkbox" />
          <label for="remember-me"></label>
          <label for="remember-me" id="remember-me-label">Se souvenir de moi</label>
          
        </div>
        <div class="signin"><button v-on:click="signIn">Connexion</button></div>
        <div class="signup"><button v-on:click="signUp">Créer un compte</button></div>
        <div class="forgotten-password"><a v-on:click="forgottenPassword">Mot de passe oublié ?</a></div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import Constants from '@/services/Constants';

import InputField from '@/components/common/InputField.vue'
import FisholaHeader from '@/layout/FisholaHeader.vue'
import router from '@/router'


import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    InputField
  }
})
export default class Login extends Vue {

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
              // console.log(this);
              if (this.status == 200 || this.status == 401 || this.status == 404) {
                // let responseText = this['responseText'];
                // console.log("responseText: " + responseText);
                // let parsed = JSON.parse(responseText);
                callback(this.status);
              } else {
                console.error("C'est la merde noire, façon " + this.status);
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
    httpCall('POST', apiUrl, loginBean, this.signedIn);

  }

  signedIn(status:number) {
    switch(status) {
      case 200:
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
    this.$root.$emit('toaster-error', 'Rôôô quel dommage !?');
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.login-page {

  display: flex;
  flex-direction: column;
  justify-content: space-between;
  
  text-align:center;

  height: calc(100vh - 35px);
  
  .login-title {
    height: 140px;
    display: flex;
    flex-direction: column;
    justify-content: center;

    .welcome {
      font-size: 24px;
      line-height: 33px;
    }
    .logo {
      font-family: 'bridamount', 'Avenir', Helvetica, Arial, sans-serif;
      font-size: 62px;
    }
  }

  .login-form {
    margin-left: 30px;
    margin-right: 30px;

    display: flex;
    flex-direction: column;
    justify-content: space-between;

    text-align:left;

  }

  .login-buttons {
    height: 264px;
    background-color: @white-smoke;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    color: @gunmetal;
    padding-top: 10px;

    display: flex;
    flex-direction: column;
    justify-content: space-around;

    .remember {
      font-size: 14px;
      line-height: 19px;
      
      #remember-me-label {
        margin-left: 8px;
      }
    }

    .signin {
      height: 45px;
      margin-left: 30px;
      margin-right: 30px;

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
    }

  }


}

</style>
