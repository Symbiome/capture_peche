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
  <div class="footer-button">
    <div>
      <button v-on:click="$emit('clicked')" v-bind:class="deleteMode ? 'delete' : ''">
        <i v-if="icon" v-bind:class="icon"/>
        {{text}}
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class FooterButton extends Vue {
  @Prop() icon?:string;
  @Prop() text?:string;
  @Prop({default: false}) deleteMode:boolean;

  mounted() {
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.footer-button {

  height: 41px;
  width: fit-content;
  position: absolute;
  bottom: calc(@footer-height - 13px);

  &.keyboardShowing {
    bottom: min(calc(@reduced-footer-height - 13px), 2px);
  }

  display: flex;
  justify-content: center;

  z-index: 10;

  div {
    height: 41px;
  }

  button {
    height: 100%;
    width: 100%;
    border-radius: 22px;

    font-style: normal;
    font-weight: bold;
    font-size: @fontsize-button;
    line-height: calc(@fontsize-button + @line-height-padding-x-large);

    color: @white;
    background-color: @terra-cotta;

    border: 0px;
    padding-left: @margin-medium;
    padding-right: @margin-medium;

    i {
      margin-right: @margin-x-small;
      font-size: @fontsize-button;
    }

    &.delete {
      background-color: @cardinal;
    }
  }
}

</style>
