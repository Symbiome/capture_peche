<template>
    <div class="referential-item">
        <h2 v-if="item.name">{{item.name}}</h2>
        <div v-for="col in columns" v-bind:key="col.field">
            <b-field :label="col.label">
                <b-input v-model="item[col.field]" :disabled="col.readOnly"></b-input>
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
}

</style>
