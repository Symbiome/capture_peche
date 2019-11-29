<template>
  <div class="my-trips-header">
    <div class="left">
      <span>Mes sorties</span>
    </div>
    <div class="right">
      <div>
        <i class="icon-calendar"></i>
        <i class="icon-sort-down">icon-sort-down</i>
      </div>
      <div>
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
      type: String
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

@import "../less/main";

.my-trips-header {

  height: 39px;
  padding-left: 10px;
  padding-right: 10px;
  padding-top: 10px;
  padding-bottom: 10px;

  font-weight: bold;
  font-size: 14px;
  line-height: 19px;

  .left {
    width: 25%;
    float: left;
  }
  .right {
    width: 75%;
    float: left;
    text-align: right;

    * {
      display: inline;
      padding: 5px;
    }
  }
}
</style>
