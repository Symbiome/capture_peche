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
    <div v-if="mapIsLoading" class="loading">Chargement ...</div>
    <i class="icon-error close-button" @click="closeMap" />
    <l-map ref="map" :options="{ zoomSnap: 0.5, }" style="height: 100%; width: 100%">
        <l-tile-layer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors' />
        <v-marker-cluster>
            <l-marker
                v-for="l in lakes" v-bind:key="l.id"
                :lat-lng="toLatLng(l)"
                :icon="lakeIcon"
                @click="selectLake(l.id)"
            >
                <l-tooltip>{{ l.name }}</l-tooltip>
            </l-marker>
            <l-marker
                v-for="l in favoriteLakes" v-bind:key="l.id"
                :lat-lng="toLatLng(l)"
                :icon="favoriteLakeIcon"
                @click="selectLake(l.id)"
            >
                <l-tooltip>{{ l.name }}</l-tooltip>
            </l-marker>
        </v-marker-cluster>
    </l-map>
</div>
</template>

<script lang="ts">
import { Lake } from '@/pojos/BackendPojos';

import { Component, Prop, Vue } from 'vue-property-decorator';

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
    @Prop() selectedLakes: Lake[];

    map: any;
    mapIsLoading = false;
    lakeIcon = icon({
        iconUrl: "/img/lake.svg",
        iconSize: [32, 37],
        iconAnchor: [16, 37]
    })
    favoriteLakeIcon = icon({
        iconUrl: "/img/heart.svg",
        iconSize: [32, 37],
        iconAnchor: [16, 37]
    })

    created() {
        this.mapIsLoading = true;
    }

    mounted() {
        // @ts-ignore
        this.map = this.$refs.map.mapObject;
        this.zoomToVisibleMarkers();
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
        if (this.selectedLakes != null && this.selectedLakes.length == 1) {
            markers = this.selectedLakes;
        }
        if (this.map && markers.length > 0) {
            this.map.fitBounds(
                markers.map(lake => { return [lake.latitude, lake.longitude] }),
                {
                    padding: [20, 20],
                    maxZoom: 10
                }
            );
            this.mapIsLoading = false;
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
<style lang="less">
@import "../../less/main";

.map {
    width: 100%;
    height: 100%;
    position: relative;
}

.loading {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background-color: #aaa;
    z-index: 99999;
}

.close-button {
    position: absolute;
    top: 10px;
    right: 10px;
    background-color: @pelorous;
    color: white;
    z-index: 99999;
    font-size: 30px;
    box-shadow: 0 0 2px #0002;
    border-radius: 50%;
    cursor: pointer;
    &:hover {
        background-color: @terra-cotta;
    }
}
</style>