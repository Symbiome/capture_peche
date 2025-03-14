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
    <div class="pane ">
        <div class="info">
            Cette carte n'est visible que par vous. Les coordonées de vos prises ne sont pas divulgées aux autres
            pêcheurs.
        </div>
        <div class="map">
            <l-map :zoom="9" :center="center" :options="{
                zoomSnap: 0.5,
            }" style="height: 100%; width: 100%" v-if="markers.length > 0">
                <l-tile-layer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors' />

                <l-marker v-for="c in markers" v-bind:key="c.id" :lat-lng="toLatLng(c)">
                    <l-popup class="catch-marker">
                        <p class="title">{{ c.tripName }}</p>

                        <p>
                            <i class="fish icon-fish" />
                            {{ c.specieName }} ({{
                                c.maillage === 'NON_DEFINI' ? ''
                                    : (c.maillage == 'MAILLEE' ?
                                        'maillé' : 'non maillé') }})
                        </p>

                        <p class="infos">
                            <span class="trip-date">{{ formattedDate(c.date) }}</span> - {{ c.lakeName }}
                        </p>
                    </l-popup>
                </l-marker>
            </l-map>
        </div>
    </div>
</template>

<script lang="ts">

import { CatchMarker } from '@/pojos/BackendPojos';
import TripsService from '@/services/TripsService';

import { Component, Vue } from 'vue-property-decorator';

import { latLng, Icon } from "leaflet";

type D = Icon.Default & {
    _getIconUrl?: string;
};

delete (Icon.Default.prototype as D)._getIconUrl;

Icon.Default.mergeOptions({
    iconRetinaUrl: require("leaflet/dist/images/marker-icon-2x.png"),
    iconUrl: require("leaflet/dist/images/marker-icon.png"),
    shadowUrl: require("leaflet/dist/images/marker-shadow.png"),
});

import { LMap, LTileLayer, LMarker, LPopup } from "vue2-leaflet";
import Helpers from '@/services/Helpers';

@Component({
    components: {
        LMap,
        LTileLayer,
        LMarker,
        LPopup,
    }
})
export default class MyTripsMapView extends Vue {
    markers: CatchMarker[] = [];
    center = latLng(46.071623, 5.890511);

    mounted() {
        TripsService.catchMarkers().then(
            (markers) => {
                console.error(markers);
                this.markers = markers
            },
            (error: Error) => { console.error(error) }
        );
    }

    toLatLng(marker: CatchMarker) {
        return latLng(marker.latitude, marker.longitude);
    }

    formattedDate(tripDate: Date): string {
        var dayOptions: Intl.DateTimeFormatOptions = {
            month: "numeric",
            day: "numeric",
            year: "numeric",
        };
        // @ts-ignore
        const date = Helpers.parseLocalDate(tripDate);
        const dateString = date.toLocaleDateString("fr-FR", dayOptions);
        return dateString;
    }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../../less/main";

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
    }
}
</style>
