<!--
  #%L
  Fishola :: Mobile
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
  <div class="carnet-logo" role="img" aria-label="Carnet de pêche">
    <img class="logo-mark" src="img/carnet/logo-wordmark-mark.svg" alt="" />
    <img class="logo-text logo-text-light" src="img/carnet/logo-wordmark-text.svg" alt="" />
    <img class="logo-text logo-text-dark" src="img/carnet/logo-wordmark-text-dark.svg" alt="" />
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";

/** Logo « Carnet de pêche » (pictogramme + nom), décliné selon le thème. */
@Component
export default class CarnetLogo extends Vue {}
</script>

<style scoped lang="less">
.carnet-logo {
  position: relative;
  width: 180px;
  height: 56px;
  overflow: hidden;
  flex-shrink: 0;

  .logo-mark {
    position: absolute;
    top: 0;
    left: 0;
  }

  .logo-text {
    position: absolute;
    top: 3.26%;
    left: 32.7%;
  }

  .logo-text-dark {
    display: none;
  }
}

:root[data-theme="dark"] .carnet-logo {
  .logo-text-light {
    display: none;
  }

  .logo-text-dark {
    display: block;
  }
}

:root[data-theme="dark"] .force-light-theme .carnet-logo {
  .logo-text-light {
    display: block;
  }

  .logo-text-dark {
    display: none;
  }
}

.carnet-logo-centered {
  margin: 0 auto;
}
</style>
