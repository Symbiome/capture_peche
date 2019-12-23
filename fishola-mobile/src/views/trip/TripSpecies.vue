<template>
  <div class="edit-trip-species page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-species-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="edit-trip-species-content">
        <h1>Espèce recherchée</h1>
        <div class="form">
          <div v-for="s in species" 
               v-bind:key="s.id"
               class="species-item"
               v-bind:class="speciesIds.indexOf(s.id) == -1 ? '' : 'selected'">
            <div class="item-selection">
              <input type="checkbox" 
                     v-bind:id="'checkbox-' + s.id"
                     v-bind:value="s.id"
                     v-model="speciesIds"
                     class="pelorous-checkbox" />
              <label v-bind:for="'checkbox-' + s.id"></label>
            </div>
            <div class="item-description" v-on:click="toggle(s)">
              {{s.name}}
            </div>
          </div>
          <div class="species-item">
            <div class="item-selection">
              <input type="checkbox" 
                     id="checkbox-other"
                     class="pelorous-checkbox" />
              <label for="checkbox-other"></label>
            </div>
            <div class="item-description">
              Autre
              <input type="text" 
                     name="other"
                     placeholder="Renseignez l'espèce"
                      />
            </div>
          </div>
          <div class="info">Utilisez “,” si vous recherchez plusieurs espèces</div>
        </div>
        <div class="bottom-page-spacer"></div>
      </div>
    </div>
    <FisholaFooter button-icon="icon-fishing"
                    button-text="Commencer"
                    v-on:buttonClicked="next"
                    shortcuts="back,step-2-4,giveup"/>
  </div>
</template>

<script lang="ts">
import Trip from '@/pojos/Trip';
import Species from '@/pojos/Species';
import Constants from '@/services/Constants';
import TripsService from '@/services/TripsService';
import ReferentialService from '@/services/ReferentialService';

import FisholaHeader from '@/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';
  
@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    FisholaFooter
  }
})
export default class TripSpecies extends Vue {
  
  @Prop() id!:string;

  trip?:Trip = new Trip();

  speciesIds:string[] = [];

  species:Species[] = [];


  created() {
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  mounted() {
  }

  tripLoaded(someTrip:any) {
    console.log("Trip chargé", someTrip);
    this.trip = someTrip;
    this.speciesIds = someTrip.speciesIds;
    ReferentialService.getSpecies(this.trip!.lakeId!, this.speciesLoaded);
  }

  speciesLoaded(result:Species[]) {
    result.forEach((s) => this.species.push(s));
  }

  toggle(s:Species) {
    let speciesId = s.id;
    let index = this.speciesIds.indexOf(speciesId);
    if (index == -1) {
      this.speciesIds.push(speciesId);
    } else {
      this.speciesIds.splice(index, 1);
    }
  }

  next() {
    let hasError = false;
    if (this.speciesIds.length == 0) {
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
      this.trip!.speciesIds = this.speciesIds;

      TripsService.finishTripCreation(this.trip!, this.tripSaved);

    }
  }

  tripSaved(savedId:string) {
    router.push({name:'trip', params: {id: savedId}});
  }

}

</script>

<style lang="less">

@import "../../less/main";

.edit-trip-species-page {

  display: flex;
  flex-direction: column;
  justify-content: space-between;

  text-align:center;

  overflow: auto;


  .edit-trip-species-content {

    flex:auto;

    display: flex;
    flex-direction: column;
    justify-content: flex-start;

    text-align:center;

    background-color: @white-smoke;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    padding-top: 30px;

    color: @gunmetal;

    h1 {
      margin-top: 0px;
      margin-bottom: 30px;
      height: 30px;
      font-style: normal;
      font-weight: normal;
      font-size: 22px;
      line-height: 30px;
      color: @pelorous;
      text-align: center;
    }

    .species-item {
      height: 56px;

      padding-left: 30px;
      padding-right: 30px;

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

      }
    }

    .info {
      font-style: italic;
      font-weight: 300;
      font-size: 10px;
      line-height: 14px;
      color: @pale-sky;
    }

  }

}

</style>
