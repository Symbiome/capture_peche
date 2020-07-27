<!--
  #%L
  Fishola :: Admin
  %%
  Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #L%
  -->
<template>
    <div class="referential-item">
        <h2 v-if="item.name">{{item.name}}</h2>
        <div v-for="col in columns" v-bind:key="col.field">
            <b-field :label="col.label">
                <!-- Short strings -->
                <b-input v-model="item[col.field]"
                         v-if="!col.isFile && !col.isABoolean && ('' + item[col.field]).length < 200"
                         :disabled="col.readOnly || col.readOnlyEdition && item['id']"></b-input>
                <!-- Long strings -->
                <b-input v-model="item[col.field]"
                         type="textarea"
                         v-if="!col.isFile && !col.isABoolean && ('' + item[col.field]).length >= 200"
                         :disabled="col.readOnly || col.readOnlyEdition && item['id']"></b-input>
                <!-- Files -->
                <b-upload v-model="input.file"
                    v-if="col.isFile"
                    accept="application/pdf"
                    >
                    <a class="button is-primary">
                        <b-icon icon="upload"></b-icon>
                        <span>Cliquez pour téléverser un nouveau fichier</span>
                    </a>
                    <a 
                        v-if="!input.file && item[col.field] && item[col.field].length > 10" 
                        :href="item[col.field]" target="blank"> Voir le fichier actuel
                    </a>
                    <span 
                        v-if="input.file" 
                        :href="item[col.field]" target="blank">
                        {{ input.file.name }}
                    </span>
                </b-upload>

                <!-- Booleans -->
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
    input = { file: null};

    save(closeModal: (() => void)) {
        let onSavedCallback = () => {
            this.$emit('referential-updated');
            this.input.file = null;
            this.$buefy.toast.open({
                message: 'Modifications enregistrées',
                type: 'is-success'
            });
            if (closeModal) {
                closeModal();
            }
        };
        // Convert file in base64 (if required)
        if (this.input.file) {
            this.getBase64(this.input.file).then( (base64) =>  {
                this.item['base64Content'] = base64;
                this.doSave(onSavedCallback);                
            }, (err) => {
                this.$buefy.toast.open({
                    message: 'Erreur lors de la lecture du fichier.',
                    type: 'is-danger'
                });
            });
        } else {
            this.doSave(onSavedCallback);
        }
    }

    doSave(onSavedCallback: () => void) {
        if (this.item.id) {
            // Update : PUT
            let url = this.backendUrl + '/' + this.item.id;
            BackendService.backendPut(url, this.item).then(onSavedCallback, (err) => {
                this.input.file = null;
                this.$buefy.toast.open({
                    message: 'Erreur lors de la modification de ' + this.item['name'] + '. Veuillez vérifier vos modifications.',
                    type: 'is-danger'
                });
            });
        } else {
            // Create : POST
            let url = this.backendUrl;
            BackendService.backendPost(url, this.item).then(onSavedCallback, (err) => {
                this.input.file = null;
                this.$buefy.toast.open({
                    message: 'Erreur lors de la création. Veuillez vérifier qu\'un élément avec ce nom n\'existe pas déjà.',
                    type: 'is-danger'
                });
            });
        }
    }

    getBase64(file: any): Promise<string> {
        return new Promise<any>((resolve, reject) => {
            var reader = new FileReader();
            reader.readAsDataURL(file);
            reader.onload = function () {
               resolve(reader.result);
            };
            reader.onerror = function (error) {
                reject(error);
            };
        });
    }


    onSaved(closeModal: (() => void)) {
        this.$emit('referential-updated');
        this.input.file = null;
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
