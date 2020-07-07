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
  <div class="top" v-bind:class="'top-' + n">
    <span>{{n}}</span>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Top extends Vue {
  @Prop() n:number;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";

  .top {
    font-weight: bold;
    font-size: @fontsize-top;
    line-height: calc(@fontsize-top + 8px);
  }

  .top-2, .top-3, .top-4, .top-5 {
    height:@fontsize-top1;
    width:@fontsize-top1;
    border-radius: 50%;
  }

  .top-1 {
    color: @white;
    position: relative;
    z-index: 1;
    line-height: calc(@fontsize-top1 - 2px);
  }

  .top-1::after {
    content: '\f11d'; // Correspond à .icon-star:before dans _icons.less
    font-family: 'Fishola-Icons';
    color: @terra-cotta;
    position: absolute;
    left: -6px;
    top: -1px;
    font-size: @fontsize-top1;
    line-height: @fontsize-top1;
    z-index: -1;
  }

  .top-2, .top-3 {
    background-color: @cyprus;
    color: @white;
  }

  .top-4, .top-5 {
    background-color: transparent;
    color: @cyprus;
    border: 1px solid @cyprus;
    line-height: @fontsize-top4;
  }

</style>
