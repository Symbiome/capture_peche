<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2024 INRAE - UMR CARRTEL
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
    <pinch-zoom class="bg" v-if="type=='JPEG'">
        <span @click="goBack" class="back-arrow">
                  <i class="icon-news-back icon-arrow" />
                  <span class="back-text"> Retour </span>
        </span>
        <img   class="fullscreen-img" :src="url"/>        
    </pinch-zoom>
    <div class="bg" v-else>
        <span @click="goBack" class="back-arrow">
                  <i class="icon-news-back icon-arrow" />
                  <span class="back-text"> Retour </span>
        </span>
        <vue-pdf-app class="fullscreen-pdf" :pdf="url"/>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";
import VuePdfApp from "vue-pdf-app";
import PinchZoom from 'vue-pinch-zoom';

@Component({
    components: { VuePdfApp, PinchZoom }
})
export default class FishingLicenceFullScreen extends Vue {
    @Prop() url: string;
    @Prop() type: string;

    goBack() {
        this.$router.go(-1);
    }
}
</script>


<style scoped lang="less">
@import "../../less/main";

.bg {
    width: 100%;
    height: 100%;
    background-color: @cyprus;
}
.fullscreen-img {
    padding-top:40px;
    height: 100vh;
    width: auto;
}
.fullscreen-pdf {
    padding-top:40px;
    height: 100vh;
    width: auto;
}
.back-arrow {
    height:40px;
    position: absolute;
    top: 5px;
    right: 5px;
}

 .icon-news-back {
      color: @pelorous;
      cursor: pointer;
      display: inline-block;
      font-size: 22px;
      transform: rotate(180deg);
    }


</style>