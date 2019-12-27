<template>
  <div class="my-trips-item">
    <div class="item-selection">
      <input type="checkbox" v-bind:id="'checkbox-' + trip.id" class="pelorous-checkbox" />
      <label v-bind:for="'checkbox-' + trip.id"></label>
    </div>
    <div class="item-description" v-on:click="openTrip">
      <div class="item-row">
        <div class="name">{{trip.name}}</div>
        <div class="right-part">
          <i v-if="trip.canBeModified" class="icon-edit warning"/>
        </div>
      </div>
      <div class="item-row">
        <div class="left-part">
          <i class="icon-calendar"/>{{trip.date}}
        </div>
        <div class="right-part">
          {{trip.duration}}<i class="icon-clock"/>
        </div>
      </div>
      <div class="item-row">
        <div class="left-part">
          <i class="icon-lake"/>{{trip.lakeName}}
        </div>
        <div class="right-part">
          {{trip.catchs}}<i class="icon-fish"/>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import TripLight from '@/pojos/TripLight';

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../../router';

@Component
export default class MyTripItem extends Vue {
  @Prop() trip!: TripLight;

  openTrip() {
    router.push({name:'trip', params: {id: this.trip.id}});
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
    padding-left: 30px;
    padding-right: 30px;
    padding-top: 20px;
    padding-bottom: 20px;

    border-bottom: 1px solid @gainsboro;

    width: 100%;
    height: 110px;

    .item-selection {
      width: 16px;
      height: 16px;

      input {
        margin: 0px;
      }

    }

    .item-description {
      margin-left: 20px;
      width: 100%;

      font-size: 12px;
      line-height: 19px;

      .item-row {
        display: flex;
        justify-content: space-between;
        margin-top: 5px;

        color: @pale-sky;

        .name {
          font-weight: bold;
          font-size: 14px;
          color: @gunmetal;
        }

        .left-part {
          i {
            margin-right: 10px;
            color: @pale-sky;
          }
        }

        .right-part {
          i {
            margin-left: 10px;
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
