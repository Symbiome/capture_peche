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
  <div class="social">
    <b>Réseau social</b>
    <p>Lac {{ lakeId }}</p>
    <ul>
      <li v-for="socialTrip in socialTrips" :key="socialTrip.id">
        {{ formattedDate(socialTrip.date) }}
        {{ socialTrip.userName}} - {{ socialTrip.tripName }} <br />
        {{ socialTrip.catchesCountPerMaillage }} <br />
        {{ socialTrip.socialReactions }} <br />
        <button @click="postSocialReaction(socialTrip.id, 'LIKE')">LIKE</button>
        <button @click="postSocialReaction(socialTrip.id, 'LOVE')">LOVE</button>
      </li>
    </ul>
  </div>
</template>

<script lang="ts">


import { SocialReaction, TripSocial } from "@/pojos/BackendPojos";
import Helpers from "@/services/Helpers";
import TripsService from "@/services/TripsService";
import { Component,  Prop,  Vue, Watch } from "vue-property-decorator";

@Component({
  components: {
  },
})
export default class SocialView extends Vue {
  @Prop() lakeId: string;
  socialTrips: TripSocial[] = [];

 
  mounted() {
    this.loadSocialTrips();   
  }

  @Watch("lakeId")
  async loadSocialTrips() {
    this.socialTrips = await TripsService.listSocialTrips(this.lakeId);
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

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../less/main";
.empty {
  padding-top: 20px;
}
.news {
  cursor: pointer;
  padding-top: 20px;
  .news-holder {
    border-bottom: 1px solid @gainsboro;
    margin-bottom: 40px;
    .news-row {
      display: flex;
      padding-left: @margin-x-large;
      padding-right: @margin-x-large;
      .news-pic {
        width: 20vw;
        height: 20vw;
        max-width: 20vh;
        max-height: 20vh;
        object-fit: cover;
      }
      .right-content {
        width: 100%;
        padding-left: 30px;
        .publication-date {
          padding-top: 10px;
          display: flex;
          gap: 10px;
          color: @pale-sky;
          text-transform: uppercase;
        }
      }
    }
  }
  .news-content {
    padding-top: 10px;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .read-more {
    display: flex;
    justify-content: end;
    gap: 10px;
    width: 100%;
    color: @pelorous;
    font-weight: bold;
    padding: 10px;
    i {
      background-color: @pelorous;
      color: white;
      padding-left: 15px;
      padding-right: 15px;
      border-radius: 10px;
    }
  }

  .only-on-small-screen {
    display: none;
  }
  @media (max-width: 1200px) {
    .only-on-small-screen {
      max-height: 80px;
      display: block;
    }
    .only-on-big-screen {
      display: none;
    }
  }
}
</style>
