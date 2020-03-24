<template>
  <div class="footer">
    <FooterButton v-if="!hideButton && (buttonIcon || buttonText)"
                  v-bind:icon="buttonIcon"
                  v-bind:text="buttonText"
                  v-on:clicked="$emit('buttonClicked')"/>

    <div class="footer-element pastille"
         v-if="activeButtons['logout']"
         v-on:click="logout">
      <i class="icon-logout"></i>
    </div>
    <div class="footer-element pastille"
         v-if="activeButtons['back']"
         v-on:click="goBack">
      <i class="icon-arrow icon-back"></i>
    </div>
    <div class="footer-element steps"
         v-if="steps.length > 0">
      <div v-for="s in steps" 
           v-bind:key="s.id"
           v-bind:class="s.active?'step step-active':'step'">
        <!-- {{s.active}} -->
      </div>
    </div>
    <div class="footer-element pastille"
         v-if="activeButtons['spacer']">
         <!-- spacer -->
    </div>
    <div class="footer-element pastille"
         v-bind:class="selected=='dashboard'?'filled':'unfilled'"
         v-if="activeButtons['dashboard']"
         v-on:click="goDashboard">
      <i class="icon-dashboard"></i>
    </div>
    <div class="footer-element pastille"
         v-bind:class="selected=='settings'?'filled':'unfilled'"
         v-if="activeButtons['settings']"
         v-on:click="goSettings">
      <i class="icon-settings"></i>
    </div>
    <div class="footer-element pastille"
         v-bind:class="selected=='credits'?'filled':'unfilled'"
         v-if="activeButtons['credits']"
         v-on:click="goCredits">
      <i class="icon-info"></i>
    </div>
    <div class="footer-element pastille"
         v-bind:class="selected=='home'?'filled':'unfilled'"
         v-if="activeButtons['home']"
         v-on:click="goHome">
      <i class="icon-home"></i>
    </div>
    <div class="footer-element"
         v-if="timer">
      {{timer}}
    </div>
    <div class="footer-element pastille"
         v-if="activeButtons['giveup']"
         v-on:click="giveup">
      Abandon
    </div>
    <div class="footer-element pastille"
         v-if="activeButtons['delete']"
         v-on:click="doDelete">
      <i class="icon-delete"></i>
    </div>
    <div class="footer-element pastille"
         v-bind:class="selected=='profile'?'filled':'unfilled'"
         v-if="activeButtons['profile']"
         v-on:click="goProfile">
      <i class="icon-profile"></i>
    </div>
    <div class="footer-element pastille"
         v-bind:class="selected=='documentation'?'filled':'unfilled'"
         v-if="activeButtons['documentation']"
         v-on:click="goDocumentation">
      <i class="icon-files"></i>
    </div>
    <div class="footer-element pastille"
         v-bind:class="selected=='feedback'?'filled':'unfilled'"
         v-if="activeButtons['feedback']"
         v-on:click="openFeedback">
      <i class="icon-faq"></i>
    </div>
    <div class="footer-element pastille"
         v-if="activeButtons['blank']">
         <!-- spacer -->
    </div>
  </div>
</template>

<script lang="ts">

import TripsService from '@/services/TripsService';
import ProfileService from '@/services/ProfileService';

import router from '@/router';

import UserProfile from '@/pojos/UserProfile';
import FooterButton from '@/components/layout/FooterButton.vue';

import { Component, Prop, Vue } from 'vue-property-decorator';
import Helpers from '@/pojos/Helpers';

@Component({
  components: {
    FooterButton
  }
})
export default class FisholaFooter extends Vue {

  @Prop({default:'logout,dashboard,home'}) shortcuts!: string;
  @Prop() selected?: string;
  @Prop() buttonIcon?: string;
  @Prop() buttonText?: string;
  @Prop() backEvent?: string;
  @Prop({default: false}) hideButton: boolean;

  steps:any[] = [];
  timer:string = '';

  activeButtons: any = {
    back: false,
    logout: false,
    spacer: false,
    dashboard: false,
    home: false,
    giveup: false,
    delete: false,
    blank: false,
    credits: false,
    documentation: false,
    settings: false,
    profile: false,
    feedback: false
  };

  mounted() {
    let array = this.shortcuts.split(',');
    array.forEach(this.activeButton);
  }

  activeButton(key:string) {
    if (key && key.indexOf('step-') == 0) {
      let stepIndex = parseInt(key[5]);
      let stepCount = parseInt(key[7]);

      for (let i=1; i<=stepCount; i++) {
        this.steps.push({
          id: i,
          active: (i<=stepIndex)
        });
      }

    }
    if (key && key.indexOf('timer-') == 0) {
      let seconds:number = parseInt(key.substring(6));
      this.timer = Helpers.formatSecondsDurationTruncate(seconds);
    }
    this.activeButtons[key] = true;
  }

  logout() {
    if (confirm("Voulez-vous vous déconnecter ?")) {

      TripsService.hasRunningTrip()
        .then((result:boolean) => {
          if (!result || confirm("Vous avez une sortie en cours, elle sera perdue. Êtes-vous sûr\u00B7e ?")) {
            ProfileService.logout()
              .then(this.logguedOut);
          }
        });

    }
  }

  logguedOut() {
    router.push('/login');
  }

  goBack() {
    if (this.backEvent) {
      this.$emit(this.backEvent);
    } else {
      window.history.back();
    }
  }

  goDashboard() {
    router.push('/dashboard');
  }

  goCredits() {
    router.push('/credits');
    this.$root.$emit('close-feedback');
  }

  goDocumentation() {
    router.push('/documentation');
  }

  goHome() {
    router.push('/trips');
  }

  goProfile() {
    router.push('/profile');
  }

  goSettings() {
    router.push('/settings');
  }

  openFeedback() {
    this.$root.$emit('open-feedback');
  }

  giveup() {
    if (confirm("Voulez-vous vraiment abandonner cette sortie ?")) {
      TripsService.cancelCreations();
      router.push('/trips');
    }
  }

  doDelete() {
    this.$emit('deleteClicked');
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.footer {

  display: flex;
  justify-content: space-around;
  align-items: center;

  height: @footer-height;
  width: 100%;
  background-color: @zircon;
  color: @pelorous;

  .footer-element {
    height: 40px;
    width: 40px;
    line-height: 40px;
    text-align: center;

    i {
      font-size: 19px;
    }
  }

  .footer-element.steps {

    width: 80px;
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;

    .step {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      border: 1px solid @pelorous;

      margin-left: 5px;
      margin-right: 5px;

    }

    .step.step-active {
      background-color: @pelorous;
    }
  }

}
</style>
