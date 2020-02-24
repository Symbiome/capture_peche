<template>
  <div>
    <FormMultiValues v-if="readonly"
                     v-bind:name="name"
                     v-bind:label="label"
                     v-bind:values="readonlyValues"
                     v-bind:readonly="true"/>
    <div class="form-yes-no" v-if="!readonly">
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
export default class FormYesNo extends Vue {
  @Prop() name!: string;
  @Prop() label?: string;
  @Prop() value?: boolean;
  @Prop() error?: string;
  @Prop() readonly!: boolean;

  options:any[] = [{id:true, name:"Oui"}, {id:false, name:"Non"}];

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
    let someValue = event.target.value;
    let someValueBoolean:boolean = someValue === 'true';
    this.$emit('input', someValueBoolean);
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.form-yes-no {
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

  .choices {
    display: flex;
    flex-direction: row;
    align-items: flex-start;
    justify-content: center;
    height: 21px;
    color: @gunmetal;

    .choice {
      height: 21px;
      margin-right: 20px;

      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: center;

      input {
        height: 16px;
        margin-right: 10px;
      }

      span {
        margin-left: 10px;
      }
    }
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
