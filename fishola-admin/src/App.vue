<template>
  <div id="app">
    <router-link v-if="!$router.currentRoute.name" to="/">Perdu ?</router-link>
    <Menu v-if="$router.currentRoute.name && $router.currentRoute.name != 'login' && $router.currentRoute.name != 'dispatcher'"/>
    <router-view class="page-content"/>
  </div>
</template>

<script lang="ts">

import Menu from '@/components/Menu.vue'
import '@mdi/font/css/materialdesignicons.css'

export default {
  name: 'App',
  components: {
    Menu
  }
}
</script>

<style lang="less">

@import "less/main";

body {
  background-color: @white;
  margin: 0px;
  height: 100%;
  overflow: hidden;
  overflow-x: hidden;
  width: 100vw;
}

html {
  height: 100%;
}

* {
  box-sizing: border-box;
  -webkit-overflow-scrolling: touch;
}

#app {
  font-family: 'Open Sans', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: @gunmetal;
  height: 100%;
  max-height: 100%;
  max-width: 100%;
}

.page-content {

  padding: 10px;

}
</style>
