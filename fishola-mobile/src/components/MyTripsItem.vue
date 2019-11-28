<template>
  <div class="my-trips-item">
    <span class="name">Sortie du mercredi 6 novembre</span><br/>
    <span class="">Mercredi 6 novembre - 4h49min</span><br/>
    Lac Léman - 4
  </div>
</template>

<script lang="ts">

import { Component, Prop, Vue } from 'vue-property-decorator';

export default {
  name: 'my-trips-item',
  components: {
  }
}

// @Component
// export default class Header extends Vue {
// }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.my-trips-item {
    margin: 0px;
    padding-left: 30px;
    padding-top: 20px;
    padding-bottom: 20px;

    border-bottom: 1px solid @gainsboro;

    font-size: 12px;
    line-height: 16px;
    color: @pale-sky;

    .name {
      font-weight: bold;
      font-size: 14px;
      line-height: 19px;
      color: @gunmetal;
    }
}

</style>
