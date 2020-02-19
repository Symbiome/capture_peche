<template>
  <div class="header">
    <div>
      <Title v-if="title"/>
    </div>
    <div class="header-buttons">
      <Avatar v-if="avatar && initials" v-bind:initials="initials"/>
      <FeedbackAnchor/>
      <MenuAnchor v-if="menu"/>
    </div>
  </div>
</template>

<script lang="ts">
import Title from '@/components/layout/Title.vue';
import Avatar from '@/components/common/Avatar.vue';
import FeedbackAnchor from '@/components/common/FeedbackAnchor.vue';
import MenuAnchor from '@/components/common/MenuAnchor.vue';

import UserProfile from '@/pojos/UserProfile';
import ProfileService from '@/services/ProfileService';

import router from '../../router';

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
  @Prop({ default: true }) title: boolean;
  @Prop({ default: true }) avatar: boolean;
  @Prop({ default: true }) menu: boolean;

  initials = '';

  created() {
    if (this.avatar) {
      ProfileService.getProfile()
        .then(
          this.profileLoaded,
          () => {
            this.$root.$emit('toaster-warning', 'Vous n\'êtes plus connecté\u00B7e');
            router.push('/login');
          });
    }
  }

  profileLoaded(profile:UserProfile) {
    this.initials = profile.initials;
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

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
