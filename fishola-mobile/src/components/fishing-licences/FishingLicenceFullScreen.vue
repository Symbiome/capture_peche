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
    <pinch-zoom class="bg" v-if="type != 'PDF'">
        <span @click="goBack" class="back-arrow">
            <i class="icon-news-back icon-arrow" />
            <span class="back-text"> Retour </span>
        </span>
        <img v-if="url" class="fullscreen-img" :src="url" />
        <span v-else>Chargement en cours...</span>
    </pinch-zoom>
    <div class="bg" v-else>
        <span @click="goBack" class="back-arrow" id="back-arrow">
            <i class="icon-news-back icon-arrow" />
            <span class="back-text"> Retour </span>
        </span>
        <vue-pdf-app v-if="url" class="fullscreen-pdf" :pdf="url" @pages-rendered="pagesRenderedHandler" />
        <span v-else>Chargement en cours...</span>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";
import VuePdfApp from "vue-pdf-app";
import PinchZoom from 'vue-pinch-zoom';
import "vue-pdf-app/dist/icons/main.css";
import Hammer from 'hammerjs';

import FishingLicenceService from "@/services/FishingLicenceService";

@Component({
    components: { VuePdfApp, PinchZoom }
})
export default class FishingLicenceFullScreen extends Vue {
    @Prop() id: string;
    @Prop() type: string;
    url = ""
    pdfViewer?: any;
    loaded = false;
    pageScale = 2;

    mounted() {
        setTimeout(async () => {
            const fileBlob = await FishingLicenceService.getLicenceFile(
                this.id
            );
            this.url = URL.createObjectURL(fileBlob);
        });
    }

    goBack() {
        this.$router.go(-1);
    }

    pagesRenderedHandler(pdfApp: any) {
        this.pdfViewer = pdfApp.pdfViewer;
        this.loaded = true;
        const maxScale = 5;
        const targetPDFElement = document.getElementById("vuePdfApp");
        if (targetPDFElement && targetPDFElement.parentElement) {
            const element = document.getElementById("vuePdfApp")!;
            const hammertime = new Hammer(element, {});

            // pinch is usually disabled as it makes the element 
            // blocking, and pan is limited to x-axis, so enable these
            hammertime.get('pinch').set({ enable: true });
            hammertime.get('pan').set({ direction: Hammer.DIRECTION_ALL });

            const container = element.parentElement!,
                containerHeight = container.offsetHeight,
                containerWidth = container.offsetWidth,
                screenWidth = document.body.clientWidth,
                screenHeight = document.body.clientHeight,
                elementHeight = element.offsetHeight,
                elementWidth = element.offsetWidth;

            let currentScale = 1,
                offsetX = 0,
                offsetY = 0,
                panOffsetX = 0,
                panOffsetY = 0,
                pinchCentreX = 0,
                pinchCentreY = 0;

            hammertime.on('pinchstart', function (e) {
                document.getElementById('back-arrow')!.style.zIndex = "-1";
                // record starting offset at beginning of pinch operation
                panOffsetX = offsetX;
                panOffsetY = offsetY;

                // record the original centre point of the pinch, relative to the element
                // e.center seems to return values relative to screen, so use screen dimensions.                
                pinchCentreX = Math.round((e.center.x - panOffsetX - (screenWidth / 2)) / currentScale);
                pinchCentreY = Math.round((e.center.y - panOffsetY - (screenHeight / 2)) / currentScale);

            });


            hammertime.on('pinch', function (e) {
                // don't allow scales less than 1, or greater than maxScale
                // e.scale is relative to the start of the current pinch operation, not the last event
                let scale = Math.min(maxScale, Math.max(1, currentScale * e.scale));

                offsetX = panOffsetX + Math.round((pinchCentreX * (1 - e.scale)));
                offsetY = panOffsetY + Math.round((pinchCentreY * (1 - e.scale)));

                // allow for dragging (i.e. panning) while pinching
                offsetX += e.deltaX;
                offsetY += e.deltaY;

                // constrain edges
                let overlapX = Math.max(0, Math.round(((elementWidth * scale) - containerWidth) / 2));
                let overlapY = Math.max(0, Math.round(((elementHeight * scale) - containerHeight) / 2));
                offsetX = Math.max(-overlapX, Math.min(overlapX, offsetX));
                offsetY = Math.max(-overlapY, Math.min(overlapY, offsetY));

                // order of transforms is important
                let transforms = [
                    'translate(' + offsetX + 'px,' + offsetY + 'px)',
                    'scale(' + scale + ')'
                ];
                element.style.transform = transforms.join(' ');

            });


            hammertime.on('pinchend', function (e) {
                // update current scale ready for next pinch or pan operation
                currentScale = Math.min(maxScale, Math.max(1, currentScale * e.scale));
                if (currentScale <= 1) {
                    document.getElementById('back-arrow')!.style.zIndex = "99";
                }
            });


            hammertime.on('panstart', function () {
                panOffsetX = offsetX;
                panOffsetY = offsetY;
            });


            hammertime.on('panmove', function (e) {
                let overlapX = Math.max(0, Math.round(((elementWidth * currentScale) - containerWidth) / 2)),
                    overlapY = Math.max(0, Math.round(((elementHeight * currentScale) - containerHeight) / 2));

                panOffsetX = Math.max(-overlapX, Math.min(overlapX, offsetX + e.deltaX));
                panOffsetY = Math.max(-overlapY, Math.min(overlapY, offsetY + e.deltaY));

                // order of transforms is important    
                let transforms = [
                    'translate(' + panOffsetX + 'px,' + panOffsetY + 'px)',
                    'scale(' + currentScale + ')'
                ];
                element.style.transform = transforms.join(' ');

            });


            hammertime.on('panend', function (e) {
                // Record final position here to take account of constraint calculations in 
                // panmove handler; magnitude of e.deltaX may have been limited.
                offsetX = panOffsetX;
                offsetY = panOffsetY;
            });
        } else {
            console.error("Cannot find PDF Viewer")
        }
    }

}
</script>


<style scoped lang="less">
@import "../../less/main";

.bg {
    padding-top: calc(5px + env(safe-area-inset-top));
    width: 100%;
    height: 100%;
}

.fullscreen-img {
    padding-top: calc(40px + env(safe-area-inset-top));
    height: 100vh;
    width: auto;
}

.fullscreen-pdf {
    padding-top: calc(40px);
    height: 100vh;
    width: 100%;
}

.back-arrow {
    height: 40px;
    position: absolute;
    top: calc(5px + env(safe-area-inset-top));
    z-index: 99;
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