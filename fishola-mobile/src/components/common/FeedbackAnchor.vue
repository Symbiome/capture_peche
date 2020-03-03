<template>
  <div class="pastille filled" v-on:click="openFeedback">
    <i class="icon-faq"></i>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class FeedbackAnchor extends Vue {

  openFeedback() {
    this.$root.$emit('open-feedback');
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  // .feedback-anchor {
  //   width: 30px;
  //   height: 30px;
  //   text-align: center;
  //   display: flex;
  //   align-items: center;
  //   justify-content: center;
  // }
</style>
