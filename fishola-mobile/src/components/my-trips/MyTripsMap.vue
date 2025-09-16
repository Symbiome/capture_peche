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
    <div class="pane " v-if="visible">
        <span v-if="mapIsLoading" class="is-loading">
            <div class="loader" />
            Chargement de la carte...
        </span>
        <div id="info" class="info" v-if="validMarkers.length > 0" v-show="showPersonnalMapWarning">
            Cette carte n'est visible que par vous. Les coordonnées de vos prises ne sont pas divulgées aux autres
            pêcheurs.
            <i class="icon icon-plus close" @click="showPersonnalMapWarning = false"></i>
        </div>
        <div class="map" v-if="validMarkers.length > 0">
            <l-map ref="map" @ready="mapReady" :options="{ zoomSnap: 0.5, }" style="height: 100%; width: 100%">
                <l-tile-layer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors' />
                <v-marker-cluster>
                    <l-marker v-for="c in validMarkers" v-bind:key="c.id" :lat-lng="toLatLng(c)"
                        :icon="c.maillage == 'MAILLEE' ? icon1 : icon2">
                        <!--<l-icon :icon-anchor="[16, 16]" :icon-size="[32, 37]">
                            <div class="custom-icon">
                                <div class="headline">
                                    {{ c.specieName }}
                                </div>
                                <i class="icon-fish" />
                            </div>
                        </l-icon>-->
                        <l-popup class="catch-marker">
                            <p class="title">{{ c.tripName }}</p>

                            <p>
                                <i class="fish icon-fish" />
                                {{ c.specieName }}
                                {{ c.maillage === 'NON_DEFINI' ? ''
                                : (c.maillage == 'MAILLEE' ?
                                '(maillé)' : '(non maillé)')
                                }}
                            </p>

                            <p class="infos">
                                <span class="trip-date">{{ formattedDate(c.date) }}</span> - {{ c.lakeName }}
                            </p>
                            <button class="button" @click="showCatch(c)">Voir la sortie</button>
                        </l-popup>
                    </l-marker>
                </v-marker-cluster>
            </l-map>
        </div>
        <div class="error-markers" v-if="invalidMarkers.length > 0">
            <b>{{ invalidMarkers.length }}</b> prises sans position renseignée
            <!-- <ul>
                <li v-for="c in invalidMarkers" :key="c.id">
                    {{ formattedDate(c.date) }} - {{ c.specieName }} ({{ c.lakeName }} {{ c.tripName }})
                </li>
            </ul>-->

        </div>
        <div v-if="!mapIsLoading && validMarkers.length == 0 && invalidMarkers.length == 0">
            Aucune sortie enregistrée
        </div>
    </div>
</template>

<script lang="ts">

import { CatchMarker } from '@/pojos/BackendPojos';
import TripsService from '@/services/TripsService';

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

import { LMap, LTileLayer, LMarker, LPopup, LIcon } from "vue2-leaflet";
import Helpers from '@/services/Helpers';

@Component({
    components: {
        LMap,
        LTileLayer,
        LMarker,
        LPopup,
        LIcon,
        "v-marker-cluster": Vue2LeafletMarkerCluster
    }
})
export default class MyTripsMapView extends Vue {
    @Prop()
    visible: boolean;
    validMarkers: CatchMarker[] = [];
    invalidMarkers: CatchMarker[] = [];
    center = latLng(46.071623, 5.890511);
    showPersonnalMapWarning = true;
    icon1 = icon({
        iconUrl: "/img/fish-blue.svg",
        iconSize: [32, 37],
        iconAnchor: [16, 37]
    })
    icon2 = icon({
        iconUrl: "/img/fish-yellow.svg",
        iconSize: [32, 37],
        iconAnchor: [16, 37]
    })
    map: any;
    mapIsLoading = false;

    mounted() {
        this.computeMapIfVisible();
    }

    @Watch("visible")
    computeMapIfVisible() {
        if (this.visible && this.validMarkers.length == 0) {
            this.mapIsLoading = true;
            TripsService.catchMarkers().then(
                (markers) => {
                    this.validMarkers = markers.filter((m: CatchMarker) => m.hasValidCoordinates)
                    this.invalidMarkers = markers.filter((m: CatchMarker) => !m.hasValidCoordinates)
                    this.zoomToVisibleMarkers();
                },
                (error: Error) => { console.error(error) }
            );
        }
    }

    toLatLng(marker: CatchMarker) {
        return latLng(marker.latitude, marker.longitude);
    }

    formattedDate(tripDate: Date): string {
        const dayOptions: Intl.DateTimeFormatOptions = {
            month: "numeric",
            day: "numeric",
            year: "numeric",
        };
        // @ts-ignore
        const date = Helpers.parseLocalDate(tripDate);
        const dateString = date.toLocaleDateString("fr-FR", dayOptions);
        return dateString;
    }

    zoomToVisibleMarkers() {
        if (this.map && this.validMarkers.length > 0) {
            const visibleLayerGroup = new L.FeatureGroup();

            this.map.eachLayer(function (layer: L.Layer) {
                if (layer instanceof L.Marker)
                    visibleLayerGroup.addLayer(layer);
            });

            const bounds = visibleLayerGroup.getBounds();
            this.map.fitBounds(bounds);
        }
        this.mapIsLoading = false;
    }

    mapReady() {
        // @ts-ignore
        this.map = this.$refs.map.mapObject;
        this.zoomToVisibleMarkers();
    }

    showCatch(c: CatchMarker) {
        this.$router.push({
            name: 'catch',
            params: {
                'tripId': c.tripId,
                'catchId': c.id
            }
        });
    }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.info {
    position: absolute;
    right: 50px;
    width: 70vw;
    z-index: 997;
    padding: 20px;
    margin-top: 10px;
    margin-bottom: 10px;
    border: 2px solid @very-light-grey;
    background-color: @white-smoke;
    border-radius: 10px;

    .close {
        transform: rotate(45deg);
        cursor: pointer;
        position: absolute;
        top: 5px;
        right: 5px;
    }
}

.map {
    height: 80vh;

    .catch-marker {
        .title {
            font-size: 18px;
            color: @pelorous;
        }

        .trip-date {
            font-weight: bolder;
        }

        .fish {
            color: @pelorous;
        }

        .infos {
            padding-bottom: 10px;
        }

        .button {
            height: 50px;
            border-radius: 50px;

            font-style: normal;
            font-weight: bold;
            font-size: @fontsize-button;
            line-height: calc(@fontsize-button + @line-height-padding-x-large);

            border: 0px;
            padding-left: @margin-medium;
            padding-right: @margin-medium;

            background-color: @terra-cotta;
            color: @white;

            &:hover {
                background-color: @white;
                color: @terra-cotta;
                border: 2px solid @terra-cotta;
            }
        }
    }
}

.error-markers {
    width: 100%;
    color: @cardinal;
    text-align: right;
    margin-top: @margin-small;
}

.custom-icon {
    background-color: @cyprus;
    padding: 10px;
    border: 1px solid #333;
    border-radius: 20px 20px 20px 20px;
    text-align: center;

    .icon-fish {
        color: @pelorous;
    }
}

.is-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 30px;
    margin: 70px;
}
</style>
