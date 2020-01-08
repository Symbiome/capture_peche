<template>
  <div class="edit-trip-meta page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="edit-trip-meta-page page">
      <SomeTripHeader v-bind:trip="trip"/>
      <div class="edit-trip-meta-content">
        <h1>Information de pêche</h1>
        <div class="form">
          <FormInput name="name"
                      label="Nom de la sortie"
                      placeholder="Nommez votre sortie"
                      v-model="name"
                      v-bind:error="nameError" />
          <FormSelect name="lake"
                      label="Lac"
                      v-bind:options="lakes"
                      v-model="lakeId"
                      v-bind:error="lakeIdError"/>
          <FormSelect name="type"
                      label="Type de pêche"
                      v-bind:options="types"
                      v-model="type"
                      v-bind:error="typeError" />
        </div>
        <div class="bottom-page-spacer"></div>
      </div>
    </div>
    <FisholaFooter button-text="Suivant"
                    v-on:buttonClicked="next"
                    shortcuts="back,step-1-4,giveup"/>
  </div>
</template>

<script lang="ts">
import Trip from '@/pojos/Trip';
import Lake from '@/pojos/Lake';
import Constants from '@/services/Constants';
import TripsService from '@/services/TripsService';
import ReferentialService from '@/services/ReferentialService';

import FormInput from '@/components/common/FormInput.vue'
import FormSelect from '@/components/common/FormSelect.vue'

import FisholaHeader from '@/layout/FisholaHeader.vue'
import SomeTripHeader from '@/components/trip/SomeTripHeader.vue'
import FisholaFooter from '@/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';
  
@Component({
  components: {
    FisholaHeader,
    SomeTripHeader,
    FormInput,
    FormSelect,
    FisholaFooter
  }
})
export default class TripMeta extends Vue {
  
  @Prop() id!:string;

  trip?:Trip = new Trip();

  name:string = '';
  nameError:string = '';
  lakeId:string = '';
  lakeIdError:string = '';
  type:string = '';
  typeError:string = '';

  lakes:Lake[] = [];
  types:any[] = [];

  created() {
    ReferentialService.getLakesAndTripTypes(this.referentialsLoaded);
  }

  mounted() {
  }

  tripLoaded(someTrip:any) {
    console.log("Trip chargé", someTrip);
    this.trip = someTrip;
    this.name = someTrip.name;
    this.lakeId = someTrip.lakeId;
    this.type = someTrip.type;
  }

  referentialsLoaded(ls:Lake[], tts:any[]) {
    ls.forEach((lake) => this.lakes.push(lake));
    tts.forEach((type) => this.types.push(type));
    TripsService.getTrip(this.id, this.tripLoaded);
  }

  next() {
    let hasError = false;
    if (this.lakeId) {
      this.lakeIdError = '';
    } else {
      hasError = true;
      this.lakeIdError = 'Information obligatoire';
    }
    if (this.name) {
      this.nameError = '';
    } else {
      hasError = true;
      this.nameError = 'Information obligatoire';
    }
    if (this.type) {
      this.typeError = '';
    } else {
      hasError = true;
      this.typeError = 'Information obligatoire';
    }
    if (hasError) {
      this.$root.$emit('toaster-error', 'Vous devez renseigner les champs obligatoires');
    } else {
      this.trip!.name = this.name;
      this.trip!.lakeId = this.lakeId;
      this.trip!.type = this.type;

      TripsService.saveTrip(this.trip!, this.tripSaved);
    }
  }

  tripSaved() {
    router.push({name:'trip-species', params: {id: this.id}});
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
