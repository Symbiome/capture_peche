<template>
  <div class="toaster" v-bind:class="visibility">
      <div class="toaster-box error">
        <div><i class="icon-warning"/>{{errorMessage}}</div>
      </div>
  </div>
</template>

<script lang="ts">

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Toaster extends Vue {

  errorMessage?: string = '';
  visibility?:string = 'toaster-hidden';

  mounted() {
    // console.log("errorMessage: ", this.errorMessage);
    // this.visibility = "toaster-visible";

    this.$root.$on('toaster-error', (art1:string) => {
      this.newError(art1);
    });
  }

    newError(text:string) {
        this.errorMessage = text;
        this.visibility = "toaster-visible";
        setTimeout(this.resetToaster, 3000);
    }

  resetToaster() {
    this.visibility = "toaster-disappears";
    // this.errorMessage = '';
  }

    // @Watch('errorMessage')
    // onPropertyChanged(value: string, oldValue: string) {
    //     console.log("new Value: " + value);
    //     if (value) {
    //         this.visibility = "toaster-visible";
    //         setTimeout(this.resetToaster, 3000);
    //     } else if (!value) {
    //         this.visibility = "toaster-hidden";
    //     } else {
    //         console.error("Don't know");
    //     }
    // }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";

.toaster-hidden {
    top: -50px;
}

.toaster-disappears {
    animation-duration: 0.5s;
    animation-name: disappear;

    top: -50px;

    @keyframes disappear {
        from {top: 0px;}
        to {top: -50px;}
    }

}

.toaster-visible {
    animation-duration: 0.5s;
    animation-name: appear;

    top: 0px;

    @keyframes appear {
        from {top: -50px;}
        to {top: 0px;}
    }

}


  .toaster {
    position:absolute;
    left: 0px;
    width: 100%;
    height: 50px;
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

            font-size: 10px;
            line-height: 12px;

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

  }

</style>
