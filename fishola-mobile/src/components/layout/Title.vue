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
  <div class="header-title" v-on:click="goHome">
    <img src="img/logo-small.svg" alt="FISHOLA" />
    <span v-if="envName" class="env">({{ envName }})</span>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";

import { RouterUtils } from "@/router/RouterUtils";

@Component
export default class Title extends Vue {
  envName?: string = import.meta.env.VITE__ENV_NAME;

  goHome() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/my-trips/list");
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../../less/main";

.header-title {
  font-size: @fontsize-header-title;

  img {
    height: calc(@fontsize-header-title + 6px);
  }

  span.env {
    color: @terra-cotta;
    font-size: @fontsize-paragraph;
  }

  @media (max-width: 340px) {
    img {
      height: 20px;
      margin-top: 5px;
    }

    span.env {
      font-size: @fontsize-small-paragraph;
    }
  }
}
</style>
