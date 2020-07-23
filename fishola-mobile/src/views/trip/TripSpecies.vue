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
  <div class="edit-trip-species page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-species-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="pane">
        <div class="pane-content rounded">
          <h1>Espèce recherchée</h1>
          <div v-for="s in sortedSpecies()"
               v-bind:key="s.id"
               class="species-item"
               v-bind:class="trip.speciesIds.indexOf(s.id) == -1 ? '' : 'selected'">
            <div class="item-selection">
              <input type="checkbox" 
                     v-bind:id="'checkbox-' + s.id"
                     v-bind:value="s.id"
                     v-model="trip.speciesIds"
                     class="pelorous-checkbox" />
              <label v-bind:for="'checkbox-' + s.id"></label>
            </div>
            <div class="item-description" v-on:click="toggle(s)">
              {{s.alias ? s.alias : s.name}}
              <span v-if="s.alias" class="real-name">({{s.name}})</span>
            </div>
          </div>
          <div class="species-item">
            <div class="item-selection">
              <input type="checkbox" 
                     id="checkbox-other"
                     class="pelorous-checkbox"
                     v-model="hasOtherSpecies" />
              <label for="checkbox-other"></label>
            </div>
            <div class="item-description">
              <label for="checkbox-other">Autre</label>
              <input type="text" 
                     name="other"
                     placeholder="Renseignez l'espèce"
                     v-model="trip.otherSpecies"
                     v-bind:disabled="!hasOtherSpecies"
                     />
            </div>
          </div>
          <div class="info">Séparez les espèces par une virgule (&nbsp;,&nbsp;) pour en renseigner plusieurs</div>
          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter v-bind:button-text="buttonLabel"
                   v-on:buttonClicked="next"
                   shortcuts="back,step-2-4,giveup"/>
  </div>
</template>

<script lang="ts">
import TripSpecies from '@/pojos/TripSpecies';
import {SpeciesWithAlias} from '@/pojos/BackendPojos';
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
export default class TripSpeciesView extends Vue {
  
  @Prop() id!:string;

  trip:TripSpecies = { id:'', speciesIds:[], lakeId:'', mode:'Live', startedAt: '', otherSpecies:'' };

  hasOtherSpecies:boolean = false;

  species:SpeciesWithAlias[] = [];
  speciesIndex:Map<string, SpeciesWithAlias[]> = new Map();

  buttonLabel:string;

  created() {
    this.buttonLabel = this.id == Constants.NEW_TRIP_ID ? 'Commencer' : 'Enregistrer'
    ReferentialService.getSpeciesPerLakePlusCustom()
      .then(this.speciesLoaded);
  }

  mounted() {
  }

  sortedSpecies():SpeciesWithAlias[] {
    return Vue.lodash.orderBy(this.species, 'name');
  }

  speciesLoaded(map:Map<string, SpeciesWithAlias[]>) {
    this.speciesIndex = map;
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  tripLoaded(someTrip:TripSpecies) {
    console.debug("Trip chargé", someTrip);
    this.trip = someTrip;
    const lakeAndCustomSpecies = this.speciesIndex.get(this.trip.lakeId)!;
    this.species = [];
    lakeAndCustomSpecies.forEach((s) => {
      if (s.builtIn || this.trip.speciesIds.indexOf(s.id) != -1) {
        this.species.push(s);
      }
    });
    if (this.trip.otherSpecies) {
      this.hasOtherSpecies = true;
    }
  }

  toggle(s:SpeciesWithAlias) {
    const speciesId = s.id;
    const index = this.trip.speciesIds.indexOf(speciesId);
    if (index == -1) {
      this.trip.speciesIds.push(speciesId);
    } else {
      this.trip.speciesIds.splice(index, 1);
    }
  }

  next() {
    let hasError = false;

    if (this.hasOtherSpecies) {
      if (!this.trip.otherSpecies) {
        hasError = true;
        this.$root.$emit('toaster-error', 'Vous devez saisir le nom de l\'espèce');
      }
    } else {
      this.trip.otherSpecies = '';
    }
    if (this.trip.speciesIds.length == 0 && !this.hasOtherSpecies) {
      hasError = true;
      this.$root.$emit('toaster-error', 'Vous devez sélectionner au moins une espèce');
    }

    // if (this.name) {
    //   this.nameError = '';
    // } else {
    //   hasError = true;
    //   this.nameError = 'Information obligatoire';
    // }
    // if (this.type) {
    //   this.typeError = '';
    // } else {
    //   hasError = true;
    //   this.typeError = 'Information obligatoire';
    // }
    if (hasError) {
      //
    } else {
      TripsService.finishTripBootstrap(this.trip, this.tripSaved);
    }
  }

  summaryNotYetSaved(tripAsAny:any) {
    // Si on a pas encore la liste des techniques c'est qu'on est pas
    // encore passé par la sauvegarde sur l'écran de résumé #JoeLaBidouille
    return !tripAsAny.techniqueIds;
  }

  tripSaved(savedId:string) {
    if (this.id == Constants.NEW_TRIP_ID || this.summaryNotYetSaved(this.trip)) {
      router.push({name:'trip-catchs', params: {id: savedId}});
    } else if (this.id == 'RUNNING') {
      router.push({name:'trip-summary', params: {id: savedId}});
    } else {
      router.push({name:'trip', params: {id: savedId}});
    }
  }

}

</script>

<style lang="less">

@import "../../less/main";

.edit-trip-species-page {

  .pane .pane-content {
    padding-left: 0px;
    padding-right: 0px;
  }

  .species-item {
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


    @media(max-height:600px) {
      height: 46px;

      padding-left: @margin-large;

    }

    @media(max-width:360px) {

      padding-left: @margin-medium;

      .item-description {
        margin-left: @margin-small;

        input {
          width: 200px;
        }
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
