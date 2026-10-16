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
  <nav class="bottom-nav keyboardSensitive hiddenWhenKeyboardShows hide-on-desktop">
    <a
      v-for="item in leftItems"
      :key="item.name"
      class="nav-item"
      :class="'nav-item-' + item.name"
      :aria-current="isActive(item) ? 'page' : null"
      v-on:click="go(item)"
    >
      <span class="nav-icon"><img :src="item.icon" alt="" /></span>
      <span class="nav-label">{{ item.label }}</span>
    </a>

    <button
      class="nav-action"
      :class="{ 'delete-mode': deleteMode }"
      :aria-label="deleteMode ? 'Supprimer' : 'Nouvelle sortie'"
      v-on:click="actionClicked"
    >
      <i v-if="deleteMode" class="icon-delete" />
      <span v-else>+</span>
    </button>

    <a
      v-for="item in rightItems"
      :key="item.name"
      class="nav-item"
      :class="'nav-item-' + item.name"
      :aria-current="isActive(item) ? 'page' : null"
      v-on:click="go(item)"
    >
      <span class="nav-icon"><img :src="item.icon" alt="" /></span>
      <span class="nav-label">{{ item.label }}</span>
    </a>
  </nav>
</template>

<script lang="ts">
import { RouterUtils } from "@/router/RouterUtils";

import { Component, Prop, Vue } from "vue-facing-decorator";

export interface BottomNavItem {
  name: string;
  label: string;
  icon: string;
  route: string;
}

/**
 * Barre de navigation principale (mobile) : Accueil, Carte, nouvelle
 * sortie, Mes prises, Compte. Le bouton central passe en suppression
 * quand la page appelante l'indique (sélection de sorties).
 */
@Component({ emits: ["delete"] })
export default class BottomNav extends Vue {
  @Prop({ default: false }) deleteMode: boolean;

  leftItems: BottomNavItem[] = [
    { name: "home", label: "Accueil", icon: "img/carnet/nav-home.svg", route: "/home" },
    { name: "map", label: "Carte", icon: "img/carnet/nav-map.svg", route: "/my-trips/map" },
  ];

  rightItems: BottomNavItem[] = [
    { name: "catches", label: "Mes prises", icon: "img/carnet/nav-fish.svg", route: "/my-trips/list" },
    { name: "account", label: "Compte", icon: "img/carnet/nav-user.svg", route: "/account" },
  ];

  isActive(item: BottomNavItem): boolean {
    return this.$route.path === item.route;
  }

  go(item: BottomNavItem) {
    RouterUtils.pushRouteNoDuplicate(this.$router, item.route);
  }

  actionClicked() {
    if (this.deleteMode) {
      this.$emit("delete");
    } else {
      RouterUtils.startNewTrip(this.$router);
    }
  }
}
</script>

<style scoped lang="less">
.bottom-nav {
  position: absolute;
  bottom: 0;
  left: 0;
  z-index: 10;

  display: grid;
  grid-template-columns: 1fr 1fr 80px 1fr 1fr;
  align-items: start;

  width: 100vw;
  height: @footer-height;
  padding: 13px 20px env(safe-area-inset-bottom) 20px;

  background-color: @carnet-blue;

  .nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;

    cursor: pointer;
    text-decoration: none;
    -webkit-tap-highlight-color: transparent;
  }

  .nav-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 30px;
  }

  .nav-item-catches .nav-icon img {
    transform: rotate(-45deg);
  }

  .nav-label {
    color: @white;
    font-size: 11px;
    font-weight: 600;
    line-height: 1.2;
    letter-spacing: @letter-spacing;
    white-space: nowrap;
  }

  .nav-action {
    justify-self: center;
    margin-top: -45px;

    display: flex;
    align-items: center;
    justify-content: center;

    width: 63px;
    height: 63px;
    padding: 0;
    border: 4px solid @white-smoke;
    border-radius: 31.5px;

    background-color: @carnet-blue;
    color: @white;
    font-family: @font-family;
    font-size: 41.745px;
    font-weight: 600;
    line-height: 1.2;

    span {
      margin-top: -2px;
    }

    i {
      font-size: @fontsize-button-big;
    }

    &.delete-mode {
      background-color: @cardinal;
    }

    &:active {
      animation: none;
    }
  }
}
</style>
