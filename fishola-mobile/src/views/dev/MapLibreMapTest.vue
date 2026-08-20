<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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
<!--
  Banc de test isolé pour MapLibreMap (#8), sur le modèle de la route publique
  /fish-measure-test : permet à l'e2e Cypress de charger la carte sans passer par
  l'authentification ni la création d'un trip. La sélection d'entité est exposée
  dans un attribut data-cy pour l'assertion (stratégie « événements, pas pixels »).
  -->
<template>
    <div class="map-test">
        <div class="map-test-frame">
            <MapLibreMap
                :isVisible="true"
                :lakes="[]"
                :favoriteLakes="[]"
                :selectedLake="null"
                @selectLake="onSelect"
                @map-click="onMapClick"
                @close="noop"
            />
        </div>
        <div class="map-test-readout">
            <span data-cy="selected-entity">{{ selectedEntityId }}</span>
            <span data-cy="last-click">{{ lastClick }}</span>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import MapLibreMap from '@/components/common/MapLibreMap.vue';

@Component({
    components: { MapLibreMap },
})
export default class MapLibreMapTest extends Vue {
    selectedEntityId: string = '';
    lastClick: string = '';

    onSelect(id: string) {
        this.selectedEntityId = id;
    }

    onMapClick(coords: { lng: number; lat: number }) {
        this.lastClick = `${coords.lng.toFixed(5)},${coords.lat.toFixed(5)}`;
    }

    noop() {
        /* le banc de test ne ferme rien */
    }
}
</script>

<style scoped lang="less">
.map-test {
    position: fixed;
    inset: 0;
    display: flex;
    flex-direction: column;
}
.map-test-frame {
    position: relative;
    flex: 1;
}
.map-test-readout {
    padding: 6px 10px;
    font-family: monospace;
    background: #111;
    color: #0f0;
    display: flex;
    gap: 20px;
}
</style>
