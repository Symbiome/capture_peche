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
  <div class="badge-unlock" v-bind:class="visibility" v-if="badge">
    <div class="badge-unlock-box" v-on:click="shareBadge">
      <div class="badge-unlock-icon">{{ badge.icon }}</div>
      <div class="badge-unlock-text">
        <div class="badge-unlock-title">Nouveau badge débloqué !</div>
        <div class="badge-unlock-name">{{ badge.name }}</div>
      </div>
      <i class="icon-share" />
    </div>
  </div>
</template>

<script lang="ts">
import GamificationService from "@/services/GamificationService";
import { BadgeBean } from "@/pojos/BackendPojos";

import { Component, Vue } from "vue-property-decorator";

const KNOWN_UNLOCKED_BADGES_KEY = "fishola.gamification.knownUnlockedBadgeIds";
const DISPLAY_DURATION_MS = 4000;

@Component
export default class BadgeUnlockNotification extends Vue {
  visibility: string = "badge-unlock-hidden";
  badge: BadgeBean | null = null;
  private queue: BadgeBean[] = [];

  mounted() {
    this.$root.$on("trips-saved", this.checkForNewBadges);
  }

  beforeDestroy() {
    this.$root.$off("trips-saved", this.checkForNewBadges);
  }

  checkForNewBadges() {
    GamificationService.getMyBadges().then((badges: BadgeBean[]) => {
      const unlocked = badges.filter((b) => b.unlocked);
      const previouslyKnownIds = this.readKnownUnlockedBadgeIds();
      this.writeKnownUnlockedBadgeIds(unlocked.map((b) => this.badgeKey(b)));

      // Premier chargement (aucune trace locale) : on initialise silencieusement,
      // sans notifier pour tous les badges déjà débloqués auparavant.
      if (previouslyKnownIds === null) {
        return;
      }
      const newlyUnlocked = unlocked.filter(
        (b) => previouslyKnownIds.indexOf(this.badgeKey(b)) === -1
      );
      newlyUnlocked.forEach((b) => this.queue.push(b));
      if (newlyUnlocked.length > 0 && !this.badge) {
        this.showNext();
      }
    });
  }

  // #90 : le badge CONCOURS peut être débloqué plusieurs fois (même id, un par concours) --
  // suivre uniquement badge.id masquerait la notification d'un 2e concours déjà "connu"
  // via le 1er. Même composition de clé que Badges.vue.
  private badgeKey(badge: BadgeBean): string {
    return badge.id + (badge.competitionId ? "-" + badge.competitionId : "");
  }

  private showNext() {
    this.badge = this.queue.shift() || null;
    if (!this.badge) {
      return;
    }
    this.visibility = "badge-unlock-visible";
    setTimeout(this.hideCurrent, DISPLAY_DURATION_MS);
  }

  private hideCurrent() {
    this.visibility = "badge-unlock-hidden";
    setTimeout(() => {
      this.badge = null;
      this.showNext();
    }, 500);
  }

  shareBadge() {
    if (this.badge) {
      this.$root.$emit("open-badge-share", this.badge);
    }
    this.hideCurrent();
  }

  private readKnownUnlockedBadgeIds(): string[] | null {
    const raw = localStorage.getItem(KNOWN_UNLOCKED_BADGES_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  private writeKnownUnlockedBadgeIds(ids: string[]) {
    localStorage.setItem(KNOWN_UNLOCKED_BADGES_KEY, JSON.stringify(ids));
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.badge-unlock {
  position: fixed;
  bottom: calc(@footer-height + @vertical-margin-small);
  left: 0;
  width: 100%;
  z-index: 997;
  display: flex;
  justify-content: center;
  pointer-events: none;
}

.badge-unlock-hidden {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.5s, transform 0.5s;
}

.badge-unlock-visible {
  opacity: 1;
  transform: translateY(0);
  transition: opacity 0.5s, transform 0.5s;
}

.badge-unlock-box {
  pointer-events: auto;
  display: flex;
  align-items: center;
  background: linear-gradient(160deg, @pelorous, @cyprus);
  color: @white;
  border-radius: 16px;
  padding: @vertical-margin-small @vertical-margin-medium;
  box-shadow: 0 4px 12px @black-alpha-50;
  cursor: pointer;

  .badge-unlock-icon {
    font-size: 2rem;
    margin-right: @vertical-margin-small;
  }

  .badge-unlock-title {
    font-size: @fontsize-small-paragraph;
    opacity: 0.9;
  }

  .badge-unlock-name {
    font-weight: bold;
  }

  i {
    margin-left: @vertical-margin-small;
  }
}
</style>
