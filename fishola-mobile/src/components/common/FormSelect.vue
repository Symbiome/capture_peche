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
    <div class="form-select" v-if="!readonly">
      <label v-bind:for="'field-' + name">
        {{label}}
      </label>
      <select v-bind:name="name"
              v-bind:id="'field-' + name"
              v-bind:class="error?'field-error':''"
              v-bind:value="value"
              v-on:input="$emit('input', $event.target.value)"
              >
        <option value="" v-if="!value"></option>
        <option v-for="option in sortedOptions()"
                v-bind:key="option.id"
                v-bind:value="option.id">
          {{ option.name }}
        </option>
      </select>
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
export default class FormSelect extends Vue {
  @Prop() name!: string;
  @Prop() label?: string;
  @Prop() value?: string;
  @Prop() error?: string;
  @Prop() options?: any[];
  @Prop() readonly!: boolean;
  @Prop() orderBy?: string;

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

  sortedOptions():any[] {
    if (this.options && this.orderBy) {
      return Vue.lodash.orderBy(this.options, this.orderBy);
    }
    return this.options || [];
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.form-select {
  margin-top: @vertical-margin-x-small;

  font-size: @fontsize-form-input;
  line-height: calc(@fontsize-form-input + @line-height-padding-medium);

  // color: @white;

  display: flex;
  flex-direction: column;
  align-items: flex-start;

  label {
    font-weight: 300;
    color: @black;
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
    font-size: @fontsize-form-input;
    font-family: 'Open Sans', sans-serif;
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

    select {
      font-size: @fontsize-form-input-desktop;
      height: 42px;
    }
  }
}

</style>
