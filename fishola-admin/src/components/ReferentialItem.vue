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
            <button v-if="!item.id" class="button is-primary" @click="save($parent.close)">Créer</button>
            <button v-if="item.id" class="button is-primary" @click="save($parent.close)">Enregistrer</button>
            <button class="button" type="button" @click="onSaved($parent.close)">Annuler</button>
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

    save(closeModal: (() => void)) {
        let onSavedCallback = () => {
            this.$emit('referential-updated');
            if (closeModal) {
                closeModal();
            }
        };
        if (this.item.id) {
            // Update : PUT
            let url = this.backendUrl + '/' + this.item.id;
            BackendService.backendPut(url, this.item).then(onSavedCallback, (err) => {
                this.$buefy.toast.open({
                    message: 'Erreur lors de la modification de ' + this.item['name'] + '. Veuillez vérifier vos modifications.',
                    type: 'is-danger'
                });
            });
        } else {
            // Create : POST
            let url = this.backendUrl;
            BackendService.backendPost(url, this.item).then(onSavedCallback, (err) => {
                this.$buefy.toast.open({
                    message: 'Erreur lors de la création. Veuillez vérifier qu\'un élément avec ce nom n\'existe pas déjà.',
                    type: 'is-danger'
                });
            });
        }
    }

    onSaved(closeModal: (() => void)) {
        this.$emit('referential-updated');
        if (closeModal) {
            closeModal();
        }
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

    }

}

</style>
