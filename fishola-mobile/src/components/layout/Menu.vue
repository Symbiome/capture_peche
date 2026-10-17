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
  <div class="menu" :class="visibility">
    <div class="menu-title" v-on:click="goDispatcher">
      <CarnetLogo />
      <span v-if="envName" class="env">({{ envName }})</span>
    </div>
    <div class="close hide-on-desktop" v-on:click="closeMenu">
      <div class="plus">+</div>
    </div>
    <div class="items">
      <div class="new-trip hide-on-mobile" v-if="connected">
        <div class="button button-primary">
          <button v-on:click="startNewTrip">+ Nouvelle sortie</button>
        </div>
      </div>
      <div
        class="item"
        v-if="connected"
        v-on:click="goAccount"
        :class="isActive('account') ? 'active' : ''"
      >
        <span>
          {{ fullName }}
        </span>
        <Avatar v-if="initials" v-bind:initials="initials" />
        <div class="active-marker"></div>
      </div>

      <div
        class="item"
        v-for="i in availableMenuItems()"
        :key="'menu-item-' + i.name"
        v-on:click="i.clickHandler"
        :class="{ active: isActive(i.name), 'is-back': i.name == 'back' }"
      >
        <span>
          {{ i.label }}
        </span>
        <div class="pastille">
          <i :class="'icon-' + i.iconName" />
        </div>
        <div class="active-marker"></div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import ProfileService from "@/services/ProfileService";

import Helpers from "@/services/Helpers";

import Avatar from "@/components/common/Avatar.vue";
import CarnetLogo from "@/components/layout/CarnetLogo.vue";
import UserProfile from "@/pojos/UserProfile";
import LogoutService from "@/services/LogoutService";

import { RouterUtils } from "@/router/RouterUtils";

import { Component, Vue } from "vue-facing-decorator";
import EventBus from "@/services/EventBus";
import { Device } from "@capacitor/device";

export class MenuItem {
  constructor(
    public name: string,
    public label: string,
    public iconName: string,
    public clickHandler: any,
    public onlyConnected: boolean,
    public onlyUnlogged: boolean,
    public path?: string
  ) {}
}

@Component({
  components: {
    Avatar,
    CarnetLogo,
  },
})
export default class Menu extends Vue {
  envName?: string = import.meta.env.VITE__ENV_NAME;

  visibility: string = "menu-hidden";

  connected: boolean = false;

  menuItems: MenuItem[] = [];
  fullName: string = "";
  initials: string = "";

  created() {
    EventBus.on("profile-updated", this.loadProfile);
    EventBus.on("loggued-out", this.onLogguedOut);
    this.loadProfile();
  }

  mounted() {
    EventBus.on("open-menu", this.openMenu);
  }

  availableMenuItems() {
    let result = this.menuItems.filter(
      (item) =>
        (this.connected && !item.onlyUnlogged) ||
        (!this.connected && !item.onlyConnected)
    );
    return result;
  }

  beforeUnmount() {
    EventBus.off("profile-updated");
    EventBus.off("loggued-out");
    EventBus.off("open-menu");
  }

  async loadProfile() {
    try {
      let profile = await ProfileService.getProfile();

      // Notify user if it is an old user and he does not know it is possible
      // To receive notification par mail
      if (
        !profile.acceptsMailNotifications &&
        (!profile.lastNewsSeenDate ||
          // @ts-ignore
          !profile.lastNewsSeenDate[0] ||
          // @ts-ignore
          profile.lastNewsSeenDate[0] == 2021)
      ) {
        await this.askAcceptMailNotification(profile);
      }

      this.profileLoaded(profile);
    } catch (e) {
      this.connected = false;
    }
    this.refreshMenuItems();
  }

  private async askAcceptMailNotification(profile: UserProfile) {
    let acceptsMailNotifications = false;
    try {
      await Helpers.confirm(
        `Vous pouvez désormais suivre l'actualité FISHOLA par mail. Souhaitez-vous être informé par mail des communications autour de FISHOLA ? Vous pouvez à tout moment activer ou désactiver cette fonctionnalité dans votre Profil. `,
        "Du nouveau sur FISHOLA",
        "Non",
        "Oui"
      );
      acceptsMailNotifications = true;
    } catch (_e) {
      acceptsMailNotifications = false;
    }
    profile.acceptsMailNotifications = acceptsMailNotifications;
    // @ts-ignore
    profile.lastNewsSeenDate[0] = 2022;
    profile.lastNewsSeenDate = Helpers.parseLocalDateTime(
      // @ts-ignore
      profile.lastNewsSeenDate
    );
    if (!profile.lastNewsSeenDate) {
      profile.lastNewsSeenDate = new Date(2025, 6, 1);
    }
    ProfileService.saveProfile(profile);
  }

  back() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/about");
  }

  async profileLoaded(profile: UserProfile) {
    this.fullName = UserProfile.fullName(profile);
    this.initials = profile.initials;
    this.connected = true;
    this.refreshMenuItems();
  }

  openMenu() {
    this.visibility = "menu-visible";
  }

  closeMenu() {
    EventBus.emit("close-feedback");
    this.visibility = "menu-disappears";
  }

  openFeedback() {
    this.closeMenu();
    EventBus.emit("open-feedback");
  }

  goDispatcher() {
    this.closeMenu();
    RouterUtils.pushRouteNoDuplicate(this.$router, "/");
  }

  goTrips() {
    this.closeMenu();
    if (this.connected) {
      // Si on est sur application -> toujours trips
      Helpers.ifApplication(() => {
        RouterUtils.pushRouteNoDuplicate(this.$router, RouterUtils.homeRoute());
      });

      // Si on est sur navigateur et qu'on est connecté -> trips
      // Si on est sur navigateur et qu'on est pas connecté -> about
      Helpers.ifWeb(() => {
        if (this.connected) {
          RouterUtils.pushRouteNoDuplicate(
            this.$router,
            RouterUtils.homeRoute()
          );
        } else {
          RouterUtils.pushRouteNoDuplicate(this.$router, "/about");
        }
      });
    } else {
      RouterUtils.pushRouteNoDuplicate(
        this.$router,
        "/offline-home/presentation"
      );
    }
  }

  goAccount() {
    this.closeMenu();
    RouterUtils.pushRouteNoDuplicate(this.$router, "/account");
  }

  goPath(path: string) {
    this.closeMenu();
    RouterUtils.pushRouteNoDuplicate(this.$router, path);
  }

  startNewTrip() {
    RouterUtils.startNewTrip(this.$router);
  }

  goDashboardPersonal() {
    this.closeMenu();
    RouterUtils.pushRouteNoDuplicate(
      this.$router,
      "/dashboard-personal/dashboard"
    );
  }

  goDashboardGlobal() {
    this.closeMenu();
    RouterUtils.pushRouteNoDuplicate(
      this.$router,
      "/dashboard-global/dashboard"
    );
  }

  goDocumentation() {
    this.closeMenu();
    RouterUtils.pushRouteNoDuplicate(this.$router, "/documentation/doc");
  }

  goCommunity() {
    this.closeMenu();
    RouterUtils.pushRouteNoDuplicate(this.$router, "/community");
  }

  goBadges() {
    this.closeMenu();
    RouterUtils.pushRouteNoDuplicate(this.$router, "/badges");
  }

  logout() {
    this.closeMenu();
    LogoutService.confirmAndLogout(this.$router);
  }

  onLogguedOut() {
    this.connected = false;
    this.fullName = "";
    this.initials = "";
    this.refreshMenuItems();
  }

  isActive(name: string): boolean {
    const item = this.menuItems.find((i) => i.name == name);
    if (item && item.path) {
      return this.$route.path == item.path;
    }
    return this.$route.name == name;
  }

  async refreshMenuItems() {
    const homeShouldBeNameMyTrips =
      this.connected || (await Device.getInfo()).platform != "web";
    this.menuItems = [
      {
        name: "back",
        label: "Retour",
        iconName: "arrow icon-back",
        clickHandler: this.back,
        onlyConnected: false,
        onlyUnlogged: true,
      },
      {
        name: "home",
        label: "Accueil",
        iconName: "home",
        clickHandler: () => this.goPath("/home"),
        onlyConnected: true,
        onlyUnlogged: false,
        path: "/home",
      },
      {
        name: "map",
        label: "Carte",
        iconName: "map",
        clickHandler: () => this.goPath("/my-trips/map"),
        onlyConnected: true,
        onlyUnlogged: false,
        path: "/my-trips/map",
      },
      {
        name: "trips",
        label: "Mes prises",
        iconName: "fish",
        clickHandler: () => this.goPath("/my-trips/list"),
        onlyConnected: true,
        onlyUnlogged: false,
        path: "/my-trips/list",
      },
      {
        name: "offline-home",
        label: homeShouldBeNameMyTrips ? "Mes sorties" : "Accueil",
        iconName: "home",
        clickHandler: this.goTrips,
        onlyConnected: false,
        onlyUnlogged: true,
      },
      {
        name: "dashboard-personal",
        label: "Mes données",
        iconName: "dashboard",
        clickHandler: this.goDashboardPersonal,
        onlyConnected: true,
        onlyUnlogged: false,
      },
      {
        name: "community",
        label: "Communauté",
        iconName: "fishing",
        clickHandler: this.goCommunity,
        onlyConnected: true,
        onlyUnlogged: false,
      },
      {
        name: "badges",
        label: "Mes badges",
        iconName: "star",
        clickHandler: this.goBadges,
        onlyConnected: true,
        onlyUnlogged: false,
      },
      {
        name: "dashboard-global",
        label: "Statistiques",
        iconName: "stats",
        clickHandler: this.goDashboardGlobal,
        onlyConnected: true,
        onlyUnlogged: false,
      },
      {
        name: "documentationFaq",
        label: "Documentation",
        iconName: "files",
        clickHandler: this.goDocumentation,
        onlyConnected: false,
        onlyUnlogged: false,
      },
      {
        name: "feedback",
        label: "Des retours ?",
        iconName: "faq",
        clickHandler: this.openFeedback,
        onlyConnected: false,
        onlyUnlogged: false,
      },
      {
        name: "logout",
        label: "Déconnexion",
        iconName: "logout",
        clickHandler: this.logout,
        onlyConnected: true,
        onlyUnlogged: false,
      },
    ];
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@media screen and (max-width: @mobile-max-width) {
  .menu-hidden {
    left: calc(100vw);
  }

  .menu-disappears {
    animation-duration: 0.2s;
    animation-name: disappear;

    left: calc(100vw);

    @keyframes disappear {
      from {
        left: 0px;
      }

      to {
        left: calc(100vw);
      }
    }
  }

  .menu-visible {
    animation-duration: 0.2s;
    animation-name: appear;

    left: 0px;

    @keyframes appear {
      from {
        left: calc(100vw);
      }

      to {
        left: 0px;
      }
    }
  }
}

.menu {
  height: 100vh;
  z-index: 998; // Very high to avoid the map being over the menu

  display: flex;
  flex-direction: column;
  align-items: flex-end;

  @media screen and (max-width: @mobile-max-width) {
    position: fixed;
    top: 0px;
    width: 100vw;
    padding: @margin-menu-item;

    background-color: @carnet-blue;
    color: @white;

    .menu-title {
      display: none;
    }
  }

  @media screen and (min-width: @desktop-min-width) {
    width: @desktop-menu-width;
    flex-shrink: 0;
    padding: 0 0 @margin-medium 0;
    overflow-y: auto;

    background-color: @white-smoke;
    border-right: 2px solid @divider;
    color: @gunmetal;

    .menu-title {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: @margin-x-small;

      width: 100%;
      min-height: 112px;
      cursor: pointer;

      span.env {
        color: @accent-text;
        font-size: @fontsize-small-paragraph;
        font-weight: 600;
      }
    }
  }

  .close {
    display: flex;
    flex-direction: row-reverse;
    align-items: center;

    height: @pastille-size;
    margin: @vertical-margin-small;
    width: 100%;

    div.plus {
      font-size: @pastille-size;
      width: fit-content;
      transform: rotate(45deg);
    }
  }

  .new-trip {
    width: 100%;
    padding: 0 @margin-medium @margin-small @margin-medium;

    .button {
      width: 100%;
      margin: 0;
    }
  }

  .items {
    width: 100%;

    display: flex;
    flex-direction: column;
    align-items: flex-end;
    padding-right: @margin-menu-item;

    @media screen and (min-width: @desktop-min-width) {
      align-items: stretch;
      padding: 0 12px;
    }

    .item {
      height: 50px;
      display: flex;
      flex-direction: row;
      align-items: center;
      width: fit-content;
      cursor: pointer;

      @media (max-height: 600px) {
        height: 45px;
      }

      span {
        margin-right: @margin-medium;
        font-size: @fonsize-menu-item-span;
        font-weight: 600;
        line-height: calc(@fonsize-menu-item + @line-height-padding-large);
      }

      i {
        font-size: @fonsize-menu-item;
        width: 30px;
        text-align: center;
      }

      .active-marker {
        display: none;
      }

      @media screen and (min-width: @desktop-min-width) {
        flex-direction: row-reverse;
        justify-content: flex-end;
        gap: 12px;
        width: 100%;
        height: 48px;
        padding: 0 12px;
        border-radius: 10px;

        span {
          margin: 0;
          font-size: 15px;
        }

        .pastille {
          width: 30px;
          height: 30px;
          background: transparent;
          color: @accent-text;
        }

        i {
          font-size: 20px;
        }

        &:hover,
        &.active {
          background-color: @carnet-blue-soft;
        }

        &.active span {
          color: @accent-text;
        }

        &.is-back {
          background-color: @carnet-navy;
          color: @white;
        }
      }
    }
  }
}

:root[data-theme="dark"] .menu {
  @media screen and (min-width: @desktop-min-width) {
    .item.active span {
      color: @white;
    }
  }
}
</style>
