<template>
  <div class="new-trip">
    <div>
      <button><i class="icon-plus"/> Nouveau</button>
    </div>
  </div>
</template>

<script lang="ts">
import UserProfile from '@/pojos/UserProfile';
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Picture extends Vue {

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.new-trip {

  height: 40px;
  width: 100%;
  position: absolute;
  bottom: 80px;

  display: flex;
  justify-content: center;

  div {
    height: 40px;
    width: 152px;
  }

  button {
    height: 100%;
    width: 100%;
    border-radius: 50px;

    font-style: normal;
    font-weight: bold;
    font-size: 18px;
    line-height: 25px;

    color: @white;
    background-color: @terra-cotta;

    border: 0px;

    i {
      margin-right: 5px;
      font-size: 20px;
    }
  }
}

</style>
