<template>
  <div class="pastille color0" v-on:click="$root.$emit('toaster-warning', 'Work in progress');">
    <span>{{initials}}</span>
  </div>
</template>

<script lang="ts">
import Constants from '@/services/Constants';

import UserProfile from '@/pojos/UserProfile';
import ProfileService from '@/services/ProfileService';
import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

@Component
export default class Avatar extends Vue {

  initials = '..';

  created() {
    ProfileService.getProfile()
        .then(
          this.profileLoaded,
          () => {
            this.$root.$emit('toaster-warning', 'Vous n\'êtes plus connecté\u00B7e');
            router.push('/login');
          });
  }

  profileLoaded(profile:UserProfile) {
    this.initials = profile.initials;
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";

  .pastille.color0 {
    color: @gunmetal;
    background: @avatar-background;
  }

</style>
