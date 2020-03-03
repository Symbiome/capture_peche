<template>
  <div class="feedback page-with-header-and-footer" v-bind:class="display ? '' : 'feedback-hidden'">
    <div class="page feedback-page">
      <div class="pane">
        <h1>Des retours ?</h1>
        <div class="pane-content">
            Un retour ? Une remarque ? Une envie ?
        </div>
      </div>
    </div>           
    <FisholaFooter shortcuts="back,credits,feedback"
                   button-icon="icon-send"
                   button-text="Envoyer"
                   v-on:buttonClicked="sendFeedback"
                   back-event="onBackButton"
                   v-on:onBackButton="closeFeedback"
                   selected="feedback" />
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    FisholaFooter
  }
})
export default class Feedback extends Vue {

  display = false;

  created() {
  }

  mounted() {
    this.$root.$on('open-feedback', this.openFeedback);
    this.$root.$on('close-feedback', this.closeFeedback);
  }

  beforeDestroy() {
    this.$root.$off('open-feedback');
    this.$root.$off('close-feedback');
  }

  openFeedback() {
    this.display = true;
  }

  closeFeedback() {
    this.display = false;
  }

  sendFeedback() {
    console.log("Ok, now send it!");
    this.closeFeedback();
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.feedback {
  position: absolute;
  top: 50px;
  left: 0px;
  z-index: 50;
  background-color: transparent;
  width: 100%;
  height: calc(100% - 50px);
  padding-top: 0px;

  &.feedback-hidden {
    display: none;
  }

  .page {
    height: calc(100% - @footer-height);

    .pane {
      margin-top: 0px;
    }

  }
}

</style>
