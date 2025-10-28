<!--
  #%L
  Fishola :: Admin
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
  <div class="lakes">
    <b-button type="is-primary" @click="copyMails" class="contact-button">
      Copier les emails de tous les utilisateurs acceptant d'être contactés
    </b-button>
    <Referential
      name="Utilisateurs"
      url="/v1/security/users"
      :columns="userColumns"
      @elementsLoaded="usersLoaded"
      :canDelete="true"
    ></Referential>
  </div>
</template>

<script lang="ts">
import Referential from "@/components/Referential.vue";

import { Component, Vue } from "vue-property-decorator";

@Component({
  components: {
    Referential
  }
})
export default class UsersVue extends Vue {
  userEmails = "";
  userColumns: any[] = [
    {
      field: "id",
      label: "Identifiant",
      visible: false,
      readOnly: true
    },
    {
      field: "firstName",
      label: "Prénom",
      readOnly: true,
      searchable: true,
    },
    {
      field: "lastName",
      label: "Nom",
      readOnly: true,
      searchable: true,
    },
    {
      field: "email",
      label: "E-mail",
      readOnly: true,
      searchable: true,
    },
    {
      field: "gender",
      label: "Genre",
      readOnly: true,
      searchable: true,
      visible: false,
    },
    {
      field: "birthYear",
      label: "Année de naissance",
      visible: false,
      readOnly: true
    },
    {
      field: "excludeFromExports",
      label: "Exclu des exports",
      isABoolean: true
    },
    {
      field: "createdOn",
      label: "Date de création",
      isADate: true,
      readOnly: true
    }
  ];

  usersLoaded(zeUsers: any[]) {
    this.userEmails = zeUsers
      .filter(u => u.acceptsEmailNotifications)
      .map(u => u.email)
      .join(";");
  }

  copyMails() {
    navigator.clipboard.writeText(this.userEmails);
    const nbUsers = this.userEmails.split(";").length;
    this.$buefy.toast.open({
      message:
        "Les emails des " +
        nbUsers +
        " utilisateurs acceptant d'être contactés par mails ont été copiés dans votre presse-papier. Vous pouvez les coller directement dans le champ 'destinataire' de votre email.",
      type: "is-success",
      duration: 7000
    });
  }
}
</script>

<style scoped lang="less">

.contact-button {
  position: absolute;
  right: 40px;
  top: 80px;
  z-index: 2;
  box-shadow: 0 0 5px 15px #fff;
}
</style>
