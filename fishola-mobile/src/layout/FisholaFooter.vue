<template>
  <div class="footer">
    <FooterButton v-if="buttonIcon || buttonText"
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
         v-bind:class="selected=='dashboard'?'filled':'unfilled'"
         v-if="activeButtons['dashboard']"
         v-on:click="goDashboard">
      <i class="icon-dashboard"></i>
    </div>
    <div class="footer-element pastille"
         v-bind:class="selected=='home'?'filled':'unfilled'"
         v-if="activeButtons['home']"
         v-on:click="goHome">
      <i class="icon-home"></i>
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
  </div>
</template>

<script lang="ts">

import Constants from '@/services/Constants';
import TripsService from '@/services/TripsService';

import router from '@/router'

import UserProfile from '@/pojos/UserProfile';
import FooterButton from '@/layout/FooterButton.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';

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

  steps:any[] = [];

  activeButtons: any = {
    back: false,
    logout: false,
    dashboard: false,
    home: false,
    giveup: false
  };

  mounted() {
    let array = this.shortcuts.split(',');
    array.forEach(this.activeButton);
  }

  activeButton(key:string) {
    if (key && key.indexOf('step-') == 0) {
      let stepIndex = parseInt(key[5]);
      let stepCount = parseInt(key[7]);
      // this.steps = [];

      for (let i=1; i<=stepCount; i++) {
        this.steps.push({
          id: i,
          active: (i<=stepIndex)
        });
      }

    }
    this.activeButtons[key] = true;
  }

  logout() {

    function httpCall(method: string, url:string, data:any, callback:()=>any) {
        var xhr = new XMLHttpRequest();
        xhr.open(method, url, true);
        xhr.withCredentials = true;
        if (callback) {
            xhr.onload = function() {
              // console.log(this);
              if (this.status == 200) {
                // let responseText = this['responseText'];
                // // console.log("responseText: " + responseText);
                // let parsed = JSON.parse(responseText);
                callback();
              } else if (this.status == 401) {
                console.error("Need to login");
              } else {
                console.error("C'est la merde noire");
              }
          };
        }
        if (data != null) {
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.send(JSON.stringify(data));
        }
        else xhr.send();
    }

    if (confirm("Voulez-vous vous déconnecter ?")) {
      let apiUrl = Constants.apiUrl("/v1/security/logout");
      httpCall('GET', apiUrl, null, this.logguedOut);
    }

  }

  logguedOut() {
    UserProfile.unsetCurrent();
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

  goHome() {
    router.push('/trips');
  }

  giveup() {
    if (confirm("Voulez-vous vraiment abandonner cette sortie ?")) {
      TripsService.cancelCreations();
      router.push('/trips');
    }
  }

  doDelete() {
    this.$root.$emit('toaster-warning', 'Work in progress');
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

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
