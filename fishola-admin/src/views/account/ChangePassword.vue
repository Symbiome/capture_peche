<!--
  #%L
  Fishola :: Admin
  %%
  Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
    <h1 class="title">Changer mon mot de passe</h1>

    <b-field label="Mot de passe actuel" :type="fieldType('oldPassword')" :message="fieldMessage('oldPassword')">
      <b-input type="password" v-model="oldPassword" password-reveal></b-input>
    </b-field>
    <b-field label="Nouveau mot de passe" :type="fieldType('newPassword')" :message="fieldMessage('newPassword')">
      <b-input type="password" v-model="newPassword" password-reveal></b-input>
    </b-field>
    <b-field label="Confirmer le nouveau mot de passe"
             :type="confirmError ? 'is-danger' : ''"
             :message="confirmError">
      <b-input type="password" v-model="confirm" password-reveal></b-input>
    </b-field>

    <b-button type="is-primary" icon-left="lock-reset" :loading="loading" @click="submit">
      Mettre à jour
    </b-button>

    <b-notification v-if="success" type="is-success" class="mt-4" @close="success = false">
      Mot de passe mis à jour.
    </b-notification>
  </section>
</template>

<script setup lang="ts">
import BackendService from "@/services/BackendService";
import { ref, computed } from "vue";

const oldPassword = ref("");
const newPassword = ref("");
const confirm = ref("");
const loading = ref(false);
const success = ref(false);
const errors = ref<Record<string, string>>({});

const confirmError = computed(() =>
  confirm.value && confirm.value !== newPassword.value ? "Les deux mots de passe ne correspondent pas." : ""
);

function fieldType(field: string) {
  return errors.value[field] ? "is-danger" : "";
}
function fieldMessage(field: string) {
  return errors.value[field] || "";
}

async function submit() {
  errors.value = {};
  success.value = false;
  if (!oldPassword.value || !newPassword.value) {
    errors.value = { oldPassword: !oldPassword.value ? "Obligatoire" : "", newPassword: !newPassword.value ? "Obligatoire" : "" };
    return;
  }
  if (newPassword.value !== confirm.value) {
    return;
  }
  loading.value = true;
  try {
    await BackendService.backendPut("/v1/admin/password", {
      oldPassword: oldPassword.value,
      newPassword: newPassword.value
    });
    success.value = true;
    oldPassword.value = "";
    newPassword.value = "";
    confirm.value = "";
  } catch (rejected: any) {
    const content = rejected && rejected.content;
    if (content && typeof content === "object") {
      errors.value = content;
    } else {
      errors.value = { oldPassword: "Échec de la mise à jour (réessayer)." };
    }
  } finally {
    loading.value = false;
  }
}
</script>
