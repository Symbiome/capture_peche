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
  <div class="licences page-with-header-and-footer shifted-background">
    <FisholaHeader />

    <div class="page documentation-page">
      <div class="pane pane-only">
        <div class="pane-content rounded">
          <h1 class="no-margin-pane">Cartes de pêche</h1>
          <div class="bottom-page-spacer"></div>
        </div>
      </div>
      <RunningOverlay class="hiddenWhenKeyboardShows" v-if="hasRunningTrip" />
    </div>
    <FisholaFooter shortcuts="logout,dashboard,home" selected="dashboard" />
  </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import RunningOverlay from "@/components/layout/RunningOverlay.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import { LicenceResponseBean, LicenceFromClientBean } from "@/pojos/BackendPojos";

import { Component, Prop, Vue } from "vue-property-decorator";

@Component({
  components: {
    FisholaHeader,
    RunningOverlay,
    FisholaFooter,
  },
})
export default class LicencesView extends Vue {
  hasRunningTrip: boolean = false;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../less/main";

.licences-page {
  .pane .pane-content {
    padding-left: 0px;
    padding-right: 0px;
  }

  .licences-row {
    padding-left: @margin-x-large;
    padding-right: @margin-x-large;
    height: 56px;

    @media (max-height: 579px) {
      height: 46px;
    }

    @media (max-width: 370px) {
      padding-left: @margin-large;
      padding-right: @margin-large;
    }

    border-bottom: 1px solid @solitude;

    display: flex;
    justify-content: space-between;
    align-items: center;

    span {
      font-size: @fontsize-small-paragraph;
      line-height: calc(
        @fontsize-small-paragraph + @line-height-padding-medium
      );
      color: @gunmetal;
    }

    a {
      color: @pelorous;
      font-size: @fontsize-paragraph;
    }

    @media screen and (min-width: @desktop-min-width) {
      span {
        font-size: @fontsize-paragraph;
        line-height: calc(@fontsize-paragraph + @line-height-padding-medium);
      }
      a {
        font-size: @fontsize-paragraph-desktop;
      }
    }

    @media screen and (min-width: 800px) {
      padding-left: @margin-large-desktop;
      padding-right: @margin-large-desktop;
    }
  }

  .faq-rows {
    padding-left: @margin-x-large;
    padding-right: @margin-x-large;

    @media (max-width: 370px) {
      padding-left: @margin-large;
      padding-right: @margin-large;
    }

    display: flex;
    flex-direction: column;
    /deep/ .faq {
      margin-top: 10px;
      margin-bottom: 10px;

      h4 {
        margin-top: 0px;
        margin-bottom: 0px;
        font-size: 18px;
        font-weight: normal;
      }

      p {
        margin-top: 5px;
        margin-bottom: 5px;
        font-size: 16px;
        color: @pale-sky;
      }
    }
  }
}

.docs-and-faq-tab {
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: space-evenly;
  margin-top: -30px;
  padding-bottom: 20px;

  .docs-or-faq {
    width: 40%;
    display: flex;
    justify-content: center;
    text-align: center;
    gap: 5px;
    color: @pale-sky;
    padding-bottom: 5px;
    padding-left: 20px;
    padding-right: 20px;
    cursor: pointer;

    &.selected {
      color: @gunmetal;
      border-bottom: 2px solid @pelorous;
    }
  }
}

@media screen and (max-width: 760px) {
  .docs-and-faq-tab {
    padding-top: 20px;
    margin-top: 0px;
  }
}
</style>
