<template>
  <div class="picture-preview">
    <div v-if="!src"
         class="no-picture"
         v-on:click="$emit('take-picture')">
      <img src="/img/camera.svg"/>
      <span>{{noPictureText}}</span>
    </div>
    <div class="picture"
         v-on:click="openModal">
      <img v-if="src"
          class="picture"
          v-bind:src="src"/>
    </div>
    <PictureModal v-if="src && showModal && enableModal"
                  v-bind:src="src"
                  v-bind:replaceButton="modifiable"
                  v-on:replace="onReplace"
                  v-on:closeModal="closeModal"/>
  </div>
</template>

<script lang="ts">

import PictureModal from '@/components/trip/PictureModal.vue';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    PictureModal
  }
})
export default class PicturePreview extends Vue {

  @Prop() src:string = '';
  @Prop({default:'Aucune photo'}) noPictureText?:string;
  @Prop({default: true}) modifiable: boolean;
  @Prop({default: true}) enableModal: boolean;

  showModal:boolean = false;

  created() {
  }

  openModal() {
    if (this.enableModal) {
      this.showModal = true;
    }
  }

  closeModal() {
    this.showModal = false;
  }

  onReplace() {
    this.closeModal();
    this.$emit('take-picture');
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

.picture-preview {

  height: 100%;
  width: 100%;

  .picture {
    height: 100%;
    width: 100%;
    object-fit: cover;
    object-position: 50% 50%;
  }

  .no-picture {

    height: 100%;

    display:flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    span {
      margin-top: 10px;
      color: @pale-sky;
      font-weight: 300;
      font-size: 12px;
      line-height: 16px;
    }
  }
}
</style>
