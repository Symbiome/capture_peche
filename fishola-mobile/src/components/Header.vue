<template>
  <div class="header">
    <Title/>
    <Avatar/>
    <FeedbackAnchor/>
    <Menu/>
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
  background-color: @pelorous;
  color: @white;
}
</style>
