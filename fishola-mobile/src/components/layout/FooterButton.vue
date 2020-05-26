<template>
  <div class="footer-button">
    <div>
      <button v-on:click="$emit('clicked')" v-bind:class="deleteMode ? 'delete' : ''">
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
  @Prop({default: false}) deleteMode:boolean;

  mounted() {
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.footer-button {

  height: 41px;
  width: fit-content;
  position: absolute;
  bottom: calc(@footer-height - 13px);

  &.keyboardShowing {
    bottom: min(calc(@reduced-footer-height - 13px), 2px);
  }
  display: flex;
  justify-content: center;

  z-index: 10;

  div {
    height: 41px;
  }

  button {
    height: 100%;
    width: 100%;
    border-radius: 22px;

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

    &.delete {
      background-color: @cardinal;
    }
  }
}

</style>
