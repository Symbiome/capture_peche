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
    <div class="form-toggle">
      <div class="toggle-row">
        <label>
          {{label}}
        </label>
        <img v-on:click="toggle()" alt="interrupteur"
             v-bind:src="value ? '/img/toggle_on.svg' : '/img/toggle_off.svg'"/>
      </div>
      <div v-bind:class="error?'error-row field-error':'error-row'" >
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

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
  }
})
export default class FormToggle extends Vue {
  @Prop() label?: string;
  @Prop() value?: boolean;
  @Prop() error?: string;
  @Prop() readonly!: boolean;

  readonlyValues:string[] = [];

  created() {
  }

  toggle() {
    if (!this.readonly) {
      this.$emit('input', !this.value);
    }
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.form-toggle {
  margin-top: @vertical-margin-x-small;

  font-size: @fontsize-form-input;
  line-height: calc(@fontsize-form-input + @line-height-padding-medium);

  div.toggle-row {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;

    label {
      font-weight: 300;
      color: @black;
    }

    img {
      height: 25px;
      cursor: pointer;
    }
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
