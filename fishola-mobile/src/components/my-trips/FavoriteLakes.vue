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
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../../less/main";

</style>
