<template>
  <div class="profile page-with-header-and-footer shifted-background">
    <FisholaHeader v-bind:avatar="false"/>
    <div class="page profile-page">
      <div class="pane">
        <h1>Mon profil ...</h1>
        <div class="pane-content">

        </div>
      </div>
    </div>
    <FisholaFooter shortcuts="back,settings,profile"
                   selected="profile" />
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
export default class Profile extends Vue {

  constructor() {
    super();
  }

  mounted() {
  }



}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../less/main";

.profile-page {

}

</style>
