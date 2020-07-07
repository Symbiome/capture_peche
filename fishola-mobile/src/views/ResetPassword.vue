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
    <div class="password-reset page-with-header full-background">
      <div class="page password-reset-page">
        <div class="password-reset-title keyboardSensitive hiddenWhenKeyboardShows_SmallScreensOnly">
          <div class="welcome keyboardSensitive">Bienvenue sur</div>
          <img class="logo keyboardSensitive" src="img/logo-big.svg" alt="FISHOLA"/>
        </div>
        <div class="pending" v-if="pending">
            <p>Réinitialisation du mot de passe en cours, veuillez patientier...</p>
            <div class="spinner">&nbsp;</div>
        </div>
        <div v-if="!pending && !reinitSuccess" class="password-reset-form error">
            <p>Votre demande de réinitialisation de mot de passe a expiré.</p>
            <p>Merci de renouveler votre demande de réinitialisation.</p>
        </div>
        <div v-if="!pending && reinitSuccess" class="password-reset-form success">
            <p>Votre mot de passe a bien été modifié.</p>
            <p>Vous pouvez dès à présent vous connecter avec votre nouveau mot de passe.</p>
        </div>
        <div class="ok">
          <button v-on:click="backToLogin">OK</button>
        </div>
      
      </div>
    </div>
</template>

<script lang="ts">
import Constants from '@/services/Constants';
import Helpers from '@/services/Helpers';
import ProfileService from '@/services/ProfileService';

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../router';

@Component
export default class ResetPassword extends Vue {
  @Prop() token!:string;

  private pending: boolean = true;
  private reinitSuccess: boolean = false;

  mounted() {
    // Send password reset request to server
      ProfileService
        .resetPassword(this.token)
        .then(() => { 
          this.reinitSuccess = true;
          this.pending = false;
        },
        () => { 
          this.reinitSuccess = false;
          this.pending = false;
        });
  }

  backToLogin() {
    router.push('/login');
  }
}
</script>
<style lang="less">

@import "../less/main";

.page-with-header {
  .password-reset-page {
    display: flex;
    flex-direction: column;
    justify-content: space-evenly;
    padding:30px;
    text-align:center;

    .password-reset-title {
      height: 140px;
      &.keyboardShowing {
        margin-top: calc(2 * env(safe-area-inset-top));
        height: 81px;
      }
      display: flex;
      flex-direction: column;
      justify-content: center;

      .welcome {
        font-size: @fontsize-header-title;
        line-height: calc(@fontsize-header-title + 9px);
        &.keyboardShowing {
          font-size: @fontsize-title-keyboardshowing;
          line-height: calc(@fontsize-title-keyboardshowing + 2px);
        }
      }
      .logo {
        height: 100px;
        &.keyboardShowing {
          height: 65px;
        }
      }
    }

    .password-reset-form {
      padding-left: 20px;
      padding-right: 20px;
      border-radius: 10px;
      &.error {
        background-color: @cardinal;
      }

      &.success {
        background-color: @lime-green;
      }
    }

     .ok {
      height: 45px;
      margin-left: 30px;
      margin-right: 30px;
      margin-bottom: 20px;
      display:flex;
      justify-content: space-around;
      &.keyboardShowing {
          margin-bottom: 5px; 
      }
      button {
          height: 100%;
          width:50%;
          border-radius: 50px;

          font-style: normal;
          font-weight: bold;
          font-size: @fontsize-button;
          line-height: calc(@fontsize-button + 7px);

          border: 0px;
          padding-left: 20px;
          padding-right: 20px;
          margin-left:20px;
          margin-right:20px;

          background-color: @terra-cotta;
          color: @white;
      }
    }
    .pending {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      @keyframes spin { 100% { -webkit-transform: rotate(360deg); transform:rotate(360deg); } }

      .spinner {
        height: 60px;
        width: 60px;
        border-radius: 50%;
        border-top: 3px solid @white;
        border-left: 3px solid @white;
        animation:spin 2s linear infinite;
      }
    }
  }
}
</style>