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
  <div class="login">
    <p>Interface d'administration de FISHOLA.</p>
    <div class="field is-grouped">
      <p class="control is-expanded">
        <b-field
          :type="errorMessage ? 'is-danger' : ''"
          :message="errorMessage"
        >
          <b-input type="password" v-model="password"> </b-input>
        </b-field>
      </p>
      <p class="control">
        <button class="button is-primary" @click="doLogin()">
          Connexion
        </button>
      </p>
    </div>
  </div>
</template>

<script lang="ts">
import router from "@/router";

import BackendService from "@/services/BackendService";

import { Component, Prop, Vue } from "vue-property-decorator";

@Component({
  components: {}
})
export default class LoginView extends Vue {
  password = "";
  errorMessage = "";

  constructor() {
    super();
  }

  doLogin() {
    this.errorMessage = "";
    BackendService.backendPost("/v1/security/admin-login", {
      password: this.password
    }).then(
      () => {
        router.push("home");
      },
      err => {
        if (err.status == 401) {
          this.errorMessage = "Mot de passe incorrect";
        } else {
          this.errorMessage = err;
        }
      }
    );
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../less/main";

.login {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  p {
    font-size: 22px;
    margin: 5px;
  }
}
</style>
