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
  <div class="maillage-legend">
    <span class="maillage" v-for="maillage in maillages" :key="maillage">
      <div
        class="square"
        :class="{
          'square-maillee': maillage != 'NON_MAILLEE',
          'square-non-maillee': maillage == 'NON_MAILLEE',
        }"
      ></div>
      <span v-if="maillage == 'MAILLEE'"> Poissons maillés </span>
      <span v-else-if="maillage == 'NON_MAILLEE'"> Poissons non maillés </span>
      <span class="line-break-on-small-screen" v-else-if="!selectedLakeUUID">
        Sans distrinction de maillage
        <i
          >(sélectionner un lac pour une meilleure visualisation du graphique)
        </i>
      </span>
      <span v-else> Sans taille réglementaire </span>
    </span>
  </div>
</template>

<script lang="ts">
import { Maillage } from "@/pojos/BackendPojos";
import { Component, Prop, Vue } from "vue-property-decorator";

@Component
export default class MaillageLegend extends Vue {
  @Prop() maillages: Maillage[];
  @Prop() selectedLakeUUID: string;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../../less/main";

.maillage-legend {
  display: flex;
  .maillage {
    padding-right: 20px;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    .square {
      width: 14px;
      height: 14px;
      &.square-maillee {
        background-color: @pelorous;
      }
      &.square-non-maillee {
        background-color: @carrot-orange;
      }
    }
  }

  @media screen and (max-width: 1400px) {
    margin-top: -20px;
    padding-bottom: 20px;
    .line-break-on-small-screen {
      flex-direction: column;
      align-items: flex-start;
      display: flex;
    }
  }
}
</style>
