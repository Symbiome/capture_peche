<template>
  <div id="app">
    <router-view/>
  </div>
</template>

<style lang="less">

@import "less/main";

body {
  background-color: @pelorous;
  margin: 0px;
  height: 100%;
}

html {
  height: 100%;
}

* {
  box-sizing: border-box;
}

#app {
  font-family: 'Open Sans', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: @white;
}

</style>
