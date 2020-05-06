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
    weatherId: '',
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
    console.log("Trip chargé", someTrip);
    this.trip = someTrip;
  }

  toggle(s:Technique) {
    let techniquesId = s.id;
    let index = this.trip.techniqueIds.indexOf(techniquesId);
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

    padding-left: 40px;

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
      margin-left: 18px;
      width: 100%;

      font-size: 12px;
      line-height: 19px;

      text-align: left;

      display: flex;
      flex-direction: row;
      align-items: center;

      input {
        padding-left: 10px;
        padding-right: 10px;
        margin-top: 5px;
        margin-left: 20px;
        height: 38px;
        border-radius: 4px;

        background: transparent;
        border: 1px solid @pale-sky;
        color: @gunmetal;

        &::placeholder {
          font-style: italic;
          font-weight: normal;
          font-size: 12px;
          color: @pale-sky;
        }

      }

      .real-name {
        font-style: italic;
        color: @pale-sky;
        margin-left: 5px;
      }

    }
  }

  .info {
    font-style: italic;
    font-weight: 300;
    font-size: 10px;
    line-height: 14px;
    color: @pale-sky;
    text-align: center;
  }

}

</style>
