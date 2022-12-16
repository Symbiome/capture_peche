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
      <div>
        <img src="img/logo-small.svg" alt="FISHOLA" />
        <span v-if="envName" class="env">({{ envName }})</span>
      </div>
    </div>
    <div class="close hide-on-desktop" v-on:click="closeMenu">
      <div class="plus">+</div>
    </div>
    <div class="items">
      <div
        class="item"
        v-if="connected"
        v-on:click="goProfile"
        :class="isActive('profile') ? 'active' : ''"
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
        :class="isActive(i.name) ? 'active' : ''"
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
import TripsService from "@/services/TripsService";

import Helpers from "@/services/Helpers";

import Avatar from "@/components/common/Avatar.vue";
import UserProfile from "@/pojos/UserProfile";

import router from "@/router";

import { Component, Prop, Vue } from "vue-property-decorator";

export class MenuItem {
  constructor(
    public name: string,
    public label: string,
    public iconName: string,
    public clickHandler: any,
    public onlyConnected: boolean
  ) {}
}

@Component({
  components: {
    Avatar,
  },
})
export default class Menu extends Vue {
  envName?: string = process.env.VUE_APP_ENV;

  visibility: string = "menu-hidden";

  connected: boolean = false;

  menuItems: MenuItem[] = [
    {
      name: "trips",
      label: "Accueil",
      iconName: "home",
      clickHandler: this.goHome,
      onlyConnected: false,
    },
    {
      name: "dashboard",
      label: "Tableau de bord",
      iconName: "dashboard",
      clickHandler: this.goDashboard,
      onlyConnected: true,
    },
    {
      name: "settings",
      label: "Paramètres",
      iconName: "settings",
      clickHandler: this.goSettings,
      onlyConnected: true,
    },
    {
      name: "documentation",
      label: "Documentation",
      iconName: "files",
      clickHandler: this.goDocumentation,
      onlyConnected: false,
    },
    {
      name: "credits",
      label: "Infos / Crédits",
      iconName: "info",
      clickHandler: this.goCredits,
      onlyConnected: false,
    },
    {
      name: "feedback",
      label: "Des retours ?",
      iconName: "faq",
      clickHandler: this.openFeedback,
      onlyConnected: false,
    },
    {
      name: "logout",
      label: "Déconnexion",
      iconName: "logout",
      clickHandler: this.logout,
      onlyConnected: true,
    },
  ];

  fullName: string = "";
  initials: string = "";

  created() {
    this.$root.$on("profile-updated", this.loadProfile);
    this.$root.$on("loggued-out", this.onLogguedOut);
    this.loadProfile();
  }

  mounted() {
    this.$root.$on("open-menu", this.openMenu);
  }

  availableMenuItems() {
    let result = this.menuItems.filter(
      (item) => this.connected || !item.onlyConnected
    );
    return result;
  }

  beforeDestroy() {
    this.$root.$off("profile-updated");
    this.$root.$off("loggued-out");
    this.$root.$off("open-menu");
  }

  async loadProfile() {
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
      let acceptsMailNotifications = false;
      try {
        await Helpers.confirm(
          this.$modal,
          `Vous pouvez désormais suivre l'actualité Fishola par mail. Souhaitez-vous être informé par mail des communications autour de Fishola ? Vous pouvez à tout moment activer ou désactiver cette fonctionnalité dans votre Profil. `,
          "Du nouveau sur Fishola",
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
      ProfileService.saveProfile(profile);
    }

    this.profileLoaded(profile);
  }

  profileLoaded(profile: UserProfile) {
    this.fullName = UserProfile.fullName(profile);
    this.initials = profile.initials;
    this.connected = true;
  }

  openMenu() {
    this.visibility = "menu-visible";
  }

  closeMenu() {
    this.$root.$emit("close-feedback");
    this.visibility = "menu-disappears";
  }

  openFeedback() {
    this.closeMenu();
    this.$root.$emit("open-feedback");
  }

  goDispatcher() {
    this.closeMenu();
    router.push("/");
  }

  goHome() {
    this.closeMenu();
    // Si on est sur application -> toujours trips
    Helpers.ifApplication(() => {
      router.push("/trips");
    });

    // Si on est sur navigateur et qu'on est connecté -> trips
    // Si on est sur navigateur et qu'on est pas connecté -> about
    Helpers.ifWeb(() => {
      if (this.connected) {
        router.push("/trips");
      } else {
        router.push("/about");
      }
    });
  }

  goProfile() {
    this.closeMenu();
    router.push("/profile");
  }

  goDashboard() {
    this.closeMenu();
    router.push("/dashboard");
  }

  goDocumentation() {
    this.closeMenu();
    router.push("/documentation/doc");
  }

  goSettings() {
    this.closeMenu();
    router.push("/settings");
  }

  goCredits() {
    this.closeMenu();
    router.push("/credits");
  }

  logout() {
    Helpers.confirm(
      this.$modal,
      "Voulez-vous vous déconnecter ?",
      "Déconnexion"
    ).then(() => {
      this.logoutConfirmed(false);
    });
  }

  promptLogoutWithRunningTrip() {
    Helpers.confirm(
      this.$modal,
      "Vous avez une sortie en cours, elle sera perdue. Êtes-vous sûr\u00B7e ?",
      "Déconnexion"
    ).then(() => {
      this.logoutConfirmed(true);
    });
  }

  logoutConfirmed(ignoreRunningTrip: boolean) {
    this.closeMenu();
    if (ignoreRunningTrip) {
      ProfileService.logout().then(this.logguedOut);
    } else {
      TripsService.hasRunningTrip().then((hasRunningTrip: boolean) => {
        if (hasRunningTrip) {
          this.promptLogoutWithRunningTrip();
        } else {
          ProfileService.logout().then(this.logguedOut);
        }
      });
    }
  }

  logguedOut() {
    router.push("/login");
    this.onLogguedOut();
  }

  onLogguedOut() {
    this.connected = false;
    this.fullName = "";
    this.initials = "";
  }

  isActive(name: string): boolean {
    return this.$route.name == name;
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../../less/main";

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
  background-color: @pelorous;

  @media screen and (max-width: @mobile-max-width) {
    position: fixed;
    top: 0px;
    width: 100vw;

    .menu-title {
      display: none;
    }
  }

  @media screen and (min-width: @desktop-min-width) {
    width: @desktop-menu-width;

    .menu-title {
      height: 96px;
      width: 100%;

      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      cursor: pointer;

      img {
        height: calc(@fontsize-header-title + 20px);
      }
      span.env {
        color: @terra-cotta;
        font-size: @fontsize-paragraph;
      }
    }
  }

  height: 100vh;
  z-index: 100;

  display: flex;
  flex-direction: column;
  align-items: flex-end;

  color: @white;

  padding: @margin-menu-item;

  @media screen and (min-width: @desktop-min-width) {
    padding: 0px;
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

  .items {
    width: 100%;

    display: flex;
    flex-direction: column;
    align-items: flex-end;

    @media screen and (min-width: @desktop-min-width) {
      align-items: flex-start;
    }

    padding-right: @margin-menu-item;

    .item {
      height: 50px;

      @media (max-height: 600px) {
        height: 45px;
      }

      display: flex;
      flex-direction: row;
      // justify-content: center;
      align-items: center;

      cursor: pointer;

      width: fit-content;

      .active-marker {
        height: 100%;
        width: 4px;
        border-top-right-radius: 4px;
        border-bottom-right-radius: 4px;
        margin-right: 23px;

        @media screen and (max-width: @mobile-max-width) {
          display: none;
        }
      }

      @media screen and (min-width: @desktop-min-width) {
        flex-direction: row-reverse;
        height: 72px;

        .pastille {
          width: 40px;
          height: 40px;
        }
        &.active {
          .active-marker {
            background-color: @white;
          }
          .pastille {
            color: @pelorous;
            background: @white;
          }
        }
        &:hover {
          span {
            font-weight: bold;
          }
        }
      }
      span {
        margin-right: @margin-medium;

        font-size: @fonsize-menu-item-span;
        font-weight: bold;

        @media screen and (min-width: @desktop-min-width) {
          margin-left: 13px;
          margin-right: unset;
          font-size: 18px;
          font-weight: normal;
        }

        line-height: calc(@fonsize-menu-item + @line-height-padding-large);
      }

      i {
        font-size: @fonsize-menu-item;
        width: 30px;

        @media screen and (min-width: @desktop-min-width) {
          width: 40px;
        }

        text-align: center;
      }
    }
  }
}
</style>
