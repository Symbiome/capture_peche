<template>
  <div class="my-trips-item">
    <span class="name">Sortie du mercredi 6 novembre</span><br/>
    <i class="icon-calendar left-icon"/>Mercredi 6 novembre - 4h49min <i class="icon-clock right-icon"/><br/>
    <i class="icon-lake left-icon"/>Lac Léman - 4 <i class="icon-fish right-icon"/>
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

    .left-icon {
      margin-right: 10px;
      color: @gunmetal;
    }
    .right-icon {
      margin-left: 10px;
      color: @pelorous;
    }
}

</style>
