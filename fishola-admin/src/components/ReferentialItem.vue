<template>
    <div class="referential-item">
        <h2 v-if="item.name">{{item.name}}</h2>
        <div v-for="col in columns" v-bind:key="col.field">
            <b-field :label="col.label">
                <b-input v-model="item[col.field]"
                         v-if="!col.isABoolean"
                         :disabled="col.readOnly"></b-input>

                <b-checkbox v-model="item[col.field]"
                            v-if="col.isABoolean">
                    {{item[col.field]?'Oui':'Non'}}
                </b-checkbox>
            </b-field>
        </div>
        <div class="buttons">
            <button class="button is-primary" @click="save()">Enregistrer</button>
            <button class="button" type="button" @click="onSaved()">Annuler</button>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator'

import BackendService from '@/services/BackendService.ts';

@Component({
  components: {
  }
})
export default class RefenretialItem extends Vue {
    @Prop() item!: any;
    @Prop() columns!: any[];
    @Prop() backendUrl!: string;

    save() {
        if (this.item.id) {
            let url = this.backendUrl + '/' + this.item.id;
            BackendService.backendPut(url, this.item).then(this.onSaved, (err) => console.error("You are fucked", err));
        } else {
            // Création
            window.alert('NYI');
        }
    }

    onSaved() {
        this.$emit('referential-updated');
        this.$parent.close();
    }


}
</script>

<style lang="less">

@import "../less/main";

.referential-item {

    padding: 10px;

    h2 {
        font-size: 24px;
    }

    background-color: @white;

    display: flex;
    flex-direction: column;

    .buttons {
        margin-top: 20px;

        width: 100%;

        display: flex;
        flex-direction: row-reverse;

        button {
            margin-left: 5px;
            margin-right: 5px;
        }

        .button.is-primary {
            background-color: @terra-cotta;
        }
    }

    .b-checkbox.checkbox:hover input[type="checkbox"]:not(:disabled) + .check,
    .b-checkbox.checkbox input[type="checkbox"]:checked + .check {
        border-color: @pelorous;
    }
    .b-checkbox.checkbox input[type="checkbox"]:checked + .check {
        background: @pelorous url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1 1'%3E%3Cpath style='fill:white' d='M 0.04038059,0.6267767 0.14644661,0.52071068 0.42928932,0.80355339 0.3232233,0.90961941 z M 0.21715729,0.80355339 0.85355339,0.16715729 0.95961941,0.2732233 0.3232233,0.90961941 z'%3E%3C/path%3E%3C/svg%3E") no-repeat center center;
    }

}

</style>
