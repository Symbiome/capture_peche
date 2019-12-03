<template>
  <div class="header">
    <div>
      <Title/>
    </div>
    <div class="header-buttons">
      <Avatar/>
      <FeedbackAnchor/>
      <Menu/>
    </div>
  </div>
</template>

<script lang="ts">
import Title from '@/layout/Title.vue';
import Avatar from '@/components/common/Avatar.vue';
import FeedbackAnchor from '@/components/common/FeedbackAnchor.vue';
import Menu from '@/layout/Menu.vue';

import { Component, Prop, Vue } from 'vue-property-decorator';

export default {
  name: 'fishola-header',
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

  display: flex;
  justify-content: space-between;

  height: 35px;
  padding-left: 20px;
  padding-right: 20px;
  padding-top: 5px;
  padding-bottom: 0px;
  line-height: 30px;


  .header-buttons {
    display: flex;
    flex-direction: row;

    * {
      margin-left: 10px;
      margin-right: 0px;
    }
  }
}
</style>
