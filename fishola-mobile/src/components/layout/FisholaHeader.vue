<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
  <div class="header hiddenWhenKeyboardShows hide-on-desktop">
    <div>
      <Title v-if="title" />
    </div>
    <div class="header-buttons">
      <Avatar
        v-if="avatar && initials"
        class="header-avatar"
        v-bind:initials="initials"
        v-on:goProfile="goProfile"
      />
      <MenuAnchor v-if="menu && !initials" />
    </div>
  </div>
</template>

<script lang="ts">
import Title from "@/components/layout/Title.vue";
import Avatar from "@/components/common/Avatar.vue";
import MenuAnchor from "@/components/common/MenuAnchor.vue";

import UserProfile from "@/pojos/UserProfile";
import ProfileService from "@/services/ProfileService";

import router from "../../router";
import { RouterUtils } from "@/router/RouterUtils";

import { Component, Prop, Vue } from "vue-property-decorator";

@Component({
  components: {
    Title,
    Avatar,
    MenuAnchor,
  },
})
export default class FisholaHeader extends Vue {
  @Prop({ default: true }) title: boolean;
  @Prop({ default: true }) avatar: boolean;
  @Prop({ default: true }) menu: boolean;

  initials = "";

  created() {
    if (this.avatar) {
      ProfileService.getProfile().then(this.profileLoaded);
    }
  }

  profileLoaded(profile: UserProfile) {
    this.initials = profile.initials;
  }

  goProfile() {
    this.$root.$emit("close-feedback");
    RouterUtils.pushRouteNoDuplicate(this.$router, "/account");
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  height: @header-height;
  padding-left: 14px;
  padding-right: 14px;

  z-index: 10;

  .header-buttons {
    display: flex;
    flex-direction: row;
    align-items: center;

    * {
      margin-left: @margin-small;
      margin-right: 0px;
    }

    .header-avatar {
      width: 48px;
      height: 48px;
      font-size: @fontsize-paragraph;
    }
  }
}
</style>
