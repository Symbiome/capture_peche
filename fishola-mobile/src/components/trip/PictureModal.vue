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
  <div class="picture-modal">
    <div class="pastille close"
         v-on:click="$emit('closeModal')">
      <i class="icon-plus"/>
    </div>
    <div class="picture-wrapper"
         v-on:click="$emit('closeModal')">
      <div class="picture-content">
        <img class="picture"
            v-bind:src="src"
            alt="Photo de la capture">
      </div>
    </div>
    <div class="replace"
         v-if="replaceButton">
      <button v-on:click="$emit('replace')">
        <i class="icon-photo"/> Remplacer la photo
      </button>
    </div>
  </div>
</template>

<script lang="ts">

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class PictureModal extends Vue {

  @Prop() src:string;
  @Prop({default: false}) replaceButton:boolean;

  created() {
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.picture-modal {

  position: fixed;
  z-index: 1500;   
  top: env(safe-area-inset-top);
  left: 0;
  width: 100%;
  height: 100%;
  background-color: @black-alpha-90;
  transition: opacity .3s ease;

  .close {
    position: fixed;
    top: calc(env(safe-area-inset-top) + 30px);
    right: 30px;
    background-color: #fff5;

    i {
      font-size: @fontsize-header-paragraph;
      transform: rotate(45deg);
    }
  }

  .replace {
    position: fixed;
    bottom: 30px;
    width: 100%;
    line-height: 40px;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;

    img {
      margin-left: @margin-medium;
    }


    button {
      height: 41px;
      width: fit-content;
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
        font-size: @fontsize-button-big;
      }

      &.delete {
        background-color: @cardinal;
      }
    }

  }

  .picture-wrapper {

    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    // border: 1px solid red;

    .picture-content {

      // border: 1px solid blue;

      img.picture {
        // height: 100%;
        // width: 100%;
        // object-fit: cover;
        // object-position: 50% 50%;
      }
    }

  }

}
</style>
