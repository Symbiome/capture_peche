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
<div>
    <p v-for="l in allLakes" :key="l.id">
        <input
            type="checkbox"
            :checked="isFavorite(l)"
            @change="toggleFavorite(l)"
        />
        {{ l.name}}
    </p>  
</div>
</template>


<script lang="ts">
import { Lake, } from '@/pojos/BackendPojos';

import { Component,  Vue } from 'vue-property-decorator';
import ReferentialService from '@/services/ReferentialService';

@Component({
  components: {
  }
})
export default class SettingsView extends Vue {
    allLakes: Lake[] = [];
    favoriteLakes: Lake[] = [];

    mounted() {
        this.loadLakes();
    }

    async loadLakes() {
        this.allLakes = await ReferentialService.getLakes();
        this.favoriteLakes = await ReferentialService.getFavoriteLakes();
    }

    isFavorite(lake:Lake) {
        return this.favoriteLakes.some(fav => fav.id === lake.id);
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

    zoomToFavoriteMarkers() {
        if (this.map && this.favoriteLakes.length > 0) {
            this.map.fitBounds(
                this.favoriteLakes.map(lake => { return [lake.latitude, lake.longitude] }),
                {padding: [20, 20]}
            );
            this.mapIsLoading = false;
        } else {
            this.mapIsLoading = false;
        }
    }

    mapReady() {
        // @ts-ignore
        this.map = this.$refs.map.mapObject;
        this.zoomToFavoriteMarkers();
    }

    toLatLng(lake: Lake) {
        return latLng(lake.latitude, lake.longitude);
    }
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
</style>
