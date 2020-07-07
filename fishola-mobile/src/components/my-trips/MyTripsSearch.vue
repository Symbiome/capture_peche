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
  <div class="my-trips-search">
    <div :class="offline ? 'offline' : ''">
      <i class="icon-magnifying-glass"></i>
      <input type="text"
             v-bind:placeholder="offline ? 'Recherche non disponible' : 'Rechercher une sortie'"
             name="search_term"
             v-bind:value="value"
             v-bind:disabled="offline"
             v-on:input="$emit('input', $event.target.value)"/>
    </div>
  </div>
</template>

<script lang="ts">

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class MyTripsSearch extends Vue {

  @Prop() value!: string;
  @Prop() offline!: boolean;

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.my-trips-search {
  height: 60px;
  padding-left: 20px;
  padding-right: 20px;
  padding-top: 10px;
  padding-bottom: 10px;
  
  @media(max-width:350px) {
    padding-left: 5px;
    padding-right: 5px;
    padding-top: 0px;
    padding-bottom: 0px;
  }
  div {
    background-color: @white;

    height: 40px;
    padding-left: 20px;
    border-radius: 30px;
    line-height: 40px;
    @media(max-width:350px) {
      padding-left: 5px;
      border-radius: 10px;
    }
    .icon-magnifying-glass {
      color: @pelorous;
      margin-right: 10px;
      @media(max-width:272px) {
        display:none;
      }
    }

    input {
      border: 0px;
    }

    input::placeholder {
      color: @pale-sky;
      font-style: italic;
      font-weight: normal;
      font-size: @fontsize-form-input;
    }


    &.offline {
      background-color: @solitude;

      input {
        background-color: @solitude;
      }
      input::placeholder {
        color: @carrot-orange;
        background-color: @solitude;
      }
    }

  }
}

</style>
