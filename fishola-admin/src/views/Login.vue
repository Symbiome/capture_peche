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
   <section class="section is-fullheight has-background-light login">
    <div class="container ">
      <div class="columns is-centered ">
        <div class="column is-5-tablet is-4-desktop is-3-widescreen login-box">
          <div class="box has-text-centered">
            <h1 class="title is-4 has-text-primary">Interface d'administration de FISHOLA</h1>

            <b-field label="Email"
              :type="errorMessage ? 'is-danger' : ''"
              :message="errorMessage">
              <b-input
                icon="email"
                type="email"
                placeholder="Entrez votre email"
                v-model="email"
              />
            </b-field>

            <b-field
              label="Mot de passe"
              :type="passwordErrorMessage ? 'is-danger' : ''"
              :message="passwordErrorMessage"
            >
              <b-input
                icon="lock"
                type="password"
                placeholder="Entrez votre mot de passe"
                password-reveal
                v-model="password"
              />
            </b-field>

            <b-button type="is-primary" expanded @click="doLogin">
              Connexion {{ errorMessage}}
            </b-button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script lang="ts">
import router from "@/router";

import BackendService from "@/services/BackendService";

import { Component, Prop, Vue } from "vue-property-decorator";

@Component({
  components: {}
})
export default class LoginView extends Vue {
  email = localStorage.getItem("last-fishola-admin-email") ?? "";
  password = "";
  errorMessage = "";
  passwordErrorMessage = "";

  constructor() {
    super();
  }

  doLogin() {
    this.errorMessage = "";
    this.passwordErrorMessage = "";
    if (!this.email || this.email.length <5 || this.email.indexOf('@') == -1) {
      this.errorMessage = "Email incorrect"
    } else {
      BackendService.backendPost("/v1/admin/login", {
        email: this.email,
        password: this.password
      }).then(
        () => {
          localStorage.setItem("last-fishola-admin-email", this.email);
          router.push("home");
        },
        err => {
          if (err.status == 401) {
            this.passwordErrorMessage = "Mot de passe incorrect";
          } else {
            this.errorMessage = err.message ?? "Veuillez vérifier votre email et mot de passe";
          }
        }
      );
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../less/main";

.login {
  height: 100%;
  .login-box {
    min-width: 50vw;
    padding-top: 20vh;
    margin:auto;
  }
}

.box {
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
</style>
