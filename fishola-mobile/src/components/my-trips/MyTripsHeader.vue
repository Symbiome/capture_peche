<template>
  <div class="my-trips-header">
    <div>
      <span v-on:click="$emit('newMockTrip')">Mes sorties</span>
    </div>
    <div class="header-icons">
      <div class="header-icons-group">
        <i class="icon-calendar"></i>
        <i class="icon-chevron"></i>
      </div>
      <div class="header-icons-group">
        <span>{{count}}</span>
        <i class="icon-fishing"></i>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

export default {
  props: {
    count: {
      type: Number
    }
  },
  name: 'my-trips-header',
  components: {
    // Title,
    // Avatar,
    // FeedbackAnchor,
    // Menu
  }
}

// @Component
// export default class Header extends Vue {
// }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.my-trips-header {

  display: flex;
  justify-content: space-between;

  height: 49px;
  padding-left: 20px;
  padding-right: 20px;
  padding-top: 10px;
  padding-bottom: 10px;
  margin-bottom: 10px;

  font-weight: bold;
  font-size: 14px;
  line-height: 19px;

  .header-icons {
    display: flex;
    flex-direction: row;
    align-items: center;

    .header-icons-group {
      display: flex;
      margin-left: 20px;
      margin-right: 0px;
      align-items: center;

      * {
        margin-left: 5px;
      }

      .icon-chevron {
        margin-top: 6px;
        font-size: 7px;
      }
    }
  }
}
</style>
