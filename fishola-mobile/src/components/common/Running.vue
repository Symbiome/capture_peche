<template>
  <span class="running" :class="negative ? 'negative':''">
    <span class="dot1"></span>
    <span class="dot2"></span>
    <span class="dot3"></span>
  </span>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Running extends Vue {
  @Prop({ default: false }) negative:boolean;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";

  .running {
    margin: 100px auto 0;
    width: 50px;
    text-align: center;
  }

  .running > span {
    margin-left: 1px;
    margin-right: 1px;
    width: 3px;
    height: 3px;
    background-color: @gunmetal;

    border-radius: 100%;
    display: inline-block;
    -webkit-animation: sk-bouncedelay 1.4s infinite ease-in-out both;
    animation: sk-bouncedelay 1.4s infinite ease-in-out both;
  }

  .running.negative > span {
    background-color: @white-smoke;
  }

  .running .dot1 {
    -webkit-animation-delay: -0.32s;
    animation-delay: -0.32s;
  }

  .running .dot2 {
    -webkit-animation-delay: -0.16s;
    animation-delay: -0.16s;
  }

  @-webkit-keyframes sk-bouncedelay {
    0%, 80%, 100% { -webkit-transform: scale(0) }
    40% { -webkit-transform: scale(1.0) }
  }

  @keyframes sk-bouncedelay {
    0%, 80%, 100% { 
      -webkit-transform: scale(0);
      transform: scale(0);
    } 40% { 
      -webkit-transform: scale(1.0);
      transform: scale(1.0);
    }
  }

</style>
