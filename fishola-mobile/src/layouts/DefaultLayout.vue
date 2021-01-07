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
  <div>
    <v-dialog :width="270"/>
    <Toaster/>
    <Menu :class="{'hide-on-desktop': isLoginPage()}"/>
    <FeedbackModal/>
    <div id="root"
         :class="{'full-width': isLoginPage()}">
      <slot />
    </div>
  </div>
</template>

<script lang="ts">

import Toaster from '@/components/layout/Toaster.vue'
import Menu from '@/components/layout/Menu.vue'
import FeedbackModal from '@/components/layout/FeedbackModal.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    Toaster,
    Menu,
    FeedbackModal
  }
})
export default class NoMenuLayout extends Vue {

  isLoginPage():boolean {
    return this.$route.name == 'login';
  }

}

</script>

<style lang="less">

@import "../less/_responsive";

#root {
  height: 100%;
  width: 100%;

  @media screen and (min-width: 600px) {
    width: calc(100% - @desktop-menu-width);
  }

  &.full-width {
    width: 100%;
  }

}

</style>
