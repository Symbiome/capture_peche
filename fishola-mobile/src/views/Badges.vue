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
  <div class="badges page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="page badges-page">
      <div class="pane pane-only">
        <div id="scrollable" class="pane-content large rounded">
          <h1 class="no-margin-pane">Mes badges</h1>

          <div class="spinner-wrapper" v-if="!ready">
            <div class="spinner"></div>
          </div>

          <div class="badges-summary" v-if="ready">
            {{ unlockedCount }} / {{ badges.length }} badges débloqués
          </div>

          <div
            class="badges-category"
            v-for="category in categories"
            :key="category"
          >
            <h2>{{ category }}</h2>
            <div class="badges-grid">
              <div
                class="badge-tile"
                v-for="badge in badgesByCategory[category]"
                :key="badge.id"
                v-bind:class="badge.unlocked ? 'unlocked' : 'locked'"
                v-on:click="badgeClicked(badge)"
              >
                <div class="badge-icon">{{ badge.icon }}</div>
                <div class="badge-name">{{ badge.name }}</div>
                <div class="badge-description">{{ badge.description }}</div>
                <div class="badge-unlocked-at" v-if="badge.unlocked">
                  Débloqué le {{ formatDate(badge.unlockedAt) }}
                </div>
              </div>
            </div>
          </div>

          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter shortcuts="logout,dashboard,home" selected="badges" />
  </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import GamificationService from "@/services/GamificationService";
import { BadgeBean } from "@/pojos/BackendPojos";
import moment from "moment";

import { Component, Vue } from "vue-property-decorator";

@Component({
  components: {
    FisholaHeader,
    FisholaFooter,
  },
})
export default class BadgesView extends Vue {
  ready: boolean = false;
  badges: BadgeBean[] = [];

  mounted() {
    GamificationService.getMyBadges().then((badges: BadgeBean[]) => {
      this.badges = badges;
      this.ready = true;
    });
  }

  get unlockedCount(): number {
    return this.badges.filter((b) => b.unlocked).length;
  }

  get categories(): string[] {
    const seen: string[] = [];
    this.badges.forEach((b) => {
      if (seen.indexOf(b.category) === -1) {
        seen.push(b.category);
      }
    });
    return seen;
  }

  get badgesByCategory(): { [category: string]: BadgeBean[] } {
    const result: { [category: string]: BadgeBean[] } = {};
    this.badges.forEach((b) => {
      if (!result[b.category]) {
        result[b.category] = [];
      }
      result[b.category].push(b);
    });
    return result;
  }

  formatDate(d: Date): string {
    return moment(d).format("DD/MM/YYYY");
  }

  badgeClicked(badge: BadgeBean) {
    if (badge.unlocked) {
      this.$root.$emit("open-badge-share", badge);
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.badges-summary {
  color: @pale-sky;
  margin-bottom: @vertical-margin-medium;
}

.badges-category {
  margin-bottom: @vertical-margin-medium;

  h2 {
    font-size: @fontsize-header-title-small;
    color: @cyprus;
    margin-bottom: @vertical-margin-x-small;
  }
}

.badges-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: @vertical-margin-small;
}

.badge-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: @vertical-margin-small;
  border-radius: 12px;
  background: @white-smoke;
  cursor: pointer;

  .badge-icon {
    font-size: 2.5rem;
    line-height: 1;
    margin-bottom: @vertical-margin-x-small;
  }

  .badge-name {
    font-weight: bold;
    color: @gunmetal;
  }

  .badge-description {
    font-size: @fontsize-small-paragraph;
    color: @pale-sky;
    margin-top: @vertical-margin-xx-small;
  }

  .badge-unlocked-at {
    font-size: @fontsize-small-paragraph;
    color: @pelorous;
    margin-top: @vertical-margin-x-small;
  }

  &.locked {
    .badge-icon,
    .badge-name {
      filter: grayscale(100%);
      opacity: 0.4;
    }

    .badge-description {
      opacity: 0.6;
    }
  }

  &.unlocked {
    background: @zircon;
  }
}
</style>
