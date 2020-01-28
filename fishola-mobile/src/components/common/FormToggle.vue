<template>
  <div>
    <div class="form-toggle">
      <div class="toggle-row">
        <label>
          {{label}}
        </label>
        <img v-on:click="toggle()"
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
      this.$root.$emit('toaster-warning', 'Work in progress');
    }
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.form-toggle {
  margin-top: 6px;

  font-size: 12px;
  line-height: 16px;

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
    }
  }

  div.error-row {
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
