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
  Bascule Plan / Satellite commune à toutes les cartes MapLibre (#199). Posée
  en bas à gauche, à l'opposé de l'attribution (bas droite) et du zoom (haut
  gauche) : elle n'est plus masquée par les bandeaux et pop-ups affichés en
  haut de carte, et sa taille en fait une cible tactile confortable.
  -->
<template>
    <button type="button" class="base-layer-toggle" @click="$emit('toggle')">
        <i class="icon-map" />
        {{ baseLayer === 'plan' ? 'Satellite' : 'Plan' }}
    </button>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import { BaseLayer } from '@/components/common/maplibreStyle';

@Component
export default class BaseLayerToggle extends Vue {
    @Prop({ default: 'plan' }) baseLayer!: BaseLayer;
}
</script>

<style scoped lang="less">
.base-layer-toggle {
    position: absolute;
    bottom: 12px;
    left: 12px;
    z-index: 500;
    display: flex;
    align-items: center;
    gap: 8px;
    min-height: 44px;
    padding: 10px 18px;
    background-color: @pelorous;
    color: @white;
    border: none;
    border-radius: 22px;
    font-size: 1rem;
    font-weight: bold;
    cursor: pointer;
    box-shadow: 0 1px 4px #0004;

    &:hover {
        background-color: @terra-cotta;
    }
}
</style>
