<template>
  <div class="my-trips-item">
    <div class="item-selection">
      <br/>
      <input type="checkbox"/>
    </div>
    <div class="item-description">
      <div class="name">{{trip.name}}</div>
      <div class="left-part">
        <i class="icon-calendar left-icon"/>
        {{trip.date}}
      </div>
      <div class="right-part">
        {{trip.duration}}
        <i class="icon-clock right-icon"/>
      </div>
      <div class="left-part">
        <i class="icon-lake left-icon"/>
        {{trip.lake}}
      </div>
      <div class="right-part">
        {{trip.catchs.length}}
        <i class="icon-fish right-icon"/>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import Trip from '@/pojos/Trip';
import { Component, Prop, Vue } from 'vue-property-decorator';

// export default {
//   name: 'my-trips-item',
//   components: {
//   }
// }

@Component
export default class MyTripItem extends Vue {
  @Prop() trip!: Trip
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.my-trips-item {
    margin: 0px;
    padding-left: 30px;
    padding-right: 30px;
    padding-top: 20px;
    padding-bottom: 20px;

    border-bottom: 1px solid @gainsboro;

    font-size: 12px;
    line-height: 16px;
    color: @pale-sky;
    width: 100%;
    height: 100px;

    .item-selection {
      width: 30px;
      float: left;
      height: 60px;

      input {
      }
    }

    .item-description {
      width: calc(100% - 30px);
      padding-left: 10px;
      float: left;
      line-height: 24px;

      .left-part {
        width: 70%;
        float: left;
      }
      .right-part {
        width: 30%;
        float: left;
        text-align: right;
      }
    }

    .name {
      font-weight: bold;
      font-size: 14px;
      line-height: 19px;
      color: @gunmetal;
    }

    .left-icon {
      margin-right: 10px;
      color: @pale-sky;
    }
    .right-icon {
      margin-left: 10px;
      color: @pelorous;
    }
}

</style>
