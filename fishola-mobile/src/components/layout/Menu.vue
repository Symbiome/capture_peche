<template>
  <div class="menu" v-bind:class="visibility">
      <div class="close"
           v-on:click="closeMenu">
           <div class="plus">+</div> 
      </div>
      <div class="items">

        <div class="item" v-on:click="goProfile">
          <span>
            {{fullName}}
          </span>
          <Avatar v-if="initials" v-bind:initials="initials"/>
        </div>

        <div class="item" v-on:click="goHome">
          <span>
            Accueil
          </span>
          <i class="icon-home"/>
        </div>

        <div class="item" v-on:click="goDashboard">
          <span>
            Tableau de bord
          </span>
          <i class="icon-dashboard"/>
        </div>

        <div class="item" v-on:click="goSettings">
          <span>
            Paramètres
          </span>
          <i class="icon-settings"/>
        </div>

        <div class="item" v-on:click="goDocumentation">
          <span>
            Documentation
          </span>
          <i class="icon-files"/>
        </div>

        <div class="item" v-on:click="goCredits">
          <span>
            Infos / Crédits
          </span>
          <i class="icon-info"/>
        </div>

        <div class="item" v-on:click="openFeedback">
          <span>
            Des retours ?
          </span>
          <i class="icon-faq"/>
        </div>

        <div class="item" v-on:click="logout">
          <span>
            Déconnexion
          </span>
          <i class="icon-logout"/>
        </div>

      </div>
  </div>
</template>

<script lang="ts">

import ProfileService from '@/services/ProfileService';
import TripsService from '@/services/TripsService';

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

  visibility:string = 'menu-hidden';

  fullName:string = '';
  initials:string = '';

  created() {
    this.$root.$on('profile-updated', this.loadProfile);
    this.loadProfile();
  }

  mounted() {
    this.$root.$on('open-menu', this.openMenu);
  }

  beforeDestroy() {
    this.$root.$off('profile-updated');
    this.$root.$off('open-menu');
  }

  loadProfile() {
    ProfileService.getProfile()
      .then(
        this.profileLoaded,
        () => {
          this.$root.$emit('toaster-warning', 'Vous n\'êtes plus connecté\u00B7e');
          router.push('/login');
        });
  }

  profileLoaded(profile:UserProfile) {
    this.fullName = UserProfile.fullName(profile);
    this.initials = profile.initials;
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
    router.push('/trips');
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
    if (confirm("Voulez-vous vous déconnecter ?")) {
      this.closeMenu();

      TripsService.hasRunningTrip()
        .then((result:boolean) => {
          if (!result || confirm("Vous avez une sortie en cours, elle sera perdue. Êtes-vous sûr\u00B7e ?")) {
            ProfileService.logout()
              .then(this.logguedOut);
          }
        });

    }
  }

  logguedOut() {
    router.push('/login');
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";

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

  .menu {
    background-color: @pelorous;
    position: fixed;
    top: 0px;
    width: 100vw;
    height: 100vh;
    z-index: 100;

    display: flex;
    flex-direction: column;
    align-items: flex-end;

    color: @white;

    padding: 15px;

    .close {
      display: flex;
      flex-direction: row-reverse;
      align-items: center;

      height: 30px;
      margin: 10px;
      width: 100%;

      div.plus {
        font-size: 30px;
        width: fit-content;
        transform: rotate(45deg);
      }
    }

    .items {
      width:100%;

      display: flex;
      flex-direction: column;
      // justify-content: center;
      align-items: flex-end;

      padding-right: 15px;

      .item {

        height: 50px;

        display: flex;
        flex-direction: row;
        // justify-content: center;
        align-items: center;

        width: fit-content;

        span {
          margin-right: 20px;

          font-size: 14px;
          font-weight: bold;
          line-height: 19px;
        }

        i {
          font-size: 20px;
          width: 30px;
          text-align: center;
        }

      }
    }

  }

</style>
