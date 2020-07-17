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
  <div class="edit-trip-techniques page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-techniques-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="pane">
        <div class="pane-content rounded">
          <h1>Technique utilisée</h1>
          <div v-for="s in techniques"
               v-bind:key="s.id"
               class="techniques-item"
               v-bind:class="trip.techniqueIds.indexOf(s.id) == -1 ? '' : 'selected'">
            <div class="item-selection">
              <input type="checkbox"
                     v-bind:id="'checkbox-' + s.id"
                     v-bind:value="s.id"
                     v-model="trip.techniqueIds"
                     class="pelorous-checkbox" />
              <label v-bind:for="'checkbox-' + s.id"></label>
            </div>
            <div class="item-description" v-on:click="toggle(s)">
              {{s.alias ? s.alias : s.name}}
              <span v-if="s.alias" class="real-name">({{s.name}})</span>
            </div>
          </div>
          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter button-text="Enregistrer"
                   v-on:buttonClicked="saveTechniques"
                   shortcuts="back,step-4-4,giveup"/>
  </div>
</template>

<script lang="ts">
import {Technique, TripBean} from '@/pojos/BackendPojos';
import Constants from '@/services/Constants';
import TripsService from '@/services/TripsService';
import ReferentialService from '@/services/ReferentialService';

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    FisholaFooter
  }
})
export default class TripTechniquesView extends Vue {

  @Prop() id!:string;

  trip:TripBean = {
    id: '',
    mode: 'Live',
    type: 'Craft',
    name: '',
    lakeId: '',
    speciesIds: [],
    date: new Date(),
    startedAt: '',
    finishedAt: '',
    catchs: [],
    otherSpecies: '',
    techniqueIds:[]
  };

  techniques:Technique[] = [];

  created() {
    ReferentialService.getTechniques()
      .then(this.techniquesLoaded);
  }

  mounted() {
  }

  techniquesLoaded(list:Technique[]) {
    this.techniques = list;
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  tripLoaded(someTrip:TripBean) {
    console.debug("Trip chargé", someTrip);
    this.trip = someTrip;
  }

  toggle(s:Technique) {
    const techniquesId = s.id;
    const index = this.trip.techniqueIds.indexOf(techniquesId);
    if (index == -1) {
      this.trip.techniqueIds.push(techniquesId);
    } else {
      this.trip.techniqueIds.splice(index, 1);
    }
  }

  saveTechniques() {
    let hasError = false;

    if (this.trip.techniqueIds.length == 0) {
      hasError = true;
      this.$root.$emit('toaster-error', 'Vous devez sélectionner au moins une technique');
    }

    if (!hasError) {
      TripsService.saveTrip(this.trip, this.tripSaved);
    }
  }

  tripSaved() {
    if (this.id == 'RUNNING') {
      router.push({name:'trip-summary', params: {id: this.id}});
    } else {
      router.push({name:'trip', params: {id: this.id}});
    }
  }

}

</script>

<style lang="less">

@import "../../less/main";

.edit-trip-techniques-page {

  .pane .pane-content {
    padding-left: 0px;
    padding-right: 0px;
  }

  .techniques-item {
    height: 56px;

    padding-left: @margin-x-large;

    display: flex;
    flex-direction: row;
    align-items: center;

    border-top: 1px solid @gainsboro;

    &.selected {
      background-color: @solitude;
    }

    &:first-child {
      border-top: 0px;
    }

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

      text-align: left;

      display: flex;
      flex-direction: row;
      align-items: center;

      input {
        padding-left: @margin-small;
        padding-right: @margin-small;
        margin-top: @vertical-margin-xx-small;
        margin-left: @margin-medium;
        height: 38px;
        border-radius: 4px;

        background: transparent;
        border: 1px solid @pale-sky;
        color: @gunmetal;

        &::placeholder {
          font-style: italic;
          font-weight: normal;
          font-size: @fontsize-form-input;
          color: @pale-sky;
        }

      }

      .real-name {
        font-style: italic;
        color: @pale-sky;
        margin-left: @margin-x-small;
      }

    }
  }

  .info {
    font-style: italic;
    font-weight: 300;
    font-size: @fontsize-info;
    line-height: calc(@fontsize-info + @line-height-padding-medium);
    color: @pale-sky;
    text-align: center;
  }

}

</style>
