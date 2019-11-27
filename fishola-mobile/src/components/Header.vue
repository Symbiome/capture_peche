<template>
  <div class="header">
    <div class="left">
      <Title/>
    </div>
    <div class="right">
      <Avatar/>
      <FeedbackAnchor/>
      <Menu/>
    </div>
  </div>
</template>

<script lang="ts">
import Title from '@/components/Title.vue';
import Avatar from '@/components/Avatar.vue';
import FeedbackAnchor from '@/components/FeedbackAnchor.vue';
import Menu from '@/components/Menu.vue';

import { Component, Prop, Vue } from 'vue-property-decorator';

export default {
  name: 'header',
  components: {
    Title,
    Avatar,
    FeedbackAnchor,
    Menu
  }
}

// @Component
// export default class Header extends Vue {
// }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.header {
  height: 50px;
  // background-color: @pelorous;
  padding-left: 10px;
  padding-right: 10px;
  padding-top: 10px;
  padding-bottom: 10px;
  line-height: 30px;
  
  * {
    // background-color: @pelorous;
    // color: @white;
  }

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
