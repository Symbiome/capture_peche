<!--
  #%L
  Carnet de pêche :: Mobile
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
  <div class="header-title" v-on:click="goHome">
    <img src="img/carnet/logo-mark.svg" alt="Carnet de pêche" />
    <span v-if="envName" class="env">({{ envName }})</span>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-facing-decorator";

import { RouterUtils } from "@/router/RouterUtils";

@Component
export default class Title extends Vue {
  envName?: string = import.meta.env.VITE__ENV_NAME;

  goHome() {
    RouterUtils.pushRouteNoDuplicate(this.$router, RouterUtils.homeRoute());
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">


.header-title {
  display: flex;
  align-items: center;
  cursor: pointer;

  img {
    display: block;
  }

  span.env {
    margin-left: @margin-small;
    color: @accent-text;
    font-size: @fontsize-small-paragraph;
    font-weight: 600;
  }
}
</style>
