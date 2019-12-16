<template>
  <div class="footer">
    <a class="footer-element pastille" v-on:click="logout">
      <i class="icon-logout"></i>
    </a>
    <div class="footer-element pastille" v-bind:class="selected=='dashboard'?'filled':'unfilled'" v-on:click="dashboard">
      <i class="icon-dashboard"></i>
    </div>
    <div class="footer-element pastille" v-bind:class="selected=='home'?'filled':'unfilled'" v-on:click="home">
      <i class="icon-home"></i>
    </div>
  </div>
</template>

<script lang="ts">

import Constants from '@/services/Constants';

import router from '@/router'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class FisholaFooter extends Vue {

  @Prop() selected?: string;

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

    let apiUrl = Constants.apiUrl("/v1/security/logout");
    httpCall('GET', apiUrl, null, this.logguedOut);

  }

  logguedOut() {
    router.push('/');
  }

  dashboard() {
    router.push('/dashboard');
  }

  home() {
    router.push('/trips');
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
}
</style>
