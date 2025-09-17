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
  <div class="my-trips-item">
    <div class="item-selection">
      <input
        type="checkbox"
        v-bind:id="'checkbox-' + trip.id"
        class="pelorous-checkbox"
        v-model="selected"
      />
      <label v-bind:for="'checkbox-' + trip.id"></label>
    </div>
    <div class="item-description" v-on:click="openTrip">
      <div class="item-row">
        <div class="name">{{ trip.name }}</div>
        <div class="right-part">
          <i
            v-if="trip.modifiable"
            class="icon-edit warning"
            title="La sortie est encore modifiable"
          />
        </div>
      </div>
      <div class="item-row">
        <div class="left-part"><i class="icon-calendar" />{{ date }}</div>
        <div class="right-part">{{ duration }}<i class="icon-clock" /></div>
      </div>
      <div class="item-row">
        <div class="left-part"><i class="icon-lake" />{{ lakeName }}</div>
        <div class="right-part">
          {{ trip.catchsCount }}<i class="icon-fish" />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { TripLight, Lake } from "@/pojos/BackendPojos";

import ReferentialService from "@/services/ReferentialService";
import Helpers from "@/services/Helpers";

import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import router from "../../router";
import { RouterUtils } from "@/router/RouterUtils";

@Component
export default class MyTripItem extends Vue {
  @Prop() trip!: TripLight;

  selected: boolean = false;
  date: string = "";
  lakeName: string = "";
  duration: string = "";

  created() {
    ReferentialService.getLakesIndex().then(this.lakesIndexLoaded);

    const dayOptions: Intl.DateTimeFormatOptions = {
      weekday: "long",
      month: "long",
      day: "numeric",
      year: "numeric",
    };
    this.date = this.trip.date.toLocaleDateString("fr-FR", dayOptions);

    this.duration = Helpers.formatSecondsDuration(this.trip.durationInSeconds);
  }

  lakesIndexLoaded(lakes: Map<string, Lake>) {
    this.lakeName = lakes.get(this.trip.lakeId)?.name ?? "";
  }

  openTrip() {
    RouterUtils.pushRouteNoDuplicate(this.$router, {
      name: "trip",
      params: { id: this.trip.id },
    });
  }

  @Watch("selected")
  onSelectedChanged(value: boolean, _oldValue: boolean) {
    if (value) {
      this.$emit("selected");
    } else {
      this.$emit("unselected");
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.my-trips-item {
  display: flex;
  align-items: center;

  margin: 0px;
  padding-right: @margin-large;
  padding-top: @vertical-margin-small;
  padding-bottom: @vertical-margin-small;

  border-bottom: 1px solid @gainsboro;

  width: 100%;
  min-height: 110px;

  .item-selection {
    width: 16px;
    height: 16px;

    input {
      margin: 0px;
    }
  }

  .item-description {
    margin-left: @margin-medium;
    width: 100%;

    font-size: @fontsize-small-paragraph;
    line-height: calc(@fontsize-small-paragraph + @line-height-padding-x-large);

    .item-row {
      display: flex;
      justify-content: space-between;
      margin-top: @vertical-margin-xx-small;

      color: @pale-sky;

      .name {
        font-weight: bold;
        font-size: @fontsize-header-paragraph;
        color: @gunmetal;

        // Truncate text over 2 lines
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .left-part {
        i {
          margin-right: @margin-small;
          color: @pale-sky;
        }
      }

      .right-part {
        i {
          margin-left: @margin-small;
          color: @pelorous;
        }

        .warning {
          color: @terra-cotta;
        }
      }
    }
  }

  @media (max-width: 350px) {
    padding-right: @margin-medium;

    .item-description {
      margin-left: @margin-small;

      .item-row {
        .left-part {
          i {
            margin-right: @margin-x-small;
          }
        }

        .right-part {
          i {
            margin-left: @margin-x-small;
          }
        }
      }
    }
  }

  @media (max-height: 650px) {
    min-height: 90px;

    .item-description {
      line-height: calc(@fontsize-small-paragraph + @line-height-padding-small);
    }
  }

  @media screen and (min-width: @desktop-min-width) {
    height: 110px;
    padding-right: @margin-large-desktop;
    &:hover {
      background-color: @gainsboro;
    }

    .item-description {
      cursor: pointer;
      margin-left: @margin-medium;
      width: 100%;

      font-size: @fontsize-paragraph;
      line-height: calc(@fontsize-paragraph + @line-height-padding-x-large);

      .item-row {
        .name {
          font-size: @fontsize-paragraph-desktop;
        }
      }
    }
  }
}
</style>
