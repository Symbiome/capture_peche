<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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
  <div class="form-multi-values">
    <div class="header hiddenWhenKeyboardShows">
      <label>
        {{label}}
      </label>
      <a v-if="!readonly" v-on:click="clicked">Modifier</a>
    </div>
    <ul>
      <li v-for="v in values"
          v-bind:key="v"
          v-bind:class="v == 'Work in progress' ? 'warning':''"
          >{{v}}</li>
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
    this.$emit('clicked');
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.form-multi-values {
  margin-top: @vertical-margin-x-small;

  font-size: @fontsize-form-input;
  line-height: calc(@fontsize-form-input + @line-height-padding-medium);

  display: flex;
  flex-direction: column;
  align-items: flex-start;

  border-bottom: 1px solid @solitude;
  margin-bottom: @vertical-margin-medium;
  padding-bottom: @vertical-margin-xx-small;

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
      font-size: @fontsize-form-header;
      line-height: calc(@fontsize-form-header + @line-height-padding-large);
      color: @pelorous;
    }
  }

  ul {
    margin-top: 0px;
    margin-bottom: 0px;
    padding-left: 0px;

    li {
      list-style:none;
      font-size: @fontsize-form-input;
      line-height: calc(@fontsize-form-input + @line-height-padding-medium);
      color: @gunmetal;
      text-align: left;

      &.warning {
        color: @carrot-orange;
      }
    }
  }

}

</style>
