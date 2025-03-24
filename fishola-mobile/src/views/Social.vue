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
    <div id="social-trips-top">
      <select placeholder="lake" v-model="selectedLakeUUID">
        <option v-for="lake in allLakes" :value="lake.id" :key="lake.id">
          {{ lake.name }}
        </option>
      </select>
    </div>
    <div id="social-trips-list">
      <div v-for="socialTrip in socialTrips"
           :key="socialTrip.id"
           class="social-trip-item">
        <div class="social-trip-infos">
          <div class="social-trip-title">{{ socialTrip.tripName }}</div>
          <div><i class="icon-profile" /> {{ socialTrip.userName}} &emsp; <i class="icon-calendar" /> {{ formattedDate(socialTrip.date) }} &emsp; <i class="icon-lake" /> {{ socialTrip.lakeName }} &emsp; <i class="icon-clock" /> {{ formattedDuration(socialTrip.durationInSeconds) }}</div>
          <div><i class="icon-fish" /> {{ socialTrip.catchesCountPerMaillage }}</div>
        </div>
        {{ socialTrip.socialReactions.length }}
        <div class="social-trip-reaction">
          Super sortie
          <div class="button">
            <button @click="postSocialReaction(socialTrip.id, 'LIKE')" class="new-button">0 <i class="icon-like" /></button>
          </div>
        </div>
        <div class="social-trip-reaction">
          Bravo pour cette sortie
          <div class="button">
            <button @click="postSocialReaction(socialTrip.id, 'LOVE')" class="new-button">0 <i class="icon-heart" /></button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">


import { Lake, SocialReaction, TripSocial } from "@/pojos/BackendPojos";
import Helpers from "@/services/Helpers";
import ReferentialService from "@/services/ReferentialService";
import TripsService from "@/services/TripsService";
import { Component,  Prop,  Vue, Watch } from "vue-property-decorator";

@Component({
  components: {
  },
})
export default class SocialView extends Vue {
  @Prop() lakeId: string;
  socialTrips: TripSocial[] = [];
  selectedLakeUUID = "";
  allLakes: Lake[] = [];

 
  mounted() {
    this.loadLakes();
    this.loadSocialTrips();
  }

  async loadLakes() {
    this.allLakes = await ReferentialService.getLakes();
    this.selectedLakeUUID = this.allLakes[0].id;
  }

  @Watch("selectedLakeUUID")
  async loadSocialTrips() {
    this.socialTrips = await TripsService.listSocialTrips(this.selectedLakeUUID);
  }

  async postSocialReaction(tripId: string, socialReaction: SocialReaction) {
    await TripsService.postSocialReaction(tripId, socialReaction);
    this.loadSocialTrips();
  }

  formattedDate(rawDate: Date): string {
    var dayOptions: Intl.DateTimeFormatOptions = {
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

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../less/main";

#social-trips-list {
  overflow-y: scroll;
  height: calc(100vh - 40px - env(safe-area-inset-top) - 20px - 22px - 8px );
  padding-bottom: 200px;
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
    align-items: center;
    gap: @margin-x-large;
    padding-right: @margin-large;
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
.social-trip-reaction {
  display: flex;
  align-items: center;
  font-size: 14px;

  .button {
    width: auto;
    button:not(:hover) {
      background-color: transparent;
    }
  }
}
</style>
