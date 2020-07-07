<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #L%
  -->
<template>
  <div class="header hiddenWhenKeyboardShows">
    <div>
      <Title v-if="title"/>
    </div>
    <div class="header-buttons">
      <Avatar v-if="avatar && initials"
              v-bind:initials="initials"
              v-on:goProfile="goProfile"/>
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

  goProfile() {
    this.$root.$emit('close-feedback');
    router.push('/profile');
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
  padding-left: @margin-medium;
  padding-right: @margin-medium;
  padding-top: @margin-header-top:;
  padding-bottom: 0px;
  line-height: @average-header-height;

  z-index: 10;

  .header-buttons {
    display: flex;
    flex-direction: row;

    * {
      margin-left: @margin-small;
      margin-right: 0px;
    }
  }
}
</style>
