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
  <div class="verify-account page-with-header full-background">
    <div class="page verify-account-page">
      <div
        class="verify-account-title keyboardSensitive hiddenWhenKeyboardShows_SmallScreensOnly"
      >
        <div class="welcome keyboardSensitive">Bienvenue sur</div>
        <img
          class="logo keyboardSensitive"
          src="img/logo-big.svg"
          alt="FISHOLA"
        />
      </div>
      <div class="pending" v-if="pending">
        <p>Validation de votre compte en cours, veuillez patientier...</p>
        <div class="spinner">&nbsp;</div>
      </div>
      <div v-if="!pending && !verifySuccess" class="verify-account-form error">
        <p>Une erreur est survenue pendant la vérification de votre email.</p>
        <p>Merci de recommencer votre inscription.</p>
      </div>
      <div v-if="!pending && verifySuccess" class="verify-account-form success">
        <p>Votre email a bien été vérifié.</p>
        <p>Vous pouvez dès à présent vous connecter sur la page de Login.</p>
      </div>
      <div class="ok">
        <button v-on:click="backToLogin">OK</button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import ProfileService from "@/services/ProfileService";

import { Component, Prop, Vue } from "vue-property-decorator";
import router from "../router";
import { RouterUtils } from "../router/RouterUtils";

@Component
export default class VerifyAccount extends Vue {
  @Prop() token!: string;

  private pending: boolean = true;
  private verifySuccess: boolean = false;

  mounted() {
    // Send password reset request to server
    ProfileService.verifyAccount(this.token).then(
      () => {
        this.verifySuccess = true;
        this.pending = false;
      },
      () => {
        this.verifySuccess = false;
        this.pending = false;
      }
    );
  }

  backToLogin() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/login");
  }
}
</script>
<style lang="less">
.page-with-header {
  .verify-account-page {
    display: flex;
    flex-direction: column;
    justify-content: space-evenly;
    padding: @margin-large;
    text-align: center;

    .verify-account-title {
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
        line-height: calc(
          @fontsize-header-title + @line-height-padding-xxx-large
        );
        &.keyboardShowing {
          font-size: @fontsize-title-keyboardshowing;
          line-height: calc(
            @fontsize-title-keyboardshowing + @line-height-padding-small
          );
        }
      }
      .logo {
        height: 100px;
        &.keyboardShowing {
          height: 65px;
        }
      }
    }

    .verify-account-form {
      padding-left: @margin-medium;
      padding-right: @margin-medium;
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
      margin-left: @margin-large;
      margin-right: @margin-large;
      margin-bottom: @vertical-margin-medium;
      display: flex;
      justify-content: space-around;
      &.keyboardShowing {
        margin-bottom: @vertical-margin-xx-small;
      }
      button {
        height: 100%;
        width: 50%;
        border-radius: 50px;

        font-style: normal;
        font-weight: bold;
        font-size: @fontsize-button;
        line-height: calc(@fontsize-button + @line-height-padding-x-large);

        border: 0px;
        padding-left: @margin-medium;
        padding-right: @margin-medium;
        margin-left: @margin-medium;
        margin-right: @margin-medium;

        background-color: @terra-cotta;
        color: @white;
      }
    }
    .pending {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      @keyframes spin {
        100% {
          -webkit-transform: rotate(360deg);
          transform: rotate(360deg);
        }
      }

      .spinner {
        height: 60px;
        width: 60px;
        border-radius: 50%;
        border-top: 3px solid @white;
        border-left: 3px solid @white;
        animation: spin 2s linear infinite;
      }
    }
  }
}
</style>
