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
  Mode liste « autour de moi » (#5). Pendant du mode carte (MapLibreMap) pour la
  user story pêcheur : depuis sa position, lister les entités hydro (cours d'eau
  / plans d'eau) TRIÉES PAR DISTANCE, avec filtre par type et pagination. Le tri
  et la distance sont calculés côté serveur (PostGIS, endpoint /nearby) ; ce
  composant ne fait que consommer et présenter. Le tap sur un item sélectionne
  l'entité (même effet que taper une géométrie sur la carte).
  -->
<template>
<div class="nearby">
    <i class="icon-error close-button" @click="close" />

    <div class="nearby-header">
        <h3>Plans d'eau autour de moi</h3>
        <div class="kind-filter">
            <button
                type="button"
                :class="{ active: kindFilter === '' }"
                @click="changeFilter('')"
            >Tous</button>
            <button
                type="button"
                :class="{ active: kindFilter === 'STILL' }"
                @click="changeFilter('STILL')"
            >Plans d'eau</button>
            <button
                type="button"
                :class="{ active: kindFilter === 'FLOWING' }"
                @click="changeFilter('FLOWING')"
            >Cours d'eau</button>
        </div>
    </div>

    <div class="nearby-body">
        <div v-if="loading && items.length === 0" class="nearby-state">
            <div class="loader" />
            Recherche de votre position…
        </div>

        <div v-else-if="error" class="nearby-state error">
            <i class="icon-warning" />
            <p>{{ error }}</p>
            <button type="button" class="retry-btn" @click="locate">Réessayer</button>
        </div>

        <div v-else-if="items.length === 0" class="nearby-state">
            <p>Aucune entité hydro dans un rayon de {{ Math.round(radiusM / 1000) }} km.</p>
        </div>

        <ul v-else class="nearby-list">
            <li
                v-for="item in items"
                :key="item.waterEntityId"
                @click="selectItem(item)"
            >
                <span class="item-main">
                    <span class="item-name">{{ item.name }}</span>
                    <span class="item-kind">{{ kindLabel(item.kind) }}</span>
                </span>
                <span class="item-distance">{{ formatDistance(item.distanceM) }}</span>
            </li>
        </ul>

        <button
            v-if="items.length > 0 && hasMore"
            type="button"
            class="more-btn"
            :disabled="loading"
            @click="loadMore"
        >{{ loading ? 'Chargement…' : 'Charger plus' }}</button>
    </div>
</div>
</template>

<script lang="ts">
import { NearbyWaterEntity } from '@/pojos/BackendPojos';
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import ReferentialService from '@/services/ReferentialService';
import GeolocationService from '@/services/GeolocationService';

// Rayon de recherche du mode liste (plus large que le défaut carte pour rester
// utile) et taille de page pour la pagination « Charger plus ».
const RADIUS_M = 5000;
const PAGE_SIZE = 20;

@Component
export default class NearbyList extends Vue {
    @Prop() isVisible: boolean;

    items: NearbyWaterEntity[] = [];
    loading = false;
    error = '';
    kindFilter = '';
    readonly radiusM = RADIUS_M;
    private pageNumber = 0;
    hasMore = false;
    private position: { lat: number; lng: number } | null = null;
    private fetchSeq = 0;

    // Le parent monte ce composant en `v-if` (donc à neuf) à chaque ouverture :
    // la géoloc démarre au mount. Le watch couvre le cas d'une réutilisation
    // d'instance (v-show) le cas échéant.
    mounted() {
        if (this.isVisible) {
            this.locate();
        }
    }

    @Watch('isVisible')
    onVisibilityChange() {
        // À chaque réouverture, on repart d'une géoloc fraîche (la position a pu
        // changer depuis la dernière fois).
        if (this.isVisible) {
            this.locate();
        }
    }

    async locate() {
        this.loading = true;
        this.error = '';
        this.items = [];
        this.pageNumber = 0;
        try {
            const pos = await GeolocationService.checkWatchAndGetPositionUntilTimeout(4000);
            this.position = { lat: pos.coords.latitude, lng: pos.coords.longitude };
            await this.fetchPage(true);
        } catch (err) {
            // Géoloc indisponible (permission refusée, position introuvable,
            // contexte non sécurisé) : message explicite plutôt qu'une liste
            // vide muette. La géoloc desktop est désormais supportée (#16) ;
            // l'échec vient le plus souvent d'une permission refusée.
            this.position = null;
            this.error = "Votre position n'est pas disponible. Autorisez la "
                + "géolocalisation dans votre navigateur puis réessayez, ou "
                + "utilisez la recherche par nom.";
        } finally {
            this.loading = false;
        }
    }

    changeFilter(kind: string) {
        if (this.kindFilter === kind) {
            return;
        }
        this.kindFilter = kind;
        this.pageNumber = 0;
        this.fetchPage(true);
    }

    loadMore() {
        this.pageNumber += 1;
        this.fetchPage(false);
    }

    private async fetchPage(reset: boolean) {
        if (!this.position) {
            return;
        }
        this.loading = true;
        const seq = ++this.fetchSeq;
        try {
            const results = await ReferentialService.getNearby(
                this.position.lat,
                this.position.lng,
                this.radiusM,
                this.kindFilter || undefined,
                this.pageNumber,
                PAGE_SIZE
            );
            // Garde anti-réponse obsolète : un changement de filtre/position plus
            // récent invalide cette réponse.
            if (seq !== this.fetchSeq) {
                return;
            }
            this.items = reset ? results : this.items.concat(results);
            // Page pleine ⇒ il reste probablement des résultats.
            this.hasMore = results.length === PAGE_SIZE;
        } catch (err) {
            if (seq === this.fetchSeq && reset) {
                this.error = "La recherche a échoué. Vérifiez votre connexion et réessayez.";
            }
        } finally {
            if (seq === this.fetchSeq) {
                this.loading = false;
            }
        }
    }

    selectItem(item: NearbyWaterEntity) {
        this.$emit('select', item);
    }

    kindLabel(kind: string): string {
        return kind === 'FLOWING' ? "Cours d'eau" : "Plan d'eau";
    }

    // « 820 m » sous le kilomètre, « 1,2 km » au-delà (séparateur décimal FR).
    formatDistance(m: number): string {
        if (m == null) {
            return '';
        }
        if (m < 1000) {
            return `${Math.round(m)} m`;
        }
        return `${(m / 1000).toFixed(1).replace('.', ',')} km`;
    }

    close() {
        this.$emit('close');
    }
}
</script>

<style scoped lang="less">
.nearby {
    width: 100%;
    position: fixed;
    z-index: 1500;
    bottom: @footer-height;
    max-height: 70vh; // fallback si dvh non supporté
    max-height: calc(100dvh - @header-height - @secondary-header-height - @footer-height - 10px);
    left: 0;
    background-color: white;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    box-shadow: 0 -2px 10px #0003;

    @media screen and (min-width: @desktop-min-width) {
        position: absolute;
        top: 70px;
        left: 0;
        width: 100%;
        height: 500px;
        border: 1px solid #aaa;
        border-radius: 4px;
        box-shadow: 0px 2px 5px #0002;
    }
}

.close-button {
    position: absolute;
    top: 14px;
    right: 14px;
    z-index: 10;
    width: 30px;
    height: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    color: @pale-sky;
    background: #f0f0f0;
    border-radius: 50%;
    cursor: pointer;
    transition: background 0.15s ease, color 0.15s ease;

    &:hover {
        background: @terra-cotta;
        color: white;
    }
}

.nearby-header {
    padding: 20px 20px 10px;
    border-bottom: 1px solid #eee;

    h3 {
        margin: 0 0 10px;
        padding-right: 36px;
        font-size: 1.1rem;
        color: @gunmetal;
    }
}

.kind-filter {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;

    button {
        background: transparent;
        border: 1px solid @pale-sky;
        border-radius: 20px;
        padding: 5px 12px;
        font-size: 0.85rem;
        color: @pale-sky;
        cursor: pointer;

        &.active {
            background: @pelorous;
            border-color: @pelorous;
            color: white;
        }
    }
}

.nearby-body {
    flex: 1;
    overflow-y: auto;
    padding: 10px 0 20px;
}

.nearby-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 15px;
    padding: 40px 30px;
    text-align: center;
    color: @pale-sky;

    &.error {
        color: @gunmetal;
        i {
            font-size: 2rem;
            color: @terra-cotta;
        }
    }

    .retry-btn {
        background: @pelorous;
        color: white;
        border: none;
        border-radius: 20px;
        padding: 8px 20px;
        cursor: pointer;
        &:hover { background: @terra-cotta; }
    }
}

.nearby-list {
    list-style: none;
    margin: 0;
    padding: 0;

    li {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 15px;
        padding: 12px 20px;
        border-bottom: 1px solid #f2f2f2;
        cursor: pointer;

        &:hover {
            background: #0001;
        }
    }

    .item-main {
        display: flex;
        flex-direction: column;
        gap: 2px;
        min-width: 0;
    }
    .item-name {
        color: @gunmetal;
        font-weight: 400;
    }
    .item-kind {
        font-size: 0.8rem;
        color: @pale-sky;
    }
    .item-distance {
        flex-shrink: 0;
        font-weight: 600;
        color: @pelorous;
        white-space: nowrap;
    }
}

.more-btn {
    display: block;
    margin: 15px auto 0;
    background: transparent;
    border: 1px solid @pelorous;
    color: @pelorous;
    border-radius: 20px;
    padding: 8px 20px;
    cursor: pointer;

    &:disabled {
        opacity: 0.5;
        cursor: default;
    }
    &:not(:disabled):hover {
        background: @pelorous;
        color: white;
    }
}
</style>
