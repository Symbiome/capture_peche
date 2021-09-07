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
  <div class="preconisations" v-touch:swipe="swiped">
    <h4>
      Préconisations
    </h4>
    <div
      v-for="slide in slides"
      :key="slide.order"
      class="slide fade"
      v-show="currentSlide == slide.order"
      v-touch:swipe="swiped"
    >
      <p class="preco-text" v-touch:swipe="swiped">{{ slide.text }}</p>
      <div class="do">
        <div class="conformity">CONFORME</div>
        <div class="frame">
          <img
            class="warning-picture"
            :src="slide.doPicPath"
            v-touch:swipe="swiped"
          />
        </div>
      </div>
      <div class="do-not">
        <div class="conformity">NON CONFORME</div>
        <div class="frame">
          <img
            class="warning-picture"
            :class="{ firstKo: slide.order == 1 }"
            :src="slide.donotPicPath"
            v-touch:swipe="swiped"
          />
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
  lastSwipe = Date.now();

  mounted() {
    this.slides = new Array<Slide>();
    this.slides.push(
      new Slide(
        1,
        "Votre prise et le marqueur doivent être intégralement visibles sur la photo et ne pas toucher les bords de l'écran.",
        "/img/preco_ok1.svg",
        "/img/preco_ko1.svg"
      )
    );
    this.slides.push(
      new Slide(
        2,
        "Placez votre prise allongée sur le franc et le marqueur à côté.",
        "/img/preco_ok1.svg",
        "/img/preco_ko2.svg"
      )
    );
    this.slides.push(
      new Slide(
        3,
        "Placez la prise sur un fond relativement uni, si possible sans objet parasite (matériel de pêche, outils, rainures) visible sur la photo.",
        "/img/preco_ok1.svg",
        "/img/preco_ko3.svg"
      )
    );
    this.currentSlide = 1 + Math.floor(Math.random() * this.slides.length);
  }

  swiped(direction: any): void {
    if (!this.lastSwipe || Date.now() - this.lastSwipe > 500) {
      this.lastSwipe = Date.now();
      if (direction == "right") {
        this.currentSlide = Math.max(1, this.currentSlide - 1);
      } else {
        this.currentSlide = Math.min(this.slides.length, this.currentSlide + 1);
      }
    }
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

.preco-text {
  font-size: 14px;
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
    background-color: white;
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
    background-color: white;
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
  height: 20vw;
  max-width: 30vw;
  margin-left: auto;
  margin-right: auto;
  @media screen and (min-width: @desktop-min-width) {
    max-width: 18vw;
  }

  &.firstKo {
    margin-left: 0px;
    margin-top: 0px;
  }
}

.dotter {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 1vh;
  width: 90%;
}
</style>
