<template>
  <div class="form-multi-values">
    <div class="header">
      <label>
        {{label}}
      </label>
      <a v-if="!readonly" v-on:click="clicked">Modifier</a>
    </div>
    <ul>
      <li v-for="v in values" v-bind:key="v">{{v}}</li>
    </ul>
  </div>
</template>

<script lang="ts">

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
  }
})
export default class FormMultiValues extends Vue {
  @Prop() name!: string;
  @Prop() label?: string;
  @Prop() values?: string[];
  @Prop() readonly!: boolean;

  clicked() {
    this.$root.$emit('toaster-warning', 'Work in progress');
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.form-multi-values {
  margin-top: 6px;

  font-size: 12px;
  line-height: 16px;

  display: flex;
  flex-direction: column;
  align-items: flex-start;

  border-bottom: 1px solid @solitude;
  margin-bottom: 20px;
  padding-bottom: 5px;

  .header {

    width: 100%;

    display: flex;
    flex-direction: row;
    justify-content: space-between;

    label {
      font-weight: 300;
      color: @black;
    }

    a {
      font-weight: bold;
      font-size: 14px;
      line-height: 19px;
      color: @pelorous;
    }
  }

  ul {
    margin-top: 0px;
    margin-bottom: 0px;
    padding-left: 0px;

    li {
      list-style:none;
      font-size: 12px;
      line-height: 16px;
      color: @gunmetal;
      text-align: left;
    }
  }

}

</style>
