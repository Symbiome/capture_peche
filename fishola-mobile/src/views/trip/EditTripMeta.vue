<template>
  <div class="edit-trip-meta page-with-header">
    <FisholaHeader />
    <div class="edit-trip-meta-page page">
      <SomeTripHeader/>
      <div class="edit-trip-meta-form">
        <h1>Information de pêche</h1>
        <div v-if="tripType">Type de trip: {{tripType}}</div>
      </div>
      <FisholaFooter buttons="back,step-1-4,giveup"/>
    </div>
  </div>
</template>

<script lang="ts">
import Trip from '@/pojos/Trip';
import TripsStorageService from '@/services/TripsStorageService';

import FisholaHeader from '@/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
  
@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    FisholaFooter
  }
})
export default class EditTripMeta extends Vue {
  
  @Prop() id!:string;

  tripType:string = '';

  created() {
    TripsStorageService.getTrip(this.id, this.tripLoaded);
  }

  mounted() {
  }

  tripLoaded(someTrip:any) {
    console.log("Trip chargé", someTrip);
    this.tripType = someTrip.type;
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


  .edit-trip-meta-form {

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
