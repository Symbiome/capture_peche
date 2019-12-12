<template>
  <div class="form-group">
    <label v-bind:for="'field-' + name">
      {{label}}
    </label>
    <input v-bind:type="type"
            v-bind:id="'field-' + name"
            v-bind:name="name"
            v-bind:value="value"
            v-on:input="$emit('input', $event.target.value)"
            v-bind:placeholder="placeholder"
            v-bind:class="error?'field-error':''" />
    <div v-bind:class="error?'field-error':''" >
      <span v-if="error">
        {{error}}
      </span>
    </div>
  </div>
</template>

<script lang="ts">

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
  }
})
export default class FisholaHeader extends Vue {
  @Prop() label?: string;
  @Prop({ default: 'text' }) type!: string;
  @Prop() name!: string;
  @Prop() value!: string;
  @Prop() placeholder?: string;
  @Prop() error?: string;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.form-group {
  margin-top: 6px;

  font-size: 12px;
  line-height: 16px;

  color: @white;

  display: flex;
  flex-direction: column;
  align-items: flex-start;

  label {
    font-weight: 300;
  }

  input {
    padding-left: 10px;
    padding-right: 10px;
    margin-top: 5px;
    width: 100%;
    height: 38px;
    border: 1px solid @transparent;
    border-radius: 4px;
    background-color: @black-alpha-50;
    color: @white;
  }

  input.field-error {
    border: 1px solid @cardinal;
  }

  input::placeholder {
    font-style: italic;
    font-weight: normal;
    font-size: 12px;
  }

  input:focus {
    color: @white;
  }

  div {
    height: 14px;
  }
  div.field-error {
    background-color: @cardinal;
    color: @white;
    font-size: 10px;
    line-height: 14px;
  }
}

</style>
