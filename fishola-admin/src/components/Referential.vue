<template>
    <div class="referential">
        <h1>{{name}}</h1>
        <b-table
            :data="data"
            :columns="columns"
            :striped="true"
            :loading="!data"
            :selected.sync="selection.item"
            >
        </b-table>
        <b-modal :active.sync="selection.item"
                 trap-focus
                 :destroy-on-hide="false"
                 aria-role="dialog"
                 aria-modal>
            <ReferentialItem :item="selection.item"
                             :columns="columns">
            </ReferentialItem>
        </b-modal>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator'

import ReferentialItem from '@/components/ReferentialItem.vue'

@Component({
  components: {
    ReferentialItem
  }
})
export default class Refenretial extends Vue {
    @Prop() name!: string;
    @Prop() url!: string;
    @Prop() columns!: any[];
    @Prop() data?: any[];

    selection = {item:null};


    backendGet(apiUrl:string):Promise<any> {
      return new Promise<any>((resolve, reject) => {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', apiUrl, true);
        xhr.withCredentials = true;
        xhr.onload = function() {
          if (this.status == 200) {
            let responseText = this['responseText'];
            let parsed = JSON.parse(responseText);
            resolve(parsed);
          } else if (this.status == 204) {
            resolve();
          } else {
            reject(this.status);
          }
        };
        xhr.send();
      });
    }

    mounted() {
        this.backendGet(this.url).then((res) => this.data = res);
    }
}
</script>

<style lang="less">

@import "../less/main";

.referential {

    h1 {
        font-size: 36px;
    }

}

</style>
