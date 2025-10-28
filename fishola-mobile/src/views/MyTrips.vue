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
  <div class="my-trips-wrapper">
    <div class="my-trips-top">
      <MyTripsHeader
        v-bind:count="count"
        v-bind:offline="offline"
        v-bind:sortDown="sortDown"
        v-on:reverseSortOrder="reverseSortOrder"
      >
        <MyTripsSearch
          class="hide-on-desktop"
          v-model="term"
          v-bind:offline="offline"
        />
      </MyTripsHeader>
      <div class="search-and-new hide-on-mobile">
        <div
          v-if="!hasRunningTrip"
          class="button button-primary"
          :class="{ delete: selectedTripIds.length > 0 }"
        >
          <button v-on:click="footerButtonClicked" class="new-button">
            <i v-bind:class="getButtonIcon()" />
            {{ getButtonText() }}
          </button>
        </div>
        <MyTripsSearch v-model="term" v-bind:offline="offline" />
      </div>
    </div>
    <MyTripsList
      v-bind:trips="trips"
      v-bind:hasSearchTerm="!!currentSearchTerm"
      v-bind:noTripYet="totalCount == 0"
      v-bind:offline="offline"
      v-bind:loading="loading"
      v-on:more-trips="loadNextPage"
      v-on:trip-selected="tripSelected"
      v-on:trip-unselected="tripUnselected"
    />
     <FisholaFooter 
              shortcuts="logout,dashboard,home" 
              selected="dashboard"
              :buttonText="getButtonText()"
              :buttonIcon="getButtonIcon()"
              @buttonClicked="footerButtonClicked"
         />
  </div>
</template>

<script lang="ts">
import { RouterUtils } from "@/router/RouterUtils";

import { TripLight } from "@/pojos/BackendPojos";

import TripsService from "@/services/TripsService";
import { TripsAndCount } from "@/services/TripsService";
import Helpers from "@/services/Helpers";

import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import MyTripsHeader from "@/components/my-trips/MyTripsHeader.vue";
import MyTripsSearch from "@/components/my-trips/MyTripsSearch.vue";
import MyTripsList from "@/components/my-trips/MyTripsList.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import { Component, Watch, Vue, Prop } from "vue-property-decorator";

@Component({
  components: {
    FisholaHeader,
    MyTripsHeader,
    MyTripsSearch,
    MyTripsList,
    FisholaFooter,
  },
})
export default class MyTripsView extends Vue {
  @Prop()
  hasRunningTrip: boolean;
  
  trips: TripLight[] = [];
  loading: boolean = true;
  offline: boolean = false;
  sortDown: boolean = true;
  term: string = "";
  currentSearchTerm: string = "";
  currentPage: number = 0;
  count: number = 0;
  totalCount: number = -1;

  refreshTimer: any = undefined;

  selectedTripIds: string[] = [];

  @Watch("term")
  onTermChanged(_value: string, _oldValue: string) {
    if (this.refreshTimer) {
      this.refreshTimer.cancel();
    }
    this.refreshTimer = Vue.lodash.debounce(this.loadTrips, 1000);
    this.refreshTimer();
  }

  constructor() {
    super();
    this.trips = [];
  }

  loadTrips() {
    this.currentPage = 0;
    this.currentSearchTerm = this.term;
    this.selectedTripIds = [];
    TripsService.listTrips(this.sortDown, this.term).then(
      this.tripsLoaded,
      this.loadError
    );
    this.loading = true;
  }

  loadNextPage() {
    if (this.trips.length < this.count) {
      TripsService.listTrips(
        this.sortDown,
        this.currentSearchTerm,
        this.currentPage + 1
      ).then(this.moreTripsLoaded, this.loadError);
    }
  }

  created() {
    this.loadTrips();
  }

  mounted() {
    this.$root.$on("trips-saved", () => {
      console.debug(
        "La liste des sorties a été mise à jour, on rafraichit ..."
      );
      this.loadTrips();
    });
  }

  beforeDestroy() {
    // On fait en sortie de ne plus écouter les évènements si le composant n'est plus actif
    this.$root.$off("trips-saved");
  }

  tripsLoaded(data: TripsAndCount) {
    this.trips.slice();
    this.trips = data.trips;
    this.loading = false;
    this.count = data.count;
    this.offline = data.offlineMarker;

    // On considère que le premier appel renvoie toujours le total
    if (this.totalCount == -1) {
      this.totalCount = data.count;
    }
  }

  moreTripsLoaded(data: TripsAndCount) {
    this.currentPage++;
    data.trips.forEach((t) => this.trips.push(t));
  }

  loadError(data: any) {
    console.error("Erreur au chargement des sorties", data);
    if (data && data.status == 401) {
      this.$root.$emit("toaster-warning", "Vous n'êtes plus connecté\u00B7e");
      RouterUtils.pushRouteNoDuplicate(this.$router, "/login");
    }
  }

  reverseSortOrder() {
    this.sortDown = !this.sortDown;
    this.loadTrips();
  }

  getButtonIcon() {
    return this.selectedTripIds.length == 0
      ? this.totalCount == 0
        ? "icon-fishing"
        : "icon-plus"
      : "icon-delete";
  }

  getButtonText() {
    return this.selectedTripIds.length == 0
      ? this.totalCount == 0
        ? "Commencer"
        : "Nouvelle sortie"
      : "Supprimer";
  }

  footerButtonClicked() {
    if (this.selectedTripIds.length == 0) {
      this.newTrip();
    } else {
      let message = "Voulez-vous supprimer cette sortie ?";
      if (this.selectedTripIds.length > 1) {
        message = "Voulez-vous supprimer ces sorties ?";
      }
      Helpers.confirm(this.$modal, message).then(this.deleteSelectedTrips);
    }
  }

  newTrip() {
    Helpers.getDeviceType().then((source) => {
      if (source == "web") {
        TripsService.newAfterwardsTrip().then((id: string) => {
          RouterUtils.pushRouteNoDuplicate(this.$router, {
            name: "trip-meta",
            params: { id: id },
          });
        });
      } else {
        RouterUtils.pushRouteNoDuplicate(this.$router, "/trips/new");
      }
    });
  }

  tripSelected(tripId: string) {
    this.selectedTripIds.push(tripId);
  }

  tripUnselected(tripId: string) {
    const index = this.selectedTripIds.indexOf(tripId);
    if (index != -1) {
      this.selectedTripIds.splice(index, 1);
    }
  }

  deleteSelectedTrips() {
    TripsService.deleteTrips(this.selectedTripIds).then(this.tripsDeleted);
  }

  tripsDeleted() {
    const plural = this.selectedTripIds.length > 1 ? "s" : "";
    const message = `${this.selectedTripIds.length} sortie${plural} supprimée${plural}`;
    this.$root.$emit("toaster-success", message);
    this.loadTrips();
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.my-trips-page {
  display: flex;
  flex-direction: column;

  .my-trips-list {
    flex-grow: 1;
    overflow: hidden;
    height: 100%;;
    border-radius: 0;
  }

  .my-trips-wrapper {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  .bottom {
    position: absolute;
    bottom: 0px;
    width: 100%;
    margin-left: -30px;
  }

  @media screen and (max-width: 1150px) and (min-width: 770px) {
    .new-button {
      font-size: 14px;
    }
  }
  @media screen and (min-width: @desktop-min-width) {
    background-color: @white-smoke;

    h1 {
      font-style: normal;
      font-weight: normal;

      color: @pelorous;
      margin-top: @margin-medium;
      font-size: @fontsize-title-desktop;
      height: calc(@fontsize-title-desktop + @line-height-padding-xx-large);
      line-height: calc(
        @fontsize-title-desktop + @line-height-padding-xx-large
      );
      text-align: left;

      margin-left: @margin-large-desktop;
      margin-right: @margin-large-desktop;
    }

    .my-trips-top {
      color: @pelorous;
      display: flex;
      flex-direction: row-reverse;
      justify-content: space-between;
      align-items: center;
      margin-right: @margin-large-desktop;

      .search-and-new {
        display: flex;
        flex-direction: row;
        align-items: center;
        .button {
          margin-left: 0px;
          margin-right: 0px;

          &.delete {
            button {
              background-color: @cardinal;
            }
          }
        }
      }
    }
    .bottom {
      position: absolute;
      bottom: 0px;
      width: calc(100% - @desktop-menu-width);
      margin-left: 0;
    }
  }
}
</style>
