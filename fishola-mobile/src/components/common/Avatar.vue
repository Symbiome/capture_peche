<template>
  <div class="pastille color0">
    <span>{{initials}}</span>
  </div>
</template>

<script lang="ts">
import UserProfile from '@/pojos/UserProfile';
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Picture extends Vue {

  initials = '..';

  mounted() {

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

    // let url = `http://${location.hostname}:8080/api/v1/security/profile`;
    // let url = `http://172.19.0.3:8080/api/v1/security/profile`;
    let url = `https://fishola-backend.demo.codelutin.com/api/v1/security/profile`;
    httpCall('GET', url, null, this.profileLoaded);

  }

  profileLoaded(result:any) {
    let profile = UserProfile.fromJson(result);
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
