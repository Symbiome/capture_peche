<template>
    <div class="referential">
        <h1>{{name}}</h1>
        <b-table
            :data="data"
            :striped="true"
            :selected.sync="selection.item"
            :loading="!data"
            >
            <template slot-scope="props">
                <b-table-column 
                    v-for="col in (columns.filter(col => col.visible !== false))"
                    :field="col.field" 
                    :label="col.label" 
                    :key="col.name" sortable>
                    {{ props.row[col.field] }}
                </b-table-column>
                <!-- Deletion button (only displayed if delete is allow) -->
                <b-table-column 
                    v-if="editable && canDelete"
                    label="Action"
                    @click.native="showDeleteDialog($event, props.row)">
                    <button class="button is-small is-danger">
                        <b-icon icon="delete" size="is-small"></b-icon>
                    </button>
                </b-table-column>
            </template>
        </b-table>
        <div class="buttons">
            <!-- Creation button (only displayed if createElement is defined) -->
            <button class="button is-primary" 
                v-if="editable && createElement != null" 
                @click="showCreateDialog()">
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

    /* The function used to create new elements. If not specified, create button will not be displayed */
    @Prop({default: null}) createElement: () => any;
    /* Indicates whether user is allowed to deleted elements in the table. */
    @Prop({default: false}) canDelete: boolean;

    mounted() {
        this.loadData();
    }

    loadData() {
        delete this.data;
        BackendService.backendGet(this.url).then((res) => this.data = res);
    }

    showCreateDialog() {
        let newElement = this.createElement();
        // This will trigger modal appearance
        this.selection.item = newElement;
    }

    showDeleteDialog(event, element: any) {
        // Do not foward click event to row (would trigger modal)
        event.stopPropagation();

        // Ask for confirmation
        this.$buefy.dialog.confirm({
            title: 'Suppresion',
            message: 'Êtes-vous sur de vouloir supprimer ' + element['name'] + '?',
            confirmText: 'Supprimer',
            type: 'is-danger',
            hasIcon: true, 
            onConfirm: () => {
                // Sends an HTTP DELETE request at url/id
                BackendService.backendDelete(`${this.url}/${element['id']}`).then(
                (res) => {
                    this.$buefy.toast.open(element['name'] + ' supprimé');
                },
                (error) => {
                    this.$buefy.toast.open('Erreur lors de la suppression de ' + element['name']);
                });
            }
        });
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
