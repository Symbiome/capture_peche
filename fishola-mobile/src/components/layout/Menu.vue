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
  <div class="menu" v-bind:class="visibility">
      <div class="menu-title" v-on:click="goHome">
        <div>
          <img src="img/logo-small.svg" alt="FISHOLA" />
          <span v-if="envName" class="env">({{envName}})</span>
        </div>
      </div>
      <div class="close"
           v-on:click="closeMenu">
           <div class="plus">+</div> 
      </div>
      <div class="items">

        <div class="item" v-if="connected" v-on:click="goProfile" :class="isActive('profile') ? 'active' : ''">
          <span>
            {{fullName}}
          </span>
          <Avatar v-if="initials" v-bind:initials="initials"/>
          <div class="active-marker"></div>
        </div>

        <div class="item" v-on:click="goHome" :class="isActive('trips') ? 'active' : ''">
          <span>
            Accueil
          </span>
          <div class="pastille">
            <i class="icon-home"/>
          </div>
          <div class="active-marker"></div>
        </div>

        <div class="item" v-if="connected" v-on:click="goDashboard" :class="isActive('dashboard') ? 'active' : ''">
          <span>
            Tableau de bord
          </span>
          <div class="pastille">
            <i class="icon-dashboard"/>
          </div>
          <div class="active-marker"></div>
        </div>

        <div class="item" v-if="connected" v-on:click="goSettings" :class="isActive('settings') ? 'active' : ''">
          <span>
            Paramètres
          </span>
          <div class="pastille">
            <i class="icon-settings"/>
          </div>
          <div class="active-marker"></div>
        </div>

        <div class="item" v-on:click="goDocumentation" :class="isActive('documentation') ? 'active' : ''">
          <span>
            Documentation
          </span>
          <div class="pastille">
            <i class="icon-files"/>
          </div>
          <div class="active-marker"></div>
        </div>

        <div class="item" v-on:click="goCredits" :class="isActive('credits') ? 'active' : ''">
          <span>
            Infos / Crédits
          </span>
          <div class="pastille">
            <i class="icon-info"/>
          </div>
          <div class="active-marker"></div>
        </div>

        <div class="item" v-on:click="openFeedback">
          <span>
            Des retours ?
          </span>
          <div class="pastille">
            <i class="icon-faq"/>
          </div>
          <div class="active-marker"></div>
        </div>

        <div class="item" v-if="connected" v-on:click="logout">
          <span>
            Déconnexion
          </span>
          <div class="pastille">
            <i class="icon-logout"/>
          </div>
          <div class="active-marker"></div>
        </div>

      </div>
  </div>
</template>

<script lang="ts">

import ProfileService from '@/services/ProfileService';
import TripsService from '@/services/TripsService';

import Helpers from '@/services/Helpers';

import Avatar from '@/components/common/Avatar.vue';
import UserProfile from '@/pojos/UserProfile';

import router from '@/router';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    Avatar
  }
})
export default class Menu extends Vue {

  envName?:string = process.env.VUE_APP_ENV;

  visibility:string = 'menu-hidden';

  connected:boolean = false;

  fullName:string = '';
  initials:string = '';

  created() {
    this.$root.$on('profile-updated', this.loadProfile);
    this.$root.$on('loggued-out', this.onLogguedOut);
    this.loadProfile();
  }

  mounted() {
    this.$root.$on('open-menu', this.openMenu);
  }

  beforeDestroy() {
    this.$root.$off('profile-updated');
    this.$root.$off('loggued-out');
    this.$root.$off('open-menu');
  }

  loadProfile() {
    ProfileService.getProfile()
      .then(this.profileLoaded);
  }

  profileLoaded(profile:UserProfile) {
    this.fullName = UserProfile.fullName(profile);
    this.initials = profile.initials;
    this.connected = true;
  }

  openMenu() {
    this.visibility = "menu-visible";
  }

  closeMenu() {
    this.$root.$emit('close-feedback');
    this.visibility = "menu-disappears";
  }

  openFeedback() {
    this.closeMenu();
    this.$root.$emit('open-feedback');
  }

  goHome() {
    this.closeMenu();
    router.push({name:'dispatcher'});
  }

  goProfile() {
    this.closeMenu();
    router.push('/profile');
  }

  goDashboard() {
    this.closeMenu();
    router.push('/dashboard');
  }

  goDocumentation() {
    this.closeMenu();
    router.push('/documentation');
  }

  goSettings() {
    this.closeMenu();
    router.push('/settings');
  }

  goCredits() {
    this.closeMenu();
    router.push('/credits');
  }

  logout() {
    Helpers.confirm(this.$modal, 'Voulez-vous vous déconnecter ?', 'Déconnexion')
      .then(() => {
        this.logoutConfirmed(false);
      });
  }

  promptLogoutWithRunningTrip() {
    Helpers.confirm(this.$modal, 'Vous avez une sortie en cours, elle sera perdue. Êtes-vous sûr\u00B7e ?', 'Déconnexion')
      .then(() => {
        this.logoutConfirmed(true);
      });
  }

  logoutConfirmed(ignoreRunningTrip:boolean) {
    this.closeMenu();
    if (ignoreRunningTrip) {
      ProfileService.logout()
        .then(this.logguedOut);
    } else {
      TripsService.hasRunningTrip()
        .then((hasRunningTrip:boolean) => {
          if (hasRunningTrip) {
            this.promptLogoutWithRunningTrip();
          } else {
            ProfileService.logout()
              .then(this.logguedOut);
          }
        });
    }
  }

  logguedOut() {
    router.push('/login');
    this.onLogguedOut();
  }

  onLogguedOut() {
    this.connected = false;
    this.fullName = '';
    this.initials = '';
  }

  isActive(name:string):boolean {
    return this.$route.name == name;
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";


  @media screen and (max-width: 599px) {
    .menu-hidden {
      left: calc(100vw);
    }

    .menu-disappears {
      animation-duration: 0.2s;
      animation-name: disappear;

      left: calc(100vw);

      @keyframes disappear {
        from {left: 0px;}
        to {left: calc(100vw);}
      }
    }

    .menu-visible {
      animation-duration: 0.2s;
      animation-name: appear;

      left: 0px;

      @keyframes appear {
        from {left: calc(100vw);}
        to {left: 0px;}
      }
    }
  }

  .menu {
    background-color: @pelorous;

    @media screen and (max-width: 599px) {
      position: fixed;
      top: 0px;
      width: 100vw;

      .menu-title {
        display: none;
      }
    }

    @media screen and (min-width: 600px) {
      width: @desktop-menu-width;

      .menu-title {
        height: 96px;
        width: 100%;

        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;

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

    @media screen and (min-width: 600px) {
      padding: 0px;
    }

    .close {

      @media screen and (min-width: 600px) {
        display: none;
      }

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
      width:100%;

      display: flex;
      flex-direction: column;
      align-items: flex-end;

      @media screen and (min-width: 600px) {
        align-items: flex-start;
      }

      padding-right: @margin-menu-item;

      .item {
        height: 50px;

        @media(max-height:600px) {
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

          @media screen and (max-width: 599px) {
            display: none;
          }

        }

        @media screen and (min-width: 600px) {
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

          @media screen and (min-width: 600px) {
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

          @media screen and (min-width: 600px) {
            width: 40px;
          }

          text-align: center;
        }

      }
    }

  }

</style>
