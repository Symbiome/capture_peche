<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
  <div id="social-trips">
    <div id="social-trips-list">
      <div v-if="!socialTrips || socialTrips.length == 0" class="empty">
        Pas de sorties sur ce plan d'eau dans la communauté.
      </div>

      <div v-for="socialTrip in socialTrips" :key="socialTrip.id" class="social-trip-item">
        <div class="social-trip-infos">
          <div class="social-trip-title">{{ socialTrip.tripName }}</div>
          <div class="social-trip-metadata">
            <span><i class="icon-profile" /> {{ socialTrip.userName }} &emsp;</span>
            <span><i class="icon-calendar" /> {{ formattedDate(socialTrip.date) }} &emsp;</span>
            <span><i class="icon-lake" /> {{ socialTrip.lakeName }} &emsp;</span>
            <span><i class="icon-clock" /> {{ formattedDuration(socialTrip.durationInSeconds) }}</span>
          </div>
          <div class="social-trip-catches">
            <i class="icon-fish" />
            {{ countCatches(socialTrip.catchesCountPerMaillage) }} prises :
            <span v-for="specie in Object.keys(socialTrip.catchesCountPerMaillage)" :key="specie" class="catch-specie">
              <span v-if="socialTrip.catchesCountPerMaillage[specie].MAILLEE">
                {{ socialTrip.catchesCountPerMaillage[specie].MAILLEE }} {{ specie }}<span
                  v-if="socialTrip.catchesCountPerMaillage[specie].MAILLEE">s</span>
                maillé<span v-if="socialTrip.catchesCountPerMaillage[specie].MAILLEE">s</span>
              </span>
              <span v-if="socialTrip.catchesCountPerMaillage[specie].NON_MAILLEE">
                {{ socialTrip.catchesCountPerMaillage[specie].NON_MAILLEE }} {{ specie }}<span
                  v-if="socialTrip.catchesCountPerMaillage[specie].NON_MAILLEE">s</span>
                non maillé<span v-if="socialTrip.catchesCountPerMaillage[specie].NON_MAILLEE">s</span>
              </span>
              <span v-if="socialTrip.catchesCountPerMaillage[specie].NON_DEFINI">
                {{ socialTrip.catchesCountPerMaillage[specie].NON_DEFINI }} {{ specie }}<span
                  v-if="socialTrip.catchesCountPerMaillage[specie].NON_DEFINI">s</span>
              </span>
            </span>
          </div>
        </div>
        <div class="social-trip-reactions">
          <div class="social-trip-reaction">
            <span class="social-trip-reaction-text">Super sortie</span>
            <div class="button reaction-button" :class="{ 'is-active': hasReaction(socialTrip.id, 'LIKE') }">
              <button
                @click="hasReaction(socialTrip.id, 'LIKE') ? deleteSocialReaction(socialTrip.id, 'LIKE') : postSocialReaction(socialTrip.id, 'LIKE')"
                class="new-button">
                {{ countSocialReaction(socialTrip.id, 'LIKE') }} <i class="icon-like" />
              </button>
            </div>
          </div>
          <div class="social-trip-reaction">
            <span class="social-trip-reaction-text">Bravo pour cette sortie</span>
            <div class="button reaction-button" :class="{ 'is-active': hasReaction(socialTrip.id, 'LOVE') }">
              <button
                @click="hasReaction(socialTrip.id, 'LOVE') ? deleteSocialReaction(socialTrip.id, 'LOVE') : postSocialReaction(socialTrip.id, 'LOVE')"
                class="new-button">
                {{ countSocialReaction(socialTrip.id, 'LOVE') }} <i class="icon-heart" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">


import { Lake, Maillage, SocialReaction, TripSocial } from "@/pojos/BackendPojos";
import Helpers from "@/services/Helpers";
import TripsService from "@/services/TripsService";
import ProfileService from "@/services/ProfileService";
import { Component, Prop, Vue, Watch } from "vue-property-decorator";

@Component({
  components: {
  },
})
export default class SocialView extends Vue {
  socialTrips: TripSocial[] = [];
  @Prop()
  lakeId: string;
  allLakes: Lake[] = [];
  userId = "";


  mounted() {
    this.loadUserId();
    this.loadSocialTrips();
  }

  async loadUserId() {
    ProfileService.getProfile().then(
      (profile) => {
        this.userId = profile.id;
      }
    );
  }

  @Watch("lakeId")
  async loadSocialTrips() {
    this.socialTrips = await TripsService.listSocialTrips(this.lakeId);
  }

  async postSocialReaction(tripId: string, socialReaction: SocialReaction) {
    await TripsService.postSocialReaction(tripId, socialReaction);
    this.loadSocialTrips();
  }

  async deleteSocialReaction(tripId: string, socialReaction: SocialReaction) {
    await TripsService.deleteSocialReaction(tripId, socialReaction);
    this.loadSocialTrips();
  }

  countSocialReaction(tripId: string, socialReaction: SocialReaction) {
    let relatedSocialTrips = this.socialTrips.find(({ id }) => id === tripId);
    if (relatedSocialTrips) {
      return relatedSocialTrips.socialReactions.reduce(
        (sum, trip) => {
          return sum + (trip['reaction'] === socialReaction ? 1 : 0);
        }
        ,
        0,
      );
    }
  }
  hasReaction(tripId: string, socialReaction: SocialReaction) {
    let relatedSocialTrips = this.socialTrips.find(({ id }) => id === tripId);
    if (relatedSocialTrips) {
      return undefined != relatedSocialTrips.socialReactions.find(item =>
        item.userId == this.userId && item.reaction == socialReaction
      );
    }
  }

  formattedDate(rawDate: Date): string {
    const dayOptions: Intl.DateTimeFormatOptions = {
      month: "numeric",
      day: "numeric",
      year: "numeric",
    };
    // @ts-ignore
    const date = Helpers.parseLocalDate(rawDate);
    const dateString = date.toLocaleDateString("fr-FR", dayOptions);
    return dateString;
  }

  formattedDuration(seconds: number): string {
    return Helpers.formatSecondsDuration(seconds);
  }

  countCatches(catches: Map<String, Map<Maillage, number>>) {
    let sum: number = 0;
    Object.values(catches).map((item) => {
      if (item.MAILLEE) {
        sum += item.MAILLEE;
      }
      if (item.NON_MAILLEE) {
        sum += item.NON_MAILLEE;
      }
      if (item.NON_DEFINI) {
        sum += item.NON_DEFINI;
      }
    });

    return sum;
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
#social-trips-top {
  text-align: center;
}

#social-trips-list {
  overflow-y: auto;
  height: calc(100vh - 40px - env(safe-area-inset-top) - 20px - 22px - 8px);
  padding-bottom: 250px;
}

.social-trip-item {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: center;
  gap: @margin-small;
  padding-top: @vertical-margin-medium;
  padding-bottom: @vertical-margin-medium;
  border-bottom: 1px solid @gainsboro;

  @media (min-width: 768px) {
    gap: @margin-x-large;
    padding-right: @margin-large;
  }
  @media (max-width: 1270px) {
    flex-direction: column;
  }
}

.social-trip-infos {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  color: @pale-sky;
  font-size: 14px;
  gap: 7px;
  flex-basis: 100%;

  @media (min-width: 768px) {
    flex-basis: 0;
  }
}

.social-trip-title {
  font-weight: bold;
  font-size: 18px;
  color: initial;
}

.social-trip-metadata {
  display: flex;
  flex-wrap: wrap;

  &>span {
    white-space: nowrap;
  }

}

.social-trip-reactions {
  display: flex;
  gap: 7px;

  @media (max-width: @desktop-min-width) {
    justify-content: center;
  }
}

.social-trip-reaction {
  display: flex;
  align-items: center;
  font-size: 14px;

  .social-trip-reaction-text {
    color: @pale-sky;
  }

  @media (max-width: 768px) {
    flex-direction: column;
  }
  .reaction-button {
    width: auto;

    button {
      background-color: transparent;
      &:hover {
        background-color: @cyprus;
      }
    }

    &.is-active {
      button:not(:hover) {
        background-color: @pelorous !important;
        color: white;
      }

    }
  }
}
.catch-specie {
  text-transform: lowercase;
  & > span {
    position: relative;
    padding-left: @margin-x-small;
    margin-left: @margin-x-small;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      height: 70%;
      width: 1px;
      background: #636E72;
      top: 20%;
      opacity: 0.6;
    }

  }
  &:first-of-type > span:first-child {
    padding-left: 0;
    margin-left: 0;

    &::before {
      content: none;
    }
  }
}
</style>
