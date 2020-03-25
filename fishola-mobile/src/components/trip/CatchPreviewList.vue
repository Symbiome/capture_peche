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
      let squareButton = document.getElementById('new-catch-square-button');
      let scrollableElements = document.getElementsByClassName('catch-preview-list-scrollable');
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

  padding-left: 35px;
  padding-right: 35px;

  .new-catch {

    height: 100%;

    padding-top: 5px;
    padding-bottom: 5px;

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

      margin-left: 5px;
      margin-right: 5px;

      cursor: pointer;

      .pastille {
        width: 70px;
        height: 70px;
        line-height: 30px;
        font-size: 30px;
        color: @white;
        background: @pale-sky;
      }

    }
  }

  .preview-wrapper {
  }

}
</style>
