<!--
  #%L
  Carnet de pêche :: Mobile
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
  <div class="pastille color0" v-on:click="goProfile">
    <span>{{initials}}</span>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-facing-decorator";

@Component({ emits: ["goProfile"] })
export default class Avatar extends Vue {

  @Prop() initials:string;

  goProfile() {
    this.$emit('goProfile');
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
  .pastille.color0 {
    color: @accent-text;
    background: @avatar-background;
    font-weight: 600;
  }

  :root[data-theme="dark"] .pastille.color0 {
    color: @white;
    background: @carnet-blue;
  }

</style>
