<template>
    <div class="referential">
        <h1>{{name}}</h1>
        <b-table
            :data="data"
            :striped="true"
            :loading="!data"
            :selected.sync="selection.item"
            >
            <template slot-scope="props">
                <b-table-column 
                    v-for="col in (columns.filter(col => col.visible !== false))"
                    :field="col.field" 
                    :label="col.label" 
                    :key="col.name" sortable>
                    {{ props.row[col.field] }}
                </b-table-column>
                <!-- Deletion button (only displayed if deleteFunction is defined) -->
                <b-table-column 
                    v-if="editable && deleteFunction != null"
                    label="Action"
                    @click.native="deleteFunction(props.row['id'])">
                    <button class="button is-small is-danger">
                        <b-icon icon="delete" size="is-small"></b-icon>
                    </button>
                </b-table-column>
            </template>
        </b-table>
        <div class="buttons">
            <!-- Creation button (only displayed if createFunction is defined) -->
            <button class="button is-primary" 
                v-if="editable && createFunction != null" 
                @click="createFunction()">
                Nouveau
            </button>
        </div>
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

    /* The function used to create elements. If not specified, create button will not be displayed */
    @Prop({default: null}) createFunction: () => void;
    /* The function used to delete elements. If not specified, deletion buttons will not be displayed */
    @Prop({default: null}) deleteFunction: (id: string) => void;

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

    .buttons {
        width: 100%;
        display: flex;
        flex-direction: row-reverse;
        padding-right: 30px;
        padding-top: 10px;
    }
}

</style>
