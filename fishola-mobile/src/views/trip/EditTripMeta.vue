<template>
  <div class="edit-trip-meta page-with-header">
    <FisholaHeader />
    <div class="edit-trip-meta-page page">
      <SomeTripHeader/>
      <div class="edit-trip-meta-content">
        <h1>Information de pêche</h1>
        <div class="form">
          <InputGroup name="name"
                      label="Nom de la sortie"
                      placeholder="Nommez votre sortie"
                      v-model="name"
                      v-bind:error="nameError" />
          <InputGroup name="lake"
                      label="Lac"
                      v-model="lake" />
          <InputGroup name="type"
                      label="Situation"
                      v-model="type" />
        </div>
      </div>
      <FisholaFooter button-text="Suivant"
                     v-on:buttonClicked="next"
                     shortcuts="back,step-1-4,giveup"/>
    </div>
  </div>
</template>

<script lang="ts">
import Trip from '@/pojos/Trip';
import TripsStorageService from '@/services/TripsStorageService';

import InputGroup from '@/components/common/InputGroup.vue'

import FisholaHeader from '@/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
  
@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    InputGroup,
    FisholaFooter
  }
})
export default class EditTripMeta extends Vue {
  
  @Prop() id!:string;

  mode:string = '';
  name:string = '';
  nameError:string = '';
  lake:string = '';
  type:string = '';

  created() {
    TripsStorageService.getTrip(this.id, this.tripLoaded);
  }

  mounted() {
  }

  tripLoaded(someTrip:any) {
    console.log("Trip chargé", someTrip);
    this.mode = someTrip.mode;
  }

  next() {
    window.alert("La suite, s'il vous plaît !");
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

.edit-trip-meta-page {

  display: flex;
  flex-direction: column;
  justify-content: space-between;

  text-align:center;

  overflow: auto;


  .edit-trip-meta-content {

    flex:auto;

    display: flex;
    flex-direction: column;
    justify-content: flex-start;

    text-align:center;

    background-color: @white-smoke;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    padding-left: 30px;
    padding-right: 30px;
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

  }

}

</style>
