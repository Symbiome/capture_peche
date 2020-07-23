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
  <div class="catch-preview-list">
    <div class="new-catch" 
         v-if="modifiable">
      <div class="new-catch-square-button"
           id="new-catch-square-button"
           v-on:click="newCatch">
        <i class="pastille icon-plus"/>
      </div>
    </div>
    <div v-for="(c, index) in reversedCatchs()"
          v-bind:key="c.id"
          class="preview-wrapper"
          v-on:click="openCatch(c)">
      <CatchPreview v-bind:lakeId="lakeId"
                    v-bind:aCatch="c"
                    v-on:openCatch="openCatch(c)"
                    v-bind:metaMode="metaMode"
                    v-bind:bottom="(bottomMode == 'species' ? 'species' : ('top-' + (index+1)))"/>
    </div>
  </div>
</template>

<script lang="ts">
import CatchSummary from '@/pojos/CatchSummary';

import CatchPreview from '@/components/trip/CatchPreview.vue';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    CatchPreview
  }
})
export default class CatchPreviewList extends Vue {

  @Prop() lakeId:string;
  @Prop() catchs:CatchSummary[];
  @Prop() modifiable:boolean;
  @Prop({default: true}) reverse:boolean;
  @Prop({default: 'size'}) metaMode:string;
  @Prop({default: 'species'}) bottomMode:string;

  created() {
  }

  mounted() {
    if (this.catchs && this.catchs.length > 0) {
      this.scrollToFirstElement();
    }
  }

  reversedCatchs() {
    if (this.catchs && this.reverse) {
      return this.catchs.slice().reverse();
    } else {
      return this.catchs;
    }
  }

  scrollToFirstElement() {
    try {
      const squareButton = document.getElementById('new-catch-square-button');
      const scrollableElements = document.getElementsByClassName('catch-preview-list-scrollable');
      if (squareButton && scrollableElements) {
        scrollableElements[0].scrollLeft = squareButton.clientWidth + 2; // +2 pour la bordure
      }
    } catch (someError) {
      console.error(someError);
    }
  }

  newCatch() {
    this.$emit('newCatch');
  }

  openCatch(aCatch:CatchSummary) {
    this.$emit('openCatchFromId', aCatch.id);
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.catch-preview-list {

  height: 100%;
  display: flex;

  padding-left: calc(@margin-large + 5px);
  padding-right: calc(@margin-large + 5px);

  .new-catch {

    height: 100%;

    padding-top: @vertical-margin-xx-small;
    padding-bottom: @vertical-margin-xx-small;

    .new-catch-square-button {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;

      background-image: url("/img/illustration_fish_wire.svg");
      background-repeat: no-repeat;
      background-size: auto 75%;
      background-position: center;

      border: 1px dashed @pale-sky;
      border-radius: 8px;
      height: 100%;
      width: calc(100vw - 80px);

      margin-left: @margin-x-small;
      margin-right: @margin-x-small;

      cursor: pointer;

      .pastille {
        width: 70px;
        height: 70px;
        font-size: @pastille-size;
        line-height: calc(@pastille-size);
        color: @white;
        background: @pale-sky;
      }

    }
  }


  // @media(max-width:360px) {

  //   padding-left: calc(@margin-large + 5px);
  //   padding-right: calc(@margin-large + 5px);

  //   .new-catch {

  //     .new-catch-square-button {

  //       height: 100%;
  //       width: calc(100vw - 80px);

  //       margin-left: @margin-x-small;
  //       margin-right: @margin-x-small;

  //       .pastille {
  //         width: 50px;
  //         height: 50px;
  //         font-size: calc(@pastille-size * 5 / 7);
  //         line-height: calc(@pastille-size * 5 / 7);
  //       }

  //     }
  //   }

  // }

  .preview-wrapper {
  }

}
</style>
