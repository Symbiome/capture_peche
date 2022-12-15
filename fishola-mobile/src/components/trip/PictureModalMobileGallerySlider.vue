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
  <div class="mobile-gallery-slider" v-touch:swipe="swiped">
    <div class="slider-pic">
      <div
        v-for="slide in slides"
        :key="slide.src"
        class="slide fade"
        v-show="currentSlide == slide.order"
        v-touch:swipe="swiped"
      >
        <img
          v-if="!slide.isAddPicButton"
          :src="slide.src"
          v-touch:swipe="swiped"
        />
        <div v-else @click="$emit('take-picture')">
          <img
            src="/img/add-pic-to-gallery.svg"
            v-touch:swipe="swiped"
            class="add-picture"
          />
        </div>
      </div>
    </div>
    <div style="clear: both" />

    <div class="footer">
      <FooterButton
        v-if="
          !readOnly &&
          !slides[currentSlide].isAddPicButton &&
          !slides[currentSlide].isMeasurementPic
        "
        icon="icon-delete"
        text="Supprimer"
        v-on:clicked="$emit('delete', slides[currentSlide].src)"
      />
      <FooterButton
        v-if="readOnly"
        text="Voir la sortie"
        @clicked="$emit('seeTrip')"
      />

      <div class="footer-element pastille" @click="swiped('right')">
        <i class="icon-arrow icon-back" v-if="currentSlide > 0"></i>
      </div>
      <div class="footer-element steps" v-if="slides.length > 0">
        <div
          v-for="s in slides"
          v-bind:key="s.id"
          v-bind:class="s.order == currentSlide ? 'step step-active' : 'step'"
          @click="currentSlide = s.order"
        >
          <!-- {{s.active}} -->
        </div>
      </div>

      <div class="footer-element pastille" @click="swiped('left')">
        <i
          class="icon-arrow icon-next"
          v-if="currentSlide < slides.length - 1"
        ></i>
      </div>
      <i class="icon-share share" @click="sharePic"></i>
    </div>
  </div>
</template>

<script lang="ts">
import PictureContentWithOrder from "@/pojos/PictureContentWithOrder";
import { Component, Prop, Vue } from "vue-property-decorator";
import FooterButton from "@/components/layout/FooterButton.vue";
import ShareService from "@/services/ShareService";

@Component({
  components: { FooterButton },
})
export default class PictureModalMobileGallerySlider extends Vue {
  @Prop() src: string;
  @Prop({ default: false }) deleteButton: boolean;
  @Prop() otherPics: PictureContentWithOrder[];
  @Prop({ default: "" }) measurementPictureSrc: "";
  @Prop() readOnly: boolean;
  slides: GallerySlide[] = [];
  lastSwipe = Date.now();
  currentSlide = 0;

  created(): void {
    for (var i = 0; i < this.otherPics.length; i++) {
      const gallerySlide = new GallerySlide();
      gallerySlide.order = i;
      gallerySlide.src = this.otherPics[i].content;
      this.slides.push(gallerySlide);
    }
    if (this.measurementPictureSrc) {
      const gallerySlide = new GallerySlide();
      gallerySlide.order = i;
      gallerySlide.src = this.measurementPictureSrc;
      gallerySlide.isMeasurementPic = true;
      i++;
      this.slides.push(gallerySlide);
    }
    // Push a last slide for adding new pics
    if (this.slides.length < 5 && !this.readOnly) {
      const gallerySlide = new GallerySlide();
      gallerySlide.order = i;
      gallerySlide.isAddPicButton = true;
      this.slides.push(gallerySlide);
    }
    for (var j = 0; j < this.slides.length; j++) {
      if (this.slides[j].src == this.src) {
        this.currentSlide = j;
      }
    }
  }

  swiped(direction: any): void {
    if (!this.lastSwipe || Date.now() - this.lastSwipe > 500) {
      this.lastSwipe = Date.now();
      if (direction == "right") {
        this.currentSlide = Math.max(0, this.currentSlide - 1);
      } else {
        this.currentSlide = Math.min(this.slides.length, this.currentSlide + 1);
      }
    }
  }

  sharePic(): void {
    ShareService.sharePicture(this.slides[this.currentSlide].src, "prise.png");
  }
}

class GallerySlide {
  order: number;
  src: string;
  isAddPicButton: boolean;
  isMeasurementPic: boolean;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../../less/main";

.mobile-gallery-slider {
  .slider-pic {
    height: 82vh;
    width: 100vw;
    img {
      height: 68vh;
      width: 100vw;
      object-fit: contain;
      background-color: @galery-pick-background;
    }
    .add-picture {
      padding-top: 10vh;
      width: 100vw;
      height: 50vh;
      margin: auto;
      object-fit: contain;
    }
  }

  .footer {
    position: absolute;
    bottom: 0;
    align-content: center;
    align-items: center;
    width: 100vw;
    .pastille {
      float: left;
    }
    .footer-element.steps {
      float: left;
      width: 40vw;
      .step {
        cursor: pointer;
        height: 13px;
        width: 13px;
        margin: 15px 2px 10px 10px;
        background-color: white;
        border-radius: 50%;
        border: 2px solid @pelorous;
        display: inline-block;
        transition: background-color 0.6s ease;

        &:hover {
          background-color: @pelorous;
        }
      }
      .step-active {
        background-color: @pelorous;
      }
    }
  }
}
/* Fading animation */
.fade {
  -webkit-animation-name: fade;
  -webkit-animation-duration: 1.5s;
  animation-name: fade;
  animation-duration: 1.5s;
}

@-webkit-keyframes fade {
  from {
    opacity: 0.4;
  }
  to {
    opacity: 1;
  }
}

@keyframes fade {
  from {
    opacity: 0.4;
  }
  to {
    opacity: 1;
  }
}

.share {
  font-weight: bolder;
  font-size: 28px !important;
  cursor: pointer;
  width: 50px;
}
</style>
