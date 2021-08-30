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
  <div class="preconisations">
    <h4>
      Préconisations
    </h4>
    <div
      v-for="slide in slides"
      :key="slide.order"
      class="slide fade"
      v-show="currentSlide == slide.order"
    >
      <p class="preco-text">{{ slide.text }}</p>
      <div class="do">
        <div class="conformity">CONFORME</div>
        <div class="frame">
          <img class="warning-picture" :src="slide.doPicPath" />
        </div>
      </div>
      <div class="do-not">
        <div class="conformity">NON CONFORME</div>
        <div class="frame">
          <img class="warning-picture" :src="slide.donotPicPath" />
        </div>
      </div>
    </div>
    <div style="clear:both" />
    <div class="dotter">
      <span
        class="dot"
        :class="{
          active: slide.order === currentSlide,
        }"
        v-for="slide in slides"
        :key="slide.order"
        @click="currentSlide = slide.order"
      >
      </span>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";

@Component({
  components: {},
})
export default class MeasurementPictureSlider extends Vue {
  slides: Slide[] = new Array<Slide>();
  currentSlide = 1;

  mounted() {
    this.slides = new Array<Slide>();
    this.slides.push(
      new Slide(
        1,
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus id diam id tellus eleifend sodales. Donec a pellentesque dui. Maecenas vitae feugiat tortor, ut lobortis nisl.",
        "/img/default_marker.jpg",
        "/img/illustration_fish.svg"
      )
    );
    this.slides.push(
      new Slide(
        2,
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus id diam id tellus eleifend sodales. Donec a pellentesque dui. Maecenas vitae feugiat tortor, ut lobortis nisl.",
        "/img/illustration_fish.svg",
        "/img/default_marker.jpg"
      )
    );
    this.slides.push(
      new Slide(
        3,
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus id diam id tellus eleifend sodales. Donec a pellentesque dui. Maecenas vitae feugiat tortor, ut lobortis nisl.",
        "/img/default_marker.jpg",
        "/img/illustration_fish.svg"
      )
    );
    this.currentSlide = 1 + Math.floor(Math.random() * this.slides.length);
  }
}

class Slide {
  order: number;
  text: string;
  doPicPath: string;
  donotPicPath: string;

  constructor(
    order: number,
    text: string,
    doPicPath: string,
    donotPicPath: string
  ) {
    this.order = order;
    this.text = text;
    this.doPicPath = doPicPath;
    this.donotPicPath = donotPicPath;
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../../less/main";

.dot {
  cursor: pointer;
  height: 13px;
  width: 13px;
  margin: 15px 2px 10px 10px;
  background-color: white;
  border-radius: 50%;
  border: 2px solid @pelorous;
  display: inline-block;
  transition: background-color 0.6s ease;
}

.active,
.dot:hover {
  background-color: @pelorous;
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
.conformity {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: -3px;
}
.do-not {
  float: left;
  margin-left: 1vw;
  width: 45%;
  .frame {
    border: 5px solid @cardinal;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .conformity {
    color: @cardinal;
  }
}

.do {
  float: left;
  margin-right: 1vw;
  width: 45%;
  .frame {
    border: 5px solid @lime-green;

    display: flex;
    align-items: center;
    justify-content: center;
  }
  .conformity {
    color: @lime-green;
  }
}
.warning-picture {
  height: 28vh;
  max-width: 20vw;
  margin-left: auto;
  margin-right: auto;
}
.dotter {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 1vh;
  width: 90%;
}
</style>
