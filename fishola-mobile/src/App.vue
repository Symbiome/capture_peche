<template>
  <div id="app">
    <router-view/>
  </div>
</template>

<style lang="less">

@import "less/main";

body {
  background-image: url("/img/background.png");
  background-repeat: no-repeat;
  background-size: cover;
  // background-image: url("/img/background_transparent.png");
  // background-color: @pelorous;
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
// #app:after {
//     content : "";
//     display: block;
//     position: absolute;
//     top: 0;
//     left: 0;
//     background-image: url("/img/background_transparent.png");
//     width: 100%;
//     height: 100%;
//     opacity : 0.3;
//     z-index: -1;
// }
</style>
