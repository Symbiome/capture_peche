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
        <div class="form-group">
          <span>E-mail</span>
          <input type="text" name="email" v-model="email" placeholder="Renseignez votre E-mail"/>
        </div>
        <div class="form-group">
          <span>Mot de passe</span>
          <input type="password" name="password" v-model="password" placeholder="Renseignez votre mot de passe"/>
        </div>
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

import FisholaHeader from '@/layout/FisholaHeader.vue'
import router from '@/router'


import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader
  }
})
export default class Login extends Vue {

  email = '';
  password = '';

  constructor() {
    super();
  }

  mounted() {
    this.email = 'thimel@codelutin.com';
  }

  signIn() {

    function httpCall(method: string, url:string, data:any, callback:()=>any) {
        var xhr = new XMLHttpRequest();
        xhr.open(method, url, true);
        xhr.withCredentials = true;
        if (callback) {
            xhr.onload = function() {
              // console.log(this);
              if (this.status == 200) {
                // let responseText = this['responseText'];
                // console.log("responseText: " + responseText);
                // let parsed = JSON.parse(responseText);
                callback();
              } else if (this.status == 401) {
                console.error("Need to login");
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

    let apiUrl = Constants.apiUrl("/v1/security/login");
    let url = `${apiUrl}?email=${this.email}&password=${this.password}`;
    httpCall('GET', url, null, this.signedIn);

  }

  signedIn() {
    router.push('trips');
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

    .form-group {
      margin-top: 10px;
      margin-bottom: 10px;

      font-size: 12px;
      line-height: 16px;

      color: @white;

      span {
        font-weight: 300;
      }

      input {
        background: #000000;
        opacity: 0.5;
        border-radius: 4px;
        height: 38px;
        border: 0px;
        color: @white;
        padding-left: 10px;
        padding-right: 10px;
        margin-top: 5px;
      }

      input::placeholder {
        font-style: italic;
        font-weight: normal;
        font-size: 12px;
      }

      input:focus {
        color: @white;
      }
    }

    span, input {
      width: 100%;
    }
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
