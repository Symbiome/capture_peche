<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
  <div>
    <FormMultiValues v-if="readonly"
                     v-bind:name="name"
                     v-bind:label="label"
                     v-bind:values="readonlyValues"
                     v-bind:readonly="true"/>
    <div class="form-radio" v-if="!readonly">
      <label>
        {{label}}
      </label>
      <div class="choices">
        <label class="choice"
               v-for="option in options"
               v-bind:for="'field-' + name + '-' + option.id"
               v-bind:key="option.id">
          <input type="radio"
                 v-bind:name="name"
                 v-bind:value="option.id"
                 v-on:input="valueChanged($event)"
                 v-bind:id="'field-' + name + '-' + option.id"
                 class="pelorous-radio"
                 v-bind:checked="value == option.id ? 'checked' : ''"
                 />
            <label v-bind:for="'field-' + name + '-' + option.id" ></label>
          <span v-if="!readonly || (value == option.id)">{{ option.name }}</span>
        </label>
      </div>
      <div v-bind:class="error?'field-error':''" >
        <span v-if="error">
          {{error}}
        </span>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

/*
Pour comprendre le fonctionnement de ce composant : https://vuejs.org/v2/guide/components.html#Using-v-model-on-Components

Il faut savoir que :
`v-model="searchText"`

est équivalent à
`v-bind:value="searchText" v-on:input="searchText = $event"`

Donc dans le parent on fait `v-model="toto"` et ça se retrouve dans l'attribut `value` du présent composant.
Si modification, on émet un message au parent qui l'intercepte et met à jour son propre modèle.
*/

import FormMultiValues from '@/components/common/FormMultiValues.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FormMultiValues
  }
})
export default class FormRadio extends Vue {
  @Prop() name!: string;
  @Prop() label?: string;
  @Prop() value?: boolean;
  @Prop() error?: string;
  @Prop() options?: any[];
  @Prop() readonly!: boolean;

  readonlyValues:string[] = [];

  created() {
    if (this.options) {
      this.options.forEach((option) => {
        if (option.id == this.value) {
          this.readonlyValues.push(option.name);
        }
      });
    }
  }

  valueChanged(event:any) {
    const someValue = event.target.value;
    this.$emit('input', someValue);
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.form-radio {
  margin-top: @vertical-margin-x-small;

  font-size: @fontsize-form-input;
  line-height: calc(@fontsize-form-input + @line-height-padding-medium);

  display: flex;
  flex-direction: column;
  align-items: flex-start;

  label {
    font-weight: 300;
    color: @black;
  }

  .choices {
    display: flex;
    flex-direction: row;
    align-items: flex-start;
    justify-content: center;
    height: 21px;
    color: @gunmetal;

    .choice {
      height: 21px;
      margin-right: @margin-medium;

      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: center;

      input {
        height: 16px;
        margin-right: @margin-small;
      }

      span {
        margin-left: @margin-small;
      }
    }
  }

  select {
    padding-left: @margin-small;
    padding-right: @margin-small;
    margin-top: @vertical-margin-xx-small;
    width: 100%;
    height: 38px;
    border-radius: 4px;

    background: transparent;
    border: 1px solid @pale-sky;
    color: @gunmetal;
  }

  select.field-error {
    border: 1px solid @cardinal !important;
  }

  div {
    height: calc(@fontsize-form-error + @line-height-padding-medium);
  }
  div.field-error {
    background-color: transparent;
    color: @cardinal;
    font-size: @fontsize-form-error;
    line-height: calc(@fontsize-form-error + @line-height-padding-medium);
  }

  @media screen and (min-width: @desktop-min-width) {
    font-size: @fontsize-form-input-desktop;
    line-height: calc(@fontsize-form-input-desktop + @line-height-padding-medium);
  }
}

</style>
