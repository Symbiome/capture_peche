<template>
  <div class="menu" v-bind:class="visibility">
      <div class="close"
           v-on:click="closeMenu">
        +
      </div>
      <div class="items">

        <div class="item">
          <span>
            Profil (TODO)
          </span>
          <i class="icon-profile"/>
        </div>

        <div class="item">
          <span>
            Accueil
          </span>
          <i class="icon-home"/>
        </div>

        <div class="item">
          <span>
            Tableau de bord
          </span>
          <i class="icon-dashboard"/>
        </div>

        <div class="item">
          <span>
            Paramètres
          </span>
          <i class="icon-settings"/>
        </div>

        <div class="item">
          <span>
            Documentation
          </span>
          <i class="icon-file"/>
        </div>

        <div class="item">
          <span>
            Infos / Crédits
          </span>
          <i class="icon-info"/>
        </div>

        <div class="item">
          <span>
            Des retours ?
          </span>
          <i class="icon-faq"/>
        </div>

        <div class="item">
          <span>
            Déconnexion
          </span>
          <i class="icon-logout"/>
        </div>

      </div>
  </div>
</template>

<script lang="ts">


import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Menu extends Vue {

  visibility:string = 'menu-hidden';

  mounted() {
    this.$root.$on('open-menu', this.openMenu);
  }

  openMenu() {
    console.log("Ouverture du menu");
    this.visibility = "menu-visible";
  }

  closeMenu() {
    console.log("Fermeture du menu");
    this.visibility = "menu-disappears";
  }

  beforeDestroy() {
    console.log("Destroy menu");
    this.$root.$off('open-menu');
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
    animation-duration: 0.5s;
    animation-name: disappear;

    left: calc(100vw);

    @keyframes disappear {
      from {left: 0px;}
      to {left: calc(100vw);}
    }
  }

  .menu-visible {
    animation-duration: 0.5s;
    animation-name: appear;

    left: 0px;

    @keyframes appear {
      from {left: calc(100vw);}
      to {left: 0px;}
    }
  }

  .menu {
    background-color: @pelorous;
    position:absolute;
    width: 100vw;
    height: 100vh;
    z-index: 100;

    display: flex;
    flex-direction: column;
    // justify-content: center;
    align-items: flex-end;

    color:white;

    padding: 15px;

    .close {
      font-size: 30px;
      transform: rotate(45deg);
      margin: 10px;
    }

    .items {
      width:100%;

      display: flex;
      flex-direction: column;
      // justify-content: center;
      align-items: flex-end;

      font-weight: bold;
      font-size: 14px;
      line-height: 19px;

      padding-right: 15px;

      .item {

        height: 50px;

        display: flex;
        flex-direction: row;
        // justify-content: center;
        align-items: center;

        width: fit-content;

        span {
        }

        i {
          margin-left: 20px;
        }

      }
    }

  }

</style>
