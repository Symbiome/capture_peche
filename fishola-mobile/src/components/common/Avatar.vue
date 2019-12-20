<template>
  <div class="pastille color0" v-on:click="$root.$emit('toaster-warning', 'Work in progress');">
    <span>{{initials}}</span>
  </div>
</template>

<script lang="ts">
import Constants from '@/services/Constants';

import UserProfile from '@/pojos/UserProfile';
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Avatar extends Vue {

  initials = '..';

  created() {
    let profile:UserProfile = UserProfile.getCurrent();
    if (profile) {
      this.initials = profile.initials;
    } else {
      this.loadProfile();
    }
  }

  loadProfile() {

    function httpCall(method: string, url:string, data:any, callback:(result:any)=>any) {
        var xhr = new XMLHttpRequest();
        xhr.open(method, url, true);
        xhr.withCredentials = true;
        if (callback) {
            xhr.onload = function() {
              // console.log(this);
              if (this.status == 200) {
                let responseText = this['responseText'];
                // console.log("responseText: " + responseText);
                let parsed = JSON.parse(responseText);
                callback(parsed);
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

    let apiUrl = Constants.apiUrl("/v1/security/profile");
    httpCall('GET', apiUrl, null, this.profileLoaded);

  }

  profileLoaded(result:any) {
    let profile = UserProfile.fromJson(result);
    UserProfile.setCurrent(profile);
    this.initials = profile.initials;
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";

  .pastille.color0 {
    color: @gunmetal;
    background: @avatar-background;
  }

</style>
