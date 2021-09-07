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
  <div class="picture-preview">
    <div v-if="!src" class="no-picture" v-on:click="$emit('take-picture')">
      <img src="/img/camera.svg" alt="Pas de photo" />
      <span>{{ noPictureText }}</span>
    </div>
    <div class="picture" v-if="src" v-on:click="openModal">
      <img
        class="picture"
        v-bind:src="src"
        alt="Photo de la capture"
        @load="pictureLoaded"
      />
    </div>
    <PictureModal
      v-if="src && showModal && enableModal"
      v-bind:src="src"
      v-bind:replaceButton="modifiable"
      v-on:replace="onReplace"
      v-on:closeModal="closeModal"
    />
  </div>
</template>

<script lang="ts">
import PictureModal from "@/components/trip/PictureModal.vue";

import { Component, Prop, Vue } from "vue-property-decorator";

@Component({
  components: {
    PictureModal,
  },
})
export default class PicturePreview extends Vue {
  @Prop() src: string = "";
  @Prop({ default: "Aucune photo" }) noPictureText?: string;
  @Prop({ default: true }) modifiable: boolean;
  @Prop({ default: true }) enableModal: boolean;

  showModal: boolean = false;

  created() {}

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
    this.$emit("take-picture");
  }

  pictureLoaded() {
    this.$emit("load");
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

    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    cursor: pointer;

    span {
      margin-top: @vertical-margin-small;
      color: @pale-sky;
      font-weight: 300;
      font-size: @fontsize-small-paragraph;
      line-height: calc(
        @fontsize-small-paragraph + @line-height-padding-medium
      );
    }
  }
}
</style>
