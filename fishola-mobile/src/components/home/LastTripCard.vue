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
  <div class="last-trip-card" :class="{ 'no-photo': !photoUrl }">
    <img v-if="photoUrl" class="trip-photo" :src="photoUrl" alt="" />
    <div class="trip-overlay"></div>

    <div class="trip-date">{{ formattedDate }}</div>
    <div class="trip-duration">{{ formattedDuration }}</div>
    <div class="trip-catches">
      <span class="fish-icon"><img src="img/carnet/fish.svg" alt="" /></span>
      <span>{{ trip.catchsCount }}</span>
    </div>

    <div class="trip-place" v-if="waterName">
      <img src="img/carnet/pin.svg" alt="" />
      <span>{{ waterName }}</span>
    </div>
    <button class="trip-open" v-on:click="openTrip">Voir</button>
  </div>
</template>

<script lang="ts">
import { TripLight } from "@/pojos/BackendPojos";
import { RouterUtils } from "@/router/RouterUtils";

import { Component, Prop, Vue } from "vue-property-decorator";

/** Carte « Ma dernière sortie » : photo, date, durée, prises et milieu. */
@Component
export default class LastTripCard extends Vue {
  @Prop() trip!: TripLight;
  @Prop() waterName?: string;
  @Prop() photoUrl?: string;

  get formattedDate(): string {
    return new Date(this.trip.date).toLocaleDateString("fr-FR", {
      day: "numeric",
      month: "long",
    });
  }

  get formattedDuration(): string {
    const totalMinutes = Math.floor((this.trip.durationInSeconds || 0) / 60);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    return hours + "h" + String(minutes).padStart(2, "0");
  }

  openTrip() {
    RouterUtils.pushRouteNoDuplicate(this.$router, {
      name: "trip",
      params: { id: this.trip.id },
    });
  }
}
</script>

<style scoped lang="less">
.last-trip-card {
  position: relative;
  height: 214px;
  border-radius: 13.118px;
  overflow: hidden;

  color: @white;
  font-size: 18px;
  line-height: 1.2;

  &.no-photo {
    background: linear-gradient(135deg, @carnet-blue 0%, @carnet-navy 100%);
  }

  .trip-photo {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .trip-overlay {
    position: absolute;
    inset: 0;
    background-color: rgba(0, 0, 0, 0.3);
  }

  .trip-date,
  .trip-duration,
  .trip-catches,
  .trip-place,
  .trip-open {
    position: absolute;
  }

  .trip-date {
    top: 21px;
    left: 15px;
    font-weight: 500;
  }

  .trip-duration {
    top: 21px;
    right: 15px;
    font-weight: 500;
  }

  .trip-catches {
    top: 44px;
    right: 16px;
    display: flex;
    align-items: center;
    font-weight: 600;

    .fish-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 27.11px;
      height: 27.11px;

      img {
        transform: rotate(-45deg);
      }
    }
  }

  .trip-place {
    left: 11px;
    bottom: 22px;
    display: flex;
    align-items: center;
    gap: 3.68px;
    max-width: calc(100% - 110px);
    font-weight: 400;

    span {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .trip-open {
    right: 16px;
    bottom: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 30px;
    min-width: 63px;
    padding: 10px;
    border: none;
    border-radius: 10px;
    background-color: @white;
    color: @carnet-navy;
    font-size: 18px;
    font-weight: 500;
    letter-spacing: @letter-spacing;

    &:active {
      animation: none;
      background-color: @white;
      color: @carnet-navy;
    }
  }
}
</style>
