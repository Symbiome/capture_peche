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
    <FormMultiValues v-if="readonly" v-bind:name="name" v-bind:label="label" v-bind:values="readonlyValues"
      v-bind:readonly="true" />
    <div v-if="!readonly" class="form-input">
      <label v-bind:for="'field-' + name">
        {{ label }}
      </label>
      <input v-bind:name="name" v-bind:id="'field-' + name" v-bind:type="dynamicType" v-bind:placeholder="placeholder"
        v-bind:value="value" v-bind:min="min" v-on:input="$emit('input', $event.target.value)"
        v-bind:class="error ? 'field-error' : ''" @keyup.enter="$emit('keyupEnter')" />
      <i v-if="type == 'password'" @click="togglePasswordVisibility()" class="show-password-icon"
        alt="Afficher le mot de passe" :class="{
      'white-background': hasWhiteBackground,
      'icon-eye': dynamicType == 'password',
      'icon-eye-slash': dynamicType != 'password'
    }" />
      <div v-bind:class="error ? 'field-error' : ''">
        <span v-if="error">
          {{ error }}
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

import { Component, Prop, Vue } from 'vue-property-decorator';

import FormMultiValues from '@/components/common/FormMultiValues.vue'

@Component({
  components: {
    FormMultiValues
  }
})
export default class FormInput extends Vue {
  @Prop() name!: string;
  @Prop({ default: 'text' }) type!: string;
  @Prop() label?: string;
  @Prop() placeholder?: string;
  @Prop() value!: string;
  @Prop() error?: string;
  @Prop() readonly!: boolean;
  @Prop() min?: string;
  @Prop({ default: true }) hasWhiteBackground: boolean;
  dynamicType: string;

  readonlyValues: string[] = [];

  created() {
    if (this.value) {
      this.readonlyValues.push(this.value);
    }
    this.dynamicType = this.type;
  }

  togglePasswordVisibility() {
    if (this.dynamicType == 'password') {
      this.dynamicType = 'text';
    } else {
      this.dynamicType = 'password';
    }
    this.$forceUpdate();
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../../less/main";

.form-input {
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

  input {
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

    &::placeholder {
      font-style: italic;
      font-weight: normal;
      font-size: @fontsize-form-input;
      color: @pale-sky;
    }

  }

  input.field-error {
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

    input {
      font-size: @fontsize-form-input-desktop;
      height: 42px;

      &::placeholder {
        font-size: @fontsize-form-input-desktop;
      }

    }
  }
}

.show-password-icon {
  display: inline-block;
  position: absolute;
  font-size: 20px;
  cursor: pointer;
  color: white;
  z-index: 2;
  right: 38px;
  margin-top: 30px;

  &.white-background {
    color: @pelorous;
  }

  @media screen and (min-width: @desktop-min-width) {
    visibility: hidden;
    right: @margin-small * 8;
    margin-top: @vertical-margin-xx-small * 6;
  }

  &:hover {
    font-weight: bolder;
    color: @pelorous;

    &.white-background {
      color: white;
    }
  }
}
</style>
