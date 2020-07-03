<template>
  <div class="home">
    <p>Bienvenue sur l'interface d'administration de FISHOLA.</p>
    <p>Vous pouvez utiliser le menu pour accéder aux différentes sections.</p>
  </div>
</template>

<script>
// @ is an alias to /src

export default {
  name: 'Home',
  components: {
    // Menu
  }
}
</script>

<style scoped lang="less">

@import "../less/main";

.home {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  p {
    font-size: 22px;
    margin: 5px;
  }
}

</style>
