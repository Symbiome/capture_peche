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
        <b-modal v-if="editable"
                 :active.sync="selection.item"
                 trap-focus
                 :destroy-on-hide="false"
                 aria-role="dialog"
                 aria-modal>
            <ReferentialItem :item="selection.item"
                             :columns="columns"
                             :backendUrl="url"
                             v-on:referential-updated="loadData">
            </ReferentialItem>
        </b-modal>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator'

import BackendService from '@/services/BackendService.ts';

import ReferentialItem from '@/components/ReferentialItem.vue';

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
    @Prop({default: true}) editable: boolean;

    selection = {item:null};

    mounted() {
        this.loadData();
    }

    loadData() {
        delete this.data;
        BackendService.backendGet(this.url).then((res) => this.data = res);
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
