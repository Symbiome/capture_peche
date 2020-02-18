<template>
  <div class="header">
    <div>
      <Title v-if="title"/>
    </div>
    <div class="header-buttons">
      <Avatar v-if="avatar"/>
      <FeedbackAnchor/>
      <MenuAnchor v-if="menu"/>
    </div>
  </div>
</template>

<script lang="ts">
import Title from '@/layout/Title.vue';
import Avatar from '@/components/common/Avatar.vue';
import FeedbackAnchor from '@/components/common/FeedbackAnchor.vue';
import MenuAnchor from '@/components/common/MenuAnchor.vue';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    Title,
    Avatar,
    FeedbackAnchor,
    MenuAnchor
  }
})
export default class FisholaHeader extends Vue {
  @Prop({ default: true }) title!: Boolean;
  @Prop({ default: true }) avatar!: Boolean;
  @Prop({ default: true }) menu!: Boolean;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.header {

  display: flex;
  justify-content: space-between;

  height: @header-height;
  padding-left: 20px;
  padding-right: 20px;
  padding-top: 10px;
  padding-bottom: 0px;
  line-height: 30px;

  z-index: 10;

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
