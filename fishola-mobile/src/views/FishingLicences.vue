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
  <div>
    <label>Mes cartes de pêche</label>
    <FishingLicenceList :licences="licences" @reload="fetchAllLicences" />

    <div class="bottom-page-spacer"></div>
    <div class="create-and-delete">
      <div class="button">
        <button @click="buttonClicked" class="new-button">
          <i class="icon-plus" />
          Nouvelle carte
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import router from "@/router";
import { RouterUtils } from "@/router/RouterUtils";

import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import FishingLicenceList from "@/components/fishing-licences/FishingLicenceList.vue";
import NewFishingLicence from "@/components/fishing-licences/NewFishingLicence.vue";

import FishingLicenceService from "@/services/FishingLicenceService";
import TripsService from "@/services/TripsService";

import { Component, Vue } from "vue-property-decorator";
import { LicenceResponseBean } from "@/pojos/BackendPojos";

@Component({
  components: {
    FisholaHeader,
    RunningOverlay,
    FisholaFooter,
    FishingLicencesView,
    FishingLicenceList,
    NewFishingLicence,
  },
})
export default class FishingLicencesView extends Vue {
  hasRunningTrip: boolean = false;
  licences: LicenceResponseBean[] = [];

  constructor() {
    super();
    this.licences = [];
  }

  created() {
    this.fetchAllLicences();
    TripsService.hasRunningTrip().then(
      (result: boolean) => (this.hasRunningTrip = result)
    );
  }

  fetchAllLicences() {
    FishingLicenceService.getAllLicences().then(this.loadLicences);
  }

  loadLicences(licences: LicenceResponseBean[]) {
    this.licences = [];
    const sortedLicences = Vue.lodash.orderBy(licences, "name");
    sortedLicences.forEach(licence => this.licences.push(licence));
  }


  buttonClicked() {
    RouterUtils.pushRouteNoDuplicate(router, "/licences/new");
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../less/main";

.create-and-delete {
  width: 100%;
  display: flex;
  justify-content: center;

  @media screen and (min-width: 770px) {
    justify-content: end;
  }
}

.new-button {
  border: 1px solid @pelorous !important;
  color: @pelorous;
  background-color: white;

  &:hover {
    border: 1px solid white;
    color: white;
    background-color: @pelorous;
  }
}


.licences-page {
  display: flex;
  flex-direction: column;

  .licences-list {
    flex-grow: 1;
    overflow: auto;
    padding-bottom: 100px;
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
      margin-bottom: @margin-xx-large;
      font-size: @fontsize-title-desktop;
      height: calc(@fontsize-title-desktop + @line-height-padding-xx-large);
      line-height: calc(@fontsize-title-desktop + @line-height-padding-xx-large );
      text-align: left;

      margin-left: @margin-large-desktop;
      margin-right: @margin-large-desktop;
    }

    .create-and-delete {
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

    .bottom {
      position: absolute;
      bottom: 0px;
      width: calc(100% - @desktop-menu-width);
      margin-left: -66px;
    }
  }
}
</style>
