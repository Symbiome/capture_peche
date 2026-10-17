<!--
  #%L
  Carnet de pêche :: Admin
  %%
  Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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
    <h1 class="title">Mes référentiels</h1>
    <p class="subtitle is-6">
      Choisissez les éléments du référentiel national proposés dans vos listes de saisie. Un élément masqué reste
      affiché sur les données existantes, reste accepté à l'import de fichiers et peut toujours être choisi via
      « Afficher tout le référentiel » dans la liste. Un nouvel élément national est affiché par défaut.
    </p>

    <b-tabs v-model="activeTab">
      <b-tab-item label="Espèces" value="species">
        <HiddenReferentialEditor type="species" :uri="SPECIES_URI" item-label="espèces" />
      </b-tab-item>
      <b-tab-item label="Techniques de pêche" value="technique">
        <HiddenReferentialEditor type="technique" :uri="TECHNIQUES_URI" item-label="techniques" />
      </b-tab-item>
    </b-tabs>
  </section>
</template>

<script setup lang="ts">
import HiddenReferentialEditor from "@/components/HiddenReferentialEditor.vue";
import { SPECIES_URI, TECHNIQUES_URI } from "@/utils/entryReferentials";
import { ref } from "vue";

const activeTab = ref("species");
</script>
