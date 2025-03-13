<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
    <div class="bottom-inducement">
        <img :src="icon" />
        <div class="text">
            <strong>{{ title }}</strong>
            <p>{{ text }} </p>
        </div>
        <button class="button" @click="$emit('click')"> {{ actionText }}</button>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";

@Component
export default class BottomInducementView extends Vue {
    @Prop() icon: string;
    @Prop() title: string;
    @Prop() text: string;
    @Prop() actionText: string;
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../../less/main";

.bottom-inducement {
    color: white;
    display: flex;
    justify-content: center;
    gap: 20px;
    align-items: center;
    margin-top: 50px;
    margin-bottom: 100px;
    width: 100vw;
    padding: 20px;
    margin-left: calc(-1 * @margin-large);


    @media screen and (min-width: @desktop-min-width) {
        width: calc(100vw - @desktop-menu-width);
        margin-left: calc(-1 * @margin-large-desktop);
    }

    img {
        width: 50px;
        height: 50px;
    }

    button {
        height: 50px;
        border-radius: 50px;

        font-style: normal;
        font-weight: bold;
        font-size: @fontsize-button;
        line-height: calc(@fontsize-button + @line-height-padding-x-large);

        border: 0px;
        padding-left: @margin-medium;
        padding-right: @margin-medium;

        background-color: @terra-cotta;
        color: @white;
    }

    background: linear-gradient(-45deg, #0A3C4D, #0F5A6F, #0A3C4D, #06333F);
    background-size: 300% 300%;
    animation: gradientAnimation 6s ease infinite;
}

@keyframes gradientAnimation {
    0% {
        background-position: 0% 50%;
    }

    50% {
        background-position: 100% 50%;
    }

    100% {
        background-position: 0% 50%;
    }
}
</style>
