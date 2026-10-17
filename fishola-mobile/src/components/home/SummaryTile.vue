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
<template>
  <div class="summary-tile" :class="{ locked: locked }" v-on:click="$emit('clicked')">
    <div class="tile-icon">
      <img v-if="icon" :src="icon" alt="" />
      <span v-else class="tile-emoji">{{ emoji }}</span>
    </div>
    <div class="tile-value" v-if="value !== undefined">{{ value }}</div>
    <div class="tile-label">{{ label }}</div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-facing-decorator";

/** Tuile carrée de l'accueil : pictogramme, valeur et libellé. */
@Component({ emits: ["clicked"] })
export default class SummaryTile extends Vue {
  @Prop() icon?: string;
  @Prop() emoji?: string;
  @Prop() value?: string | number;
  @Prop() label!: string;
  @Prop({ default: false }) locked: boolean;
}
</script>

<style scoped lang="less">
.summary-tile {
  display: flex;
  flex-direction: column;
  align-items: center;

  aspect-ratio: 1;
  padding: 11px 6px 0 6px;
  border-radius: 6.619px;
  background-color: @surface;
  opacity: 0.99;

  color: @gunmetal;
  text-align: center;
  line-height: 1.2;
  overflow: hidden;

  @media screen and (min-width: @desktop-min-width) {
    aspect-ratio: auto;
    padding-bottom: 16px;
  }

  .tile-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 41px;
  }

  .tile-emoji {
    font-size: 32px;
    line-height: 41px;
  }

  .tile-value {
    margin-top: 1px;
    font-size: 22px;
    font-weight: 700;
  }

  .tile-label {
    margin-top: -3px;
    font-size: 14.562px;
    font-weight: 500;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &.locked {
    .tile-emoji {
      filter: grayscale(100%);
      opacity: 0.4;
    }

    .tile-label {
      margin-top: 6px;
      opacity: 0.6;
    }
  }
}
</style>
