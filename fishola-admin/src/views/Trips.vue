<template>
  <div class="trips">
    <p>Vous pouvez exporter les sorties.</p>
    <button class="button is-primary" @click="save()">Lancer un export CSV</button>
  </div>
</template>

<script>

export default {
  name: 'Trips',
  components: {
  }
}
</script>

<style scoped lang="less">

@import "../less/main";

.trips {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  font-size: 22px;

  p {
    margin: 5px;
  }
}

</style>
