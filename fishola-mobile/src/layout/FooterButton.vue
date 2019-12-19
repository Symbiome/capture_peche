<template>
  <div class="footer-button">
    <div>
      <button v-on:click="$emit('clicked')">
        <i v-if="icon" v-bind:class="icon"/>
        {{text}}
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class FooterButton extends Vue {
  @Prop() icon?:string;
  @Prop() text?:string;

  mounted() {
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.footer-button {

  height: 40px;
  width: 100%;
  position: absolute;
  bottom: calc(@footer-height - 20px);

  display: flex;
  justify-content: center;

  div {
    height: 40px;
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
    padding-left: 20px;
    padding-right: 20px;

    i {
      margin-right: 5px;
      font-size: 20px;
    }
  }
}

</style>
