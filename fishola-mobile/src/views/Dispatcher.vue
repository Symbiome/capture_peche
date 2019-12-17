<template>
  <div class="dispatcher">
    <div class="spinner">&nbsp;</div>
  </div>
</template>

<script lang="ts">

import Constants from '@/services/Constants';

import router from '@/router'


import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {}
})
export default class Dispatcher extends Vue {

  constructor() {
    super();
  }

  mounted() {
    this.checkForActiveSession();
  }

  checkForActiveSession() {

    function httpCall(method: string, url:string, data:any, callback:(status:any)=>any) {
        var xhr = new XMLHttpRequest();
        xhr.open(method, url, true);
        xhr.withCredentials = true;
        if (callback) {
            xhr.onload = function() {
              callback(this.status);
          };
        }
        if (data != null) {
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.send(JSON.stringify(data));
        }
        else xhr.send();
    }

    let apiUrl = Constants.apiUrl("/v1/security/profile");
    httpCall('GET', apiUrl, null, this.done);

  }

  done(status:number) {
    switch(status) {
      case 200:
        router.push('trips');
        break;
      default:
        router.push('login');
    }
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.dispatcher {

  height: 100%;

  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  @keyframes spin { 100% { -webkit-transform: rotate(360deg); transform:rotate(360deg); } }

  .spinner {
    height: 60px;
    width: 60px;
    border-radius: 50%;
    border-top: 3px solid @white;
    border-left: 3px solid @white;
    animation:spin 2s linear infinite;
  }

}

</style>
