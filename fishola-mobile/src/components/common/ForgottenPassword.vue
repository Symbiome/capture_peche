<template>
  <div class="forgotten-password keyboardSensitive" v-bind:class="{ expanded: !collapsed, collapsed: collapsed}">
    <a v-on:click="expandCollapse">
      <!-- Back button (expanded formed) -->
      <div v-if="!collapsed" class="pastille backarrow">
        <i class="icon-arrow icon-back"></i>
      </div>
      <!-- Title (collapsed format) -->
      <span v-if="collapsed"> Mot de passe oublié ?</span> 
    </a>
    <div v-if="!collapsed && !reinitRequestSent">
       <FormInput name="forgottenEmail"
          type="email"
          label="Pour quel e-mail avez-vous oublié votre mot de passe? "
          placeholder="Renseignez votre E-mail"
          v-model="forgottenEmail"
          v-bind:error="emailError"
        />
        <FormInput name="newPassword"
          type="password"
          label="Choisissez un nouveau mot de passe"
          placeholder="Nouveau mot de passe"
          v-model="newPassword"
          v-bind:error="passwordError"
        />
        <div class="sendpassword"><button v-on:click="resetPassword">Réinitialiser</button></div>
    </div>
    <div class="reinitRequestSent" v-if="reinitRequestSent">
      Votre demande de réinitialisation de mot de passe a été envoyée. Veuillez-vérifier vos emails sur le compte {{ forgottenEmail }}
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import FormInput from '@/components/common/FormInput.vue';
import Constants from '@/services/Constants';
import ProfileService from '@/services/ProfileService';

/**
* ForgottenPassword component, which, when clicked:
* - hides all the css classes given in props
* - alows user to enter an email and get forgotten password reset mail
*/
@Component({
  components: {
    FormInput
  }
})
export default class ForgottenPassword extends Vue {

  // Already typed email (typically to reuse email already typed in a login input)
  @Prop() alreadTypedEmail?:string;
  // Css selector describing elements to hide when component gets clicked
  // Example value: ".class1,.class2,.class3"
  @Prop() tohideSelector?:string;

  // Indicates if components is in its collapsed (just forgotten password text) or expanded (with inputs & buttons) form
  private collapsed: boolean = true;

  private forgottenEmail?: string;
  private newPassword?: string;
  private emailError = '';
  private passwordError = '';
  private reinitRequestSent = false;

  /*
  * Expand or collapsed the component according to its current state
  */
  expandCollapse() {
    // Initialize email with already typed email
    this.forgottenEmail = this.alreadTypedEmail;
    this.newPassword = '';
    this.reinitRequestSent = false;
    this.hideRevealElements();
    this.collapsed = !this.collapsed;
  }

  /**
  * Sends password reinitialization request to server
  */
  resetPassword() {

    this.emailError = '';
    this.passwordError = '';
    let loginBean =  {email: this.forgottenEmail || "", password: this.newPassword || ""};

    ProfileService
      .resetPassword(loginBean)
      .then(this.resetResult, () => {this.resetResult(404)});
  }

  resetResult(status:number) {
    switch(status) {
      case 200:
        this.$root.$emit('toaster-success', 'Demande de changement envoyée');
        this.reinitRequestSent = true;
        break;
      case 400:
        this.$root.$emit('toaster-error', 'Mot de passe non valide');
        this.passwordError = 'Mot de passe non valide';
        break;
      case 404:
        this.$root.$emit('toaster-error', 'E-mail inconnu');
        this.emailError = 'E-mail inconnu';
        break;
      default:
        this.$root.$emit('toaster-error', 'Erreur technique');
        this.emailError = 'Erreur technique, veuillez réessayer';
        console.error("Unexpected status: " + status);
    }
  }

  /**
  * Hides or reveal all elements matching the toHideSelector prop
  */
  hideRevealElements() {
    if (this.tohideSelector != null) {
      let toHides = document.querySelectorAll(this.tohideSelector);
      toHides.forEach( toHide => {
        if (toHide.classList.contains("hidden")) {
          toHide.classList.remove("hidden");
        } else {
          toHide.classList.add("hidden");
        }
      });
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";
  .forgotten-password {
    font-size: 16px;
    line-height: 22px;
    margin-left: 30px;
    margin-right: 30px;
    margin-bottom:30px;
    &.collapsed {
      // Hidden in collapsed mode
      &.keyboardShowing {
        display:none;
      }
    }
    &.expanded {
      &.keyboardShowing {
        position: absolute;
        bottom: 0;
        border-top-left-radius: 30px;
        border-top-right-radius: 30px;
        color: @gunmetal;
        width:100vw;
        padding-top:20px;
        padding-left:30px;
        padding-right:30px;
        margin-left: 0px;
        margin-right: 0px;
        margin-bottom: -10px;
        background-color: @white-smoke;
      }
    }
  }
  .backarrow {
    float: left;
    color: @pelorous;
  }

  .sendpassword {
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
    .reinitRequestSent {
      font-size: 14px;
    }
</style>
