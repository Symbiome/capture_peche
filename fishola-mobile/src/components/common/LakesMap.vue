<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
<div class="map">
    <i class="icon-error close-button" @click="closeMap" />
    <l-map  ref="map" :options="{ zoomSnap: 0.5, }" style="height: 100%; width: 100%">
        <l-tile-layer
            v-on:load="mapIsLoading = false"
            v-on:loading="mapIsLoading = true"
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
        />
        <v-marker-cluster>
            <l-marker
                v-for="l in lakes" v-bind:key="l.id"
                :lat-lng="toLatLng(l)"
                :icon="selectedLake?.id == l.id ? selectedLakeIcon : lakeIcon"
                @click="selectLake(l.id)"
            >
                <l-tooltip>{{ l.name }}</l-tooltip>
            </l-marker>
            <l-marker
                v-for="l in favoriteLakes" v-bind:key="l.id"
                :lat-lng="toLatLng(l)"
                :icon="selectedLake?.id == l.id ? selectedLakeIcon : favoriteLakeIcon"
                @click="selectLake(l.id)"
            >
                <l-tooltip>{{ l.name }}</l-tooltip>
            </l-marker>
        </v-marker-cluster>
    </l-map>
    <div v-if="mapIsLoading" class="is-loading">
        <div class="loader" />
        Chargement ...
    </div>
</div>
</template>

<script lang="ts">
import { WaterEntity as Lake } from '@/pojos/BackendPojos';

import { Component, Prop, Vue, Watch } from 'vue-property-decorator';

import L, { latLng, Icon, icon } from "leaflet";
import Vue2LeafletMarkerCluster from "vue2-leaflet-markercluster";

type D = Icon.Default & {
    _getIconUrl?: string;
};

delete (Icon.Default.prototype as D)._getIconUrl;
import "leaflet/dist/leaflet.css";
import "leaflet.markercluster/dist/MarkerCluster.css";
import "leaflet.markercluster/dist/MarkerCluster.Default.css";
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';
Icon.Default.mergeOptions({
    iconRetinaUrl: markerIcon2x,
    iconUrl: markerIcon,
    shadowUrl: markerShadow,
});

import { LMap, LTileLayer, LMarker, LPopup, LIcon, LTooltip } from "vue2-leaflet";

@Component({
    components: {
    LMap,
    LTileLayer,
    LMarker,
    LPopup,
    LIcon,
    LTooltip,
    "v-marker-cluster": Vue2LeafletMarkerCluster
    }
})
export default class LakesMap extends Vue {
    @Prop() lakes: Lake[];
    @Prop() favoriteLakes: Lake[];
    @Prop() selectedLake: Lake;
    @Prop() isVisible: boolean;

    map: any;
    lakeIcon = icon({
        iconUrl: "/img/lake.svg",
        iconSize: [32, 37],
        iconAnchor: [16, 37]
    })
    selectedLakeIcon = icon({
        iconUrl: "/img/selected.svg",
        iconSize: [32, 37],
        iconAnchor: [16, 37]
    })
    favoriteLakeIcon = icon({
        iconUrl: "/img/heart.svg",
        iconSize: [32, 37],
        iconAnchor: [16, 37]
    })
    mapIsLoading = false;

    @Watch("isVisible")
    redrawMapIfVisible() {
        if (this.isVisible) {
            this.mapReady();
        }
    }

    isFavorite(lake:Lake) {
        return this.favoriteLakes && this.favoriteLakes.some(fav => fav.id === lake.id);
    }

    toggleFavorite(lake: Lake) {
        const index = this.favoriteLakes.findIndex(fav => fav.id === lake.id);
        if (index === -1) {
        this.favoriteLakes.push(lake);
        } else {
        this.favoriteLakes.splice(index, 1);
        }
        this.$emit('favoriteLakesChanged', this.favoriteLakes);
    }

    zoomToVisibleMarkers() {
        let markers = this.lakes;
        if (this.favoriteLakes != null && this.favoriteLakes.length > 0) {
            markers = this.favoriteLakes;
        }
        if (this.selectedLake != null) {
            markers = [this.selectedLake];
        }
        if (this.map && markers.length > 0) {
            this.map.fitBounds(
                markers.map(lake => { return [lake.latitude, lake.longitude] }),
                {
                    padding: [20, 20],
                    maxZoom: 10
                }
            );
            this.$emit('zoomed');
        }
    }

    mapReady() {
        if (this.$refs.map) {
            this.map = this.$refs.map.mapObject;
            this.map.invalidateSize(false); /* To force the redraw of the map */
            this.zoomToVisibleMarkers();
        }
    }

    toLatLng(lake: Lake) {
        return latLng(lake.latitude, lake.longitude);
    }

    selectLake(id: string) {
        this.$emit('selectLake', id);
    }
    closeMap() {
        this.$emit('close');
    }
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">


.map {
    width: 100%;
    height: 100%;
    position: relative;
}


.map {
  position: fixed;
  z-index: 1500;
  bottom: @footer-height;
  max-height: 70vh; // fallback if dvh is not supported
  max-height: calc(100dvh - @header-height - @secondary-header-height - @footer-height - 10px);
  left: 0;
  background-color: @black-alpha-90;
  transition: opacity 0.3s ease;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top-left-radius: 30px;
  border-top-right-radius: 30px;

  @media screen and (min-width: @desktop-min-width) {
    position: absolute;
    top: 70px;
    left: 0;
    width: 100%;
    height: 500px !important;
    border: 1px solid #aaa;
    border-radius: 4px;
    box-shadow: 0px 2px 5px #0002;
  }
}
.is-loading {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 9999999;
  background: #fffa;
  display: flex;
  flex-direction: column;
  gap: 30px;
  align-items: center;
  justify-content: center;
    font-size: 1.2rem;
    font-weight: bold;
    color: #777;
  height: 100%;
  width: 100%;
}

.close-button {
    position: absolute;
    top: 20px;
    right: 20px;
    background-color: @pelorous;
    color: white;
    z-index: 99999;
    font-size: 40px;
    box-shadow: 0 0 2px #0002;
    border-radius: 50%;
    cursor: pointer;
    &:hover {
        background-color: @terra-cotta;
    }

    @media screen and (min-width: @desktop-min-width) {
        top: 10px;
        right: 10px;
        font-size: 30px;
    }
}

</style>