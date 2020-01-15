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
        <option v-for="option in options"
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
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.form-select {
  margin-top: 6px;

  font-size: 12px;
  line-height: 16px;

  // color: @white;

  display: flex;
  flex-direction: column;
  align-items: flex-start;

  label {
    font-weight: 300;
    color: @black;
  }

  select {
    padding-left: 10px;
    padding-right: 10px;
    margin-top: 5px;
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
    height: 14px;
  }
  div.field-error {
    background-color: transparent;
    color: @cardinal;
    font-size: 10px;
    line-height: 14px;
  }
}

</style>
