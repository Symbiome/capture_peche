<template>
  <div>
    <FormMultiValues v-if="readonly"
                    v-bind:name="name"
                    v-bind:label="label"
                    v-bind:values="readonlyValues"
                    v-bind:readonly="true"/>
    <div v-if="!readonly" class="form-textarea">
      <label v-bind:for="'field-' + name">
        {{label}}
      </label>
      <textarea v-bind:name="name"
              v-bind:id="'field-' + name"
              v-bind:placeholder="placeholder"
              v-bind:value="value"
              v-on:input="$emit('input', $event.target.value)"
              v-bind:class="error?'field-error':''" />
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

import { Component, Prop, Vue } from 'vue-property-decorator';

import FormMultiValues from '@/components/common/FormMultiValues.vue'

@Component({
  components: {
    FormMultiValues
  }
})
export default class FormTextarea extends Vue {
  @Prop() name!: string;
  @Prop() label?: string;
  @Prop() placeholder?: string;
  @Prop() value!: string;
  @Prop() error?: string;
  @Prop() readonly!: boolean;

  readonlyValues:string[] = [];

  created() {
    if (this.value) {
      this.readonlyValues.push(this.value);
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.form-textarea {
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

  textarea {
    padding-left: 10px;
    padding-right: 10px;
    margin-top: 5px;
    width: 100%;
    height: 100px;
    border-radius: 4px;

    background: transparent;
    border: 1px solid @pale-sky;
    color: @gunmetal;

    &::placeholder {
      font-style: italic;
      font-weight: normal;
      font-size: 12px;
      color: @pale-sky;
    }

  }

  textarea.field-error {
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
