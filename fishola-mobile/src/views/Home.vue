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
  <div class="home">
    <div class="home-content">
      <div class="home-header">
        <Title class="hide-on-desktop" />
        <Avatar
          v-if="initials"
          class="home-avatar"
          v-bind:initials="initials"
          v-on:goProfile="goAccount"
        />
      </div>

      <h1 class="home-greeting">Bonjour {{ firstName }} !</h1>

      <h2 class="home-section-title">Ma dernière sortie</h2>
      <LastTripCard
        v-if="lastTrip"
        v-bind:trip="lastTrip"
        v-bind:waterName="lastTripWaterName"
        v-bind:photoUrl="lastTripPhotoUrl"
      />
      <div class="home-empty" v-else-if="tripsLoaded">
        <span>Aucune sortie enregistrée pour le moment.</span>
        <div class="button button-primary">
          <button v-on:click="startNewTrip">Commencer</button>
        </div>
      </div>

      <h2 class="home-section-title">Résumé</h2>
      <div class="home-tiles">
        <SummaryTile icon="img/carnet/stat-trips.svg" :value="tripsCount" label="Sorties" v-on:clicked="goActivitySummary" />
        <SummaryTile icon="img/carnet/stat-hours.svg" :value="hoursCount" label="Heures" v-on:clicked="goActivitySummary" />
        <SummaryTile icon="img/carnet/stat-catches.svg" :value="catchesCount" label="Prises" v-on:clicked="goActivitySummary" />
      </div>

      <h2 class="home-section-title">Progression</h2>
      <div class="home-progress" v-if="badges.length" v-on:click="goBadges">
        <div class="progress-label">
          {{ unlockedBadgesCount }} / {{ badges.length }} badges débloqués
        </div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: unlockedRatio + '%' }"></div>
        </div>
      </div>
      <div class="home-tiles" v-if="nextBadges.length">
        <SummaryTile
          v-for="badge in nextBadges"
          :key="badge.id"
          :emoji="badge.icon || defaultBadgeEmoji"
          :label="badge.name"
          :locked="true"
          v-on:clicked="goBadges"
        />
      </div>
    </div>
    <BottomNav />
  </div>
</template>

<script lang="ts">
import { BadgeBean, TripLight } from "@/pojos/BackendPojos";
import UserProfile from "@/pojos/UserProfile";

import Constants from "@/services/Constants";
import DashboardService, { DashboardAndSpecies } from "@/services/DashboardService";
import GamificationService from "@/services/GamificationService";
import ProfileService from "@/services/ProfileService";
import ReferentialService from "@/services/ReferentialService";
import TripsService, { TripsAndCount } from "@/services/TripsService";
import { RouterUtils } from "@/router/RouterUtils";

import Avatar from "@/components/common/Avatar.vue";
import BottomNav from "@/components/layout/BottomNav.vue";
import Title from "@/components/layout/Title.vue";
import LastTripCard from "@/components/home/LastTripCard.vue";
import SummaryTile from "@/components/home/SummaryTile.vue";

import { Component, Vue } from "vue-facing-decorator";

const UNKNOWN_VALUE = "–";
const NEXT_BADGES_COUNT = 3;
const DEFAULT_BADGE_EMOJI = "🏅";

/** Accueil du pêcheur : dernière sortie, résumé de l'activité, badges. */
@Component({
  components: { Avatar, BottomNav, Title, LastTripCard, SummaryTile },
})
export default class HomeView extends Vue {
  firstName: string = "";
  initials: string = "";
  tripsLoaded: boolean = false;
  lastTrip: TripLight | null = null;
  lastTripWaterName: string = "";
  lastTripPhotoUrl: string = "";
  tripsCount: string | number = UNKNOWN_VALUE;
  hoursCount: string | number = UNKNOWN_VALUE;
  catchesCount: string | number = UNKNOWN_VALUE;
  badges: BadgeBean[] = [];
  defaultBadgeEmoji: string = DEFAULT_BADGE_EMOJI;

  mounted() {
    ProfileService.getProfile().then(this.profileLoaded, () => undefined);
    TripsService.listTrips(true, "").then(this.tripsListed, this.tripsFailed);
    DashboardService.loadDashboardOrTimeout(undefined, "").then(
      this.dashboardLoaded,
      () => undefined
    );
    DashboardService.loadEffortStatisticsOrTimeout(undefined, "").then(
      (stats) => (this.hoursCount = Math.round(stats.totalHours)),
      () => undefined
    );
    GamificationService.getMyBadges().then(
      (badges) => (this.badges = badges),
      () => undefined
    );
  }

  profileLoaded(profile: UserProfile) {
    this.firstName = profile.firstName;
    this.initials = profile.initials;
  }

  tripsListed(data: TripsAndCount) {
    this.tripsLoaded = true;
    this.tripsCount = data.count;
    const sorted = data.trips
      .slice()
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
    this.lastTrip = sorted.length ? sorted[0] : null;
    if (this.lastTrip && this.lastTrip.waterEntityId) {
      ReferentialService.getWaterEntityById(this.lastTrip.waterEntityId).then(
        (water) => (this.lastTripWaterName = water ? water.name : "")
      );
    }
  }

  tripsFailed() {
    this.tripsLoaded = true;
  }

  dashboardLoaded(data: DashboardAndSpecies) {
    const counts = data.dashboard.caughtSpeciesCount || {};
    this.catchesCount = Object.values(counts).reduce((sum, n) => sum + n, 0);
    const pictures = (data.dashboard.picturesPerTrip || []).find(
      (p) => this.lastTrip && p.tripId === this.lastTrip.id
    );
    if (pictures && pictures.pictureURLs.length) {
      this.lastTripPhotoUrl = Constants.apiUrl(pictures.pictureURLs[0]);
    }
  }

  get unlockedBadgesCount(): number {
    return this.badges.filter((b) => b.unlocked).length;
  }

  get unlockedRatio(): number {
    return this.badges.length
      ? Math.round((100 * this.unlockedBadgesCount) / this.badges.length)
      : 0;
  }

  get nextBadges(): BadgeBean[] {
    return this.badges.filter((b) => !b.unlocked).slice(0, NEXT_BADGES_COUNT);
  }

  startNewTrip() {
    RouterUtils.startNewTrip(this.$router);
  }

  goAccount() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/account");
  }

  goBadges() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/badges");
  }

  goActivitySummary() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/dashboard-personal/dashboard");
  }
}
</script>

<style scoped lang="less">
.home {
  position: relative;
  height: 100%;
  background-color: @white-smoke;

  @media screen and (min-width: @desktop-min-width) {
    height: 100vh;
  }
}

.home-content {
  height: calc(100% - @footer-height);
  overflow-y: auto;
  padding: 0 12px @margin-x-large 12px;
  margin-top: env(safe-area-inset-top);

  @media screen and (min-width: @desktop-min-width) {
    height: 100%;
    max-width: 900px;
    padding: @margin-large @margin-large-desktop;
  }
}

.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0 0 2px;

  @media screen and (min-width: @desktop-min-width) {
    justify-content: flex-end;
    padding: 0;
  }

  .home-avatar {
    width: 56px;
    height: 56px;
    font-size: @fontsize-span-big;
  }
}

.home-greeting {
  margin: 23px 0 0 2px;
  color: @gunmetal;
  font-size: 27px;
  font-weight: 700;
  line-height: 1.2;
}

.home-section-title {
  margin: 30px 0 22px 0;
  color: @gunmetal;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.2;
}

.home-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: @margin-medium;
  padding: @margin-medium;
  border-radius: 13.118px;
  background-color: @surface;
  color: @pale-sky;
  text-align: center;
}

.home-tiles {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;

  @media screen and (min-width: @desktop-min-width) {
    grid-template-columns: repeat(3, 160px);
  }
}

.home-progress {
  margin-bottom: 18px;
  cursor: pointer;

  .progress-label {
    margin-bottom: 8px;
    color: @gunmetal;
    font-size: 14.562px;
    font-weight: 500;
  }

  .progress-bar {
    height: 10px;
    border-radius: 5px;
    background-color: @carnet-blue-soft;
    overflow: hidden;
  }

  .progress-fill {
    height: 100%;
    border-radius: 5px;
    background-color: @carnet-blue;
  }
}
</style>
