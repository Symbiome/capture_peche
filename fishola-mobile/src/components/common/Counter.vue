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
  <span>
    {{displayNumber}}
  </span>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';

@Component
export default class Counter extends Vue {

  displayNumber:number = 0;
  interval?:number;

  @Prop() n:number;

  @Watch('n')
  onNChanged(value: number, oldValue: number) {
    clearInterval(this.interval);
    if (this.n == this.displayNumber){
      return;
    }
    this.interval = setInterval(this.increaseNumber, 20);
  }

    increaseNumber() {
        if (this.displayNumber != this.n){
            var change = (this.n - this.displayNumber) / 10;
            change = change >= 0 ? Math.ceil(change) : Math.floor(change);
            this.displayNumber = this.displayNumber + change;
        }
    }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
</style>
