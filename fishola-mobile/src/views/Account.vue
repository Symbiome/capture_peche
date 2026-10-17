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
  <div class="account">
    <div class="account-content">
      <img class="account-deco hide-on-desktop" src="img/carnet/deco-fish.svg" alt="" />
      <div class="account-header hide-on-desktop">
        <CarnetLogo />
      </div>

      <h1 class="account-title">Compte</h1>

      <section class="account-section" v-for="section in sections" :key="section.title">
        <h2>{{ section.title }}</h2>
        <a
          class="account-row"
          v-for="item in section.items"
          :key="item.label"
          v-on:click="item.action"
        >
          <span class="row-icon">
            <img v-if="item.icon" :src="item.icon" alt="" />
            <i v-else :class="item.fontIcon" />
          </span>
          <span class="row-label">{{ item.label }}</span>
        </a>
      </section>
    </div>
    <BottomNav />
  </div>
</template>

<script lang="ts">
import DocumentationService from "@/services/DocumentationService";
import LogoutService from "@/services/LogoutService";
import { RouterUtils } from "@/router/RouterUtils";

import BottomNav from "@/components/layout/BottomNav.vue";
import CarnetLogo from "@/components/layout/CarnetLogo.vue";

import { Component, Vue } from "vue-facing-decorator";
import EventBus from "@/services/EventBus";

interface AccountItem {
  label: string;
  icon?: string;
  fontIcon?: string;
  action: () => void;
}

interface AccountSection {
  title: string;
  items: AccountItem[];
}

/** Compte : point d'entrée vers le profil, les données et l'aide. */
@Component({
  components: { BottomNav, CarnetLogo },
})
export default class AccountView extends Vue {
  get sections(): AccountSection[] {
    return [
      {
        title: "Mon espace",
        items: [
          { label: "Mon profil", icon: "img/carnet/account-profile.svg", action: () => this.go("/profile/profile") },
          { label: "Résumé de mon activité", icon: "img/carnet/account-activity.svg", action: () => this.go("/dashboard-global/dashboard") },
          { label: "Mes badges", fontIcon: "icon-star", action: () => this.go("/badges") },
          { label: "Données et confidentialité", icon: "img/carnet/account-privacy.svg", action: this.openCgu },
          { label: "Mode hors ligne", icon: "img/carnet/account-offline.svg", action: () => this.go("/offline-areas") },
          { label: "Paramètres", fontIcon: "icon-settings", action: () => this.go("/profile/settings") },
        ],
      },
      {
        title: "Communauté",
        items: [
          { label: "Actualités et partages", fontIcon: "icon-fishing", action: () => this.go("/community") },
          { label: "Statistiques du réseau", fontIcon: "icon-stats", action: () => this.go("/dashboard-global/dashboard") },
        ],
      },
      {
        title: "Le projet",
        items: [
          { label: "Pourquoi carnet de pêche ?", icon: "img/carnet/account-why.svg", action: () => this.go("/about") },
          { label: "Qui porte le projet ?", icon: "img/carnet/account-who.svg", action: () => this.go("/credits") },
        ],
      },
      {
        title: "Aide",
        items: [
          { label: "Contact", icon: "img/carnet/account-contact.svg", action: this.openFeedback },
          { label: "A propos", icon: "img/carnet/account-about.svg", action: () => this.go("/documentation/doc") },
          { label: "Déconnexion", fontIcon: "icon-logout", action: this.logout },
        ],
      },
    ];
  }

  go(path: string) {
    RouterUtils.pushRouteNoDuplicate(this.$router, path);
  }

  openCgu() {
    window.open(DocumentationService.getCGUUrl(), "_blank");
  }

  openFeedback() {
    EventBus.emit("open-feedback");
  }

  logout() {
    LogoutService.confirmAndLogout(this.$router);
  }
}
</script>

<style scoped lang="less">
.account {
  position: relative;
  height: 100%;
  background-color: @white-smoke;

  @media screen and (min-width: @desktop-min-width) {
    height: 100vh;
  }
}

.account-content {
  position: relative;
  height: calc(100% - @footer-height);
  overflow-y: auto;
  overflow-x: hidden;
  padding: env(safe-area-inset-top) 14px @margin-x-large 14px;

  @media screen and (min-width: @desktop-min-width) {
    height: 100%;
    max-width: 700px;
    padding: @margin-large @margin-large-desktop;
  }
}

.account-deco {
  position: absolute;
  top: 0;
  right: 0;
}

.account-header {
  padding: 10px 0 0 1px;
}

.account-title {
  margin: 39px 0 0 0;
  color: @accent-text;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
}

:root[data-theme="dark"] .account-title {
  color: @white;
}

.account-section {
  padding: 28px 0 12px 0;
  border-top: 2px solid @divider;

  &:first-of-type {
    padding-top: 22px;
    border-top: none;
  }

  h2 {
    margin: 0 0 10px 2px;
    color: @gunmetal;
    font-size: 18px;
    font-weight: 700;
    line-height: 1.2;
  }
}

.account-row {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding-left: 2px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;

  .row-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    flex-shrink: 0;

    i {
      color: @accent-text;
      font-size: 22px;
    }
  }

  .row-label {
    color: @gunmetal;
    font-size: 14px;
    font-weight: 600;
    line-height: 1.2;
  }

  &:hover .row-label {
    color: @accent-text;
  }
}
</style>
