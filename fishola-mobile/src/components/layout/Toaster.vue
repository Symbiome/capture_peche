<template>
  <div class="toaster" v-bind:class="visibility">
      <div class="toaster-box" v-bind:class="level">
        <div>
          <i class="icon-error" v-if="level == 'error'"/>
          <i class="icon-warning" v-if="level == 'warning'"/>
          <i class="icon-success" v-if="level == 'success'"/>
          {{message}}
        </div>
      </div>
  </div>
</template>

<script lang="ts">

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Toaster extends Vue {

  visibility:string = 'toaster-hidden';
  level:string = '';
  message:string = '';

  mounted() {
    this.$root.$on('toaster-error', (text:string, durationArg?:number) => {
      let duration = durationArg || 2000;
      this.newError(text, duration);
    });
    this.$root.$on('toaster-warning', (text:string, durationArg?:number) => {
      let duration = durationArg || 2000;
      this.newWarning(text, duration);
    });
    this.$root.$on('toaster-success', (text:string, durationArg?:number) => {
      let duration = durationArg || 2000;
      this.newSuccess(text, duration);
    });
  }

  beforeDestroy() {
    console.debug("Destroy toaster");
    this.$root.$off('toaster-error');
    this.$root.$off('toaster-warning');
    this.$root.$off('toaster-success');
  }

  newError(text:string, duration:number) {
    this.newMessage(text, 'error', duration);
  }

  newWarning(text:string, duration:number) {
    this.newMessage(text, 'warning', duration);
  }

  newSuccess(text:string, duration:number) {
    this.newMessage(text, 'success', duration);
  }

  newMessage(text:string, level:string, duration:number) {
    this.message = text;
    this.level = level;
    this.visibility = "toaster-visible";
    setTimeout(this.resetToaster, (2*500) + duration);
  }

  resetToaster() {
    this.visibility = "toaster-disappears";
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";

  .toaster-hidden {
    top: calc(-1 * @toaster-height);
  }

  .toaster-disappears {
    animation-duration: 0.5s;
    animation-name: disappear;

    top: calc(-1 * @toaster-height);

    @keyframes disappear {
      from {top: 0px;}
      to {top: calc(-1 * @toaster-height);}
    }
  }

  .toaster-visible {
    animation-duration: 0.5s;
    animation-name: appear;

    top: 0px;

    @keyframes appear {
      from {top: calc(-1 * @toaster-height);}
      to {top: 0px;}
    }
  }


  .toaster {
    position:absolute;
    left: 0px;
    width: 100%;
    height: @toaster-height;
    z-index: 999;

    display: flex;
    justify-content: center;
    align-items: center;

    .toaster-box {
      width: 100%;
      height: 100%;

      display: flex;
      justify-content: center;
      align-items: center;

      div {
        font-size: 12px;
        line-height: 16px;

        i {
          margin-right: 6px;
          font-size: 12px;
        }
      }
    }

    .toaster-box.error {
      color: @white;
      background: @cardinal;
    }

    .toaster-box.warning {
      color: @white;
      background: @carrot-orange;
    }

    .toaster-box.success {
      color: @white;
      background: @lime-green;
    }

  }

</style>
