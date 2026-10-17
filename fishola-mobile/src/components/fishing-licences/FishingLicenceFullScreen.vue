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
    <div class="bg">
        <span @click="goBack" class="back-arrow" :class="{ 'behind-content': zoomed }">
            <i class="icon-news-back icon-arrow" />
            <span class="back-text"> Retour </span>
        </span>
        <template v-if="url">
            <div class="zoom-container" v-if="type != 'PDF'">
                <img ref="zoomTarget" class="fullscreen-img" :src="url" @load="enableZoom" />
            </div>
            <div class="zoom-container fullscreen-pdf" v-else-if="pdfComponent">
                <div ref="zoomTarget" class="pdf-zoom-target">
                    <div class="pdf-pages">
                        <component :is="pdfComponent" :source="url" @rendered="enableZoom" />
                    </div>
                </div>
            </div>
        </template>
        <span v-else>Chargement en cours...</span>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-facing-decorator";
import { markRaw } from "vue";

import FishingLicenceService from "@/services/FishingLicenceService";
import { enablePinchZoom } from "@/services/PinchZoom";

@Component
export default class FishingLicenceFullScreen extends Vue {
    @Prop() id: string;
    @Prop() type: string;
    url = "";
    zoomed = false;
    pdfComponent: any = null;
    disableZoom?: () => void;

    async mounted() {
        if (this.type === "PDF" && import.meta.env.VITE__REMOVE_PDF_VIEWER !== "true") {
            // vue-pdf-embed épinglé en 2.1.4 (pdf.js 4.10) : à partir de la 2.1.5,
            // pdf.js 5+ exige Promise.withResolvers jusque dans son worker, absent
            // avant iOS 17.4 / Chrome 119, alors que .browserslistrc vise iOS 15.
            const module = await import("vue-pdf-embed");
            this.pdfComponent = markRaw(module.default);
        }
        setTimeout(async () => {
            const fileBlob = await FishingLicenceService.getLicenceFile(
                this.id
            );
            this.url = URL.createObjectURL(fileBlob);
        });
    }

    beforeUnmount() {
        this.disableZoom?.();
    }

    goBack() {
        this.$router.go(-1);
    }

    enableZoom() {
        const target = this.$refs.zoomTarget as HTMLElement | undefined;
        if (!target || this.disableZoom) {
            return;
        }
        this.disableZoom = enablePinchZoom(target, (zoomed) => (this.zoomed = zoomed));
    }
}
</script>


<style scoped lang="less">
.bg {
    padding-top: calc(5px + env(safe-area-inset-top));
    width: 100%;
    height: 100%;
}

.zoom-container {
    width: 100%;
    height: 100vh;
    overflow: hidden;
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
    box-sizing: border-box;

    // Comme l'ancien visualiseur : le zoom s'applique à toute la zone, et les
    // pages défilent nativement à l'intérieur.
    .pdf-zoom-target,
    .pdf-pages {
        height: 100%;
    }

    .pdf-pages {
        overflow-y: auto;
    }
}

.back-arrow {
    height: 40px;
    position: absolute;
    top: calc(5px + env(safe-area-inset-top));
    z-index: 99;
    right: 5px;

    &.behind-content {
        z-index: -1;
    }
}

.icon-news-back {
    color: @accent-text;
    cursor: pointer;
    display: inline-block;
    font-size: 22px;
    transform: rotate(180deg);
}
</style>