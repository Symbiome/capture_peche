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
  <div class="forgotten-password keyboardSensitive" v-bind:class="{ expanded: !collapsed, collapsed: collapsed}">
    <a v-on:click="expandCollapse">
      <!-- Title (collapsed format) -->
      <span v-if="collapsed"> Mot de passe oublié ?</span> 
    </a>
    <div v-if="!collapsed">
       <h1 class="title">Mot de passe oublié</h1>
       <FormInput name="forgottenEmail"
          type="email"
          label="Pour quel e-mail avez-vous oublié votre mot de passe ?"
          placeholder="Renseignez votre E-mail"
          v-model="forgottenEmail"
          v-bind:error="emailError"
          v-on:keyupEnter="sendPasswordReinitialisationRequest"
        />
        <FormInput name="newPassword"
          type="password"
          label="Choisissez un nouveau mot de passe"
          placeholder="Nouveau mot de passe"
          v-model="newPassword"
          v-bind:error="passwordError"
          v-on:keyupEnter="sendPasswordReinitialisationRequest"
        />
        <FormInput name="newPasswordConfirmation"
          type="password"
          label="Confirmez votre nouveau mot de passe"
          placeholder="Nouveau mot de passe"
          v-model="newPasswordConfirmation"
          v-bind:error="passwordConfirmationError"
          v-on:keyupEnter="sendPasswordReinitialisationRequest"
        />
        <div class="sendpassword">
          <button class="cancel" v-on:click="expandCollapse">Annuler</button>
          <button v-on:click="sendPasswordReinitialisationRequest">Réinitialiser</button>
        </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import FormInput from '@/components/common/FormInput.vue';
import Constants from '@/services/Constants';
import ProfileService from '@/services/ProfileService';
import Helpers from '@/services/Helpers';

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
  private newPasswordConfirmation?: string;
  private emailError = '';
  private passwordError = '';
  private passwordConfirmationError = '';

  /*
  * Expand or collapsed the component according to its current state
  */
  expandCollapse() {
    // Initialize email with already typed email
    this.forgottenEmail = this.alreadTypedEmail;
    this.newPassword = '';
    this.newPasswordConfirmation = '';
    this.hideRevealElements();
    this.collapsed = !this.collapsed;
  }

  /**
  * Sends password reinitialization request to server
  */
  sendPasswordReinitialisationRequest() {

    this.emailError = '';
    this.passwordError = '';
    this.passwordConfirmationError = '';
    if (this.newPassword && this.newPassword.length > 2 && this.newPassword != this.newPasswordConfirmation) {
        this.$root.$emit('toaster-error', 'Les mots de passe ne correspondent pas');
        this.passwordConfirmationError = 'Les mots de passe ne correspondent pas';
    } else {
      const loginBean =  {email: this.forgottenEmail || "", password: this.newPassword || ""};

      ProfileService
        .requestPasswordReset(loginBean)
        .then(this.resetResult, () => {this.resetResult(404)});
    }
  }

  resetResult(status:number) {
    switch(status) {
      case 200:
        Helpers.alert(this.$modal,
         'Votre demande de réinitialisation de mot de passe a été envoyée. Merci de vérifier votre boîte e-mail',
         'Demande envoyée')
        .then(() => {
          this.$root.$emit('toaster-success', 'Demande de changement de mot de passe envoyée', 10000);
          this.expandCollapse();
        });
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
      const toHides = document.querySelectorAll(this.tohideSelector);
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
    font-size: @fontsize-paragraph;
    line-height: calc(@fontsize-paragraph + @line-height-padding-large);
    margin-left: @margin-large;
    margin-right: @margin-large;
    margin-bottom:@margin-large;

    @media(max-height:579px) {
      margin-bottom:@margin-xx-large;
    }

    &.collapsed {
      // Hidden in collapsed mode
      &.keyboardShowing {
        display: none;
      }
    }
    &.expanded {
      &.keyboardShowing {
        position: absolute;
        top: calc(env(safe-area-inset-top));
        border-top-left-radius: @margin-large;
        border-top-right-radius: @margin-large;
        color: @gunmetal;
        width:100vw;
        height:200vh;
        padding-top:@margin-medium;
        padding-left:@margin-large;
        padding-right:@margin-large;
        margin-left: 0px;
        margin-right: 0px;
        margin-bottom: calc(-1 * @margin-small);
        background-color: @white-smoke;
      }
    }
  }
  .title {
    margin-bottom: @margin-medium;
    height: calc(@fontsize-title + @line-height-padding-xx-large);
    font-style: normal;
    font-weight: normal;
    font-size: @fontsize-title;
    line-height: calc(@fontsize-title + @line-height-padding-xx-large);
    color: @pelorous;
    text-align: center;

    @media(max-height:579px) {
      height: calc(@fontsize-title + @line-height-padding-large);
      line-height: calc(@fontsize-title + @line-height-padding-large);
      margin-top: @vertical-margin-xx-small;
      margin-bottom: @vertical-margin-xx-small;
    }

  }
  .sendpassword {
      height: 45px;
      width: 100%;

      display: flex;
      flex-direction: row;
      justify-content: space-between;
      align-items: center;

      &.keyboardShowing {
          margin-bottom: @vertical-margin-xx-small; 
      }
      button {
          height: 100%;
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
          &.cancel {
        
             border: 1px solid @pelorous;

            background-color: @white-smoke;
            color: @pelorous;
          }
      }
    }
</style>
