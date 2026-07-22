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
  <section class="section">
    <div class="container">
      <div class="columns is-centered">
        <div class="column is-6-tablet is-5-desktop is-4-widescreen">
          <div class="box">
            <h1 class="title is-4 has-text-primary">Changer mon mot de passe</h1>

            <div v-if="successMessage" class="notification is-success is-light">{{ successMessage }}</div>
            <div v-if="generalError" class="notification is-danger is-light">{{ generalError }}</div>

            <b-field label="Mot de passe actuel"
                     :type="oldError ? 'is-danger' : ''" :message="oldError">
              <b-input icon="lock" type="password" password-reveal v-model="oldPassword" />
            </b-field>

            <b-field label="Nouveau mot de passe"
                     :type="newError ? 'is-danger' : ''" :message="newError">
              <b-input icon="lock" type="password" password-reveal v-model="newPassword" />
            </b-field>

            <b-field label="Confirmer le nouveau mot de passe"
                     :type="confirmError ? 'is-danger' : ''" :message="confirmError">
              <b-input icon="lock" type="password" password-reveal v-model="confirmPassword" />
            </b-field>

            <div class="buttons">
              <b-button type="is-primary" :loading="loading" @click="doChange">Enregistrer</b-button>
              <b-button @click="cancel">Annuler</b-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import router from "@/router";
import BackendService from "@/services/BackendService";
import {BButton, BField, BInput} from "buefy";
import {ref} from "vue";

const oldPassword = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const oldError = ref("");
const newError = ref("");
const confirmError = ref("");
const generalError = ref("");
const successMessage = ref("");
const loading = ref(false);

function cancel() {
  router.push({name: "home"});
}

async function doChange() {
  oldError.value = "";
  newError.value = "";
  confirmError.value = "";
  generalError.value = "";
  successMessage.value = "";

  if (!oldPassword.value) {
    oldError.value = "Champ requis";
    return;
  }
  if (!newPassword.value) {
    newError.value = "Champ requis";
    return;
  }
  if (newPassword.value !== confirmPassword.value) {
    confirmError.value = "Les deux mots de passe ne correspondent pas";
    return;
  }

  loading.value = true;
  try {
    await BackendService.backendPut("/v1/admin/password", {
      oldPassword: oldPassword.value,
      newPassword: newPassword.value
    });
    successMessage.value = "Mot de passe modifié.";
    oldPassword.value = "";
    newPassword.value = "";
    confirmPassword.value = "";
  } catch (err: any) {
    if (err.status === 400 && err.content) {
      oldError.value = err.content.oldPassword ?? "";
      newError.value = err.content.newPassword ?? "";
      if (!err.content.oldPassword && !err.content.newPassword) {
        generalError.value = "Requête invalide.";
      }
    } else if (err.status === 401) {
      generalError.value = "Session expirée — reconnectez-vous.";
      await router.push({name: "login"});
    } else {
      generalError.value = "Une erreur est survenue lors de la modification.";
    }
  } finally {
    loading.value = false;
  }
}
</script>
