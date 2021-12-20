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
  <div class="modal-container">
    <div class="mobile-gallery hide-on-desktop">
      <div class="transparent-background" @click="$emit('closeModal')"></div>
      <div class="pane popup-content">
        <div class="no-scroll">
          <h2 class="title">Galerie Photos</h2>
          <!-- pictures required for measurement -->
          <PictureModalMobileGallerySlider
            :src="focusedPicSrc"
            :otherPics="otherPics"
            :measurementPictureSrc="measurementPictureSrc"
            @delete="deletePicture"
            @take-picture="takePicture"
          />
        </div>
      </div>
    </div>
    <!-- Gallery on desktop-->
    <div class="gallery hide-on-mobile">
      <div class="pic-miniatures-container">
        <!-- Show all gallery pics -->
        <div
          v-for="pictureSrc in allNonMeasurePictures"
          class="pic-miniature picture-preview"
          :key="pictureSrc.order"
          @click="focusedPicSrc = pictureSrc.content"
          :class="{
            'pic-miniature': true,
            'pic-selected': pictureSrc.content == focusedPicSrc,
          }"
        >
          <img class="picture" :src="pictureSrc.content" />
        </div>
        <!-- Then measurement pic (if any) -->
        <div
          v-if="measurementPictureSrc"
          class="pic-miniature picture-preview"
          :key="measurementPictureSrc.order"
          @click="focusedPicSrc = measurementPictureSrc"
          :class="{
            'pic-miniature': true,
            'pic-selected': measurementPictureSrc == focusedPicSrc,
          }"
        >
          <img class="picture" :src="measurementPictureSrc" />
        </div>
        <!-- Empty miniature picture for adding pictures -->
        <div
          class="pic-miniature picture-preview"
          v-if="
            focusedPicSrc &&
            (allNonMeasurePictures.length < 4 ||
              (!measurementPictureSrc && allNonMeasurePictures.length < 5))
          "
        >
          <div class="picture add-pic-button">
            <img
              src="/img/add-pic-to-gallery.svg"
              alt="Ajouter une photo"
              class="picture"
              v-on:click="takePicture"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="picture-modal">
      <div class="pastille close" v-on:click="$emit('closeModal')">
        <i class="icon-plus" />
      </div>
      <div class="picture-wrapper" v-on:click="$emit('closeModal')">
        <div class="picture-content">
          <img
            class="picture"
            v-bind:src="focusedPicSrc"
            alt="Photo de la capture"
          />
        </div>
      </div>
      <div class="replace" v-if="allowDeletion">
        <button v-on:click="deletePicture(focusedPicSrc)">
          <i class="icon-delete" /> Supprimer
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import PictureContentWithOrder from "@/pojos/PictureContentWithOrder";
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import PicturePreview from "@/components/trip/PicturePreview.vue";
import PictureModalMobileGallerySlider from "@/components/trip/PictureModalMobileGallerySlider.vue";

@Component({
  components: {
    PicturePreview,
    PictureModalMobileGallerySlider,
  },
})
export default class PictureModal extends Vue {
  @Prop() src: string;
  @Prop({ default: false }) deleteButton: boolean;
  @Prop() otherPics: PictureContentWithOrder[];
  @Prop({ default: "" }) measurementPictureSrc: "";
  focusedPicSrc: string = "";
  allNonMeasurePictures: PictureContentWithOrder[] = [];
  allowDeletion = false;

  mounted() {
    this.allNonMeasurePictures = this.otherPics;
    if (!this.allNonMeasurePictures) {
      this.allNonMeasurePictures = [];
    }
    this.focusedPicSrc = this.src;
    this.allowDeletion = this.deleteButton;
  }

  @Watch("focusedPicSrc")
  focusPicSrcChanged(newValue: string) {
    this.allowDeletion = newValue != this.measurementPictureSrc;
  }

  @Watch("src")
  srcChanged(newValue: string) {
    this.focusedPicSrc = this.src;
  }

  takePicture() {
    this.$emit("closeModal");
    this.$emit("take-picture");
  }

  deletePicture(pictureToDeleteSrc: string) {
    this.$emit("closeModal");
    this.$emit("delete", pictureToDeleteSrc);
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../../less/main";

.no-scroll {
  overflow: hidden;
}
.modal-container {
  position: fixed;
  z-index: 1500;
  top: env(safe-area-inset-top);
  left: 0;
  width: 100%;
  height: 100%;
  background-color: @black-alpha-90;
  transition: opacity 0.3s ease;
}
.picture-modal {
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
    margin-left: calc(@desktop-menu-width + 15px + 140px);
    width: calc(100vw - @desktop-menu-width - 140px);
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

    // border: 1px solid red;

    .picture-content {
      // border: 1px solid blue;

      img.picture {
        width: auto;
        margin: 10px;
        max-width: calc(100vw - @desktop-menu-width - 15px - 200px);
        height: calc(100vh - 20px);
        object-fit: cover;
      }
    }
  }
}

.gallery {
  width: 140px;
  float: left;
  margin-left: calc(@desktop-menu-width + 15px);

  .pic-miniatures-container {
    display: flex;
    flex-direction: column;
    margin-top: 10px;
    gap: 10px;

    .pic-miniature {
      cursor: pointer;
      :hover {
        -webkit-transform: scale(1.05);
        -moz-transform: scale(1.05);
        -o-transform: scale(1.05);
        transform: scale(1.05);
      }
      max-width: 140px;
      height: 90px;
      flex: 1 1 auto;

      &.pic-selected {
        border: 4px solid @pelorous;
        -webkit-transform: scale(1.05);
        -moz-transform: scale(1.05);
        -o-transform: scale(1.05);
        transform: scale(1.05);
        :hover {
          -webkit-transform: scale(1);
          -moz-transform: scale(1);
          -o-transform: scale(1);
          transform: scale(1);
        }
      }
    }

    .add-pic-button {
      cursor: pointer;
      background-color: white;
      border: 2px solid @gainsboro;
      border-radius: 2px;
      img {
        object-fit: contain;
      }
      padding: 10px;
    }
  }
}

.mobile-gallery {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 100vh;
  width: 100%;
  z-index: 99;
  background-color: rgba(0, 0, 0, 0.6);

  .title {
    color: @pelorous;
    font-weight: normal;
    font-size: calc(@fontsize-title + 5px);
    text-align: center;
  }
  .transparent-background {
    height: 18vh;
  }
  .popup-content {
    margin-top: 0px !important;
    height: 82vh;
    overflow-y: auto;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
  }
}
</style>
