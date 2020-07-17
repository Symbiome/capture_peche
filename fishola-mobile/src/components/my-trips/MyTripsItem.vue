<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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
      <input type="checkbox"
             v-bind:id="'checkbox-' + trip.id"
             class="pelorous-checkbox"
             v-model="selected"/>
      <label v-bind:for="'checkbox-' + trip.id"></label>
    </div>
    <div class="item-description" v-on:click="openTrip">
      <div class="item-row">
        <div class="name">{{trip.name}}</div>
        <div class="right-part">
          <i v-if="trip.modifiable" class="icon-edit warning"/>
        </div>
      </div>
      <div class="item-row">
        <div class="left-part">
          <i class="icon-calendar"/>{{date}}
        </div>
        <div class="right-part">
          {{duration}}<i class="icon-clock"/>
        </div>
      </div>
      <div class="item-row">
        <div class="left-part">
          <i class="icon-lake"/>{{lakeName}}
        </div>
        <div class="right-part">
          {{trip.catchsCount}}<i class="icon-fish"/>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import {TripLight, Lake} from '@/pojos/BackendPojos';

import ReferentialService from '@/services/ReferentialService';
import Helpers from '@/services/Helpers';

import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import router from '../../router';

@Component
export default class MyTripItem extends Vue {
  @Prop() trip!: TripLight;

  selected:boolean = false;
  date:string = '';
  lakeName:string = '';
  duration:string = '';

  created() {
    ReferentialService.getLakesIndex().then(this.lakesIndexLoaded);

    var dayOptions = {weekday: "long", month: "long", day: "numeric", year: "numeric"};
    this.date = this.trip.date.toLocaleDateString('fr-FR', dayOptions);

    this.duration = Helpers.formatSecondsDuration(this.trip.durationInSeconds);
  }

  lakesIndexLoaded(lakes:Map<string, Lake>) {
    this.lakeName = lakes.get(this.trip.lakeId)!.name;
  }

  openTrip() {
    router.push({name:'trip', params: {id: this.trip.id}});
  }

  @Watch('selected')
  onSelectedChanged(value: boolean, oldValue: boolean) {
    if (value) {
      this.$emit('selected');
    } else {
      this.$emit('unselected');
    }
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.my-trips-item {

    display: flex;
    align-items: center;

    margin: 0px;
    padding-left: @margin-large;
    padding-right: @margin-large;
    padding-top: @vertical-margin-medium;
    padding-bottom: @vertical-margin-medium;

    border-bottom: 1px solid @gainsboro;

    width: 100%;
    // TODO responsive
    height: 110px;

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

}

</style>
