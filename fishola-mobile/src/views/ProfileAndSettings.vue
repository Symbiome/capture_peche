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
    <div class="my-trips page-with-header shifted-background">
        <FisholaHeader />
        <div class="page my-trips-page">
            <div class="pane pane-only">
                <div class="pane-content rounded">
                    <div class="tabs">
                        <div class="tab" :class="showProfile ? 'selected' : ''" @click="showProfile = true">
                            Mon profil
                        </div>
                        <div class="tab" :class="showProfile ? '' : 'selected'" @click="showProfile = false">
                            Paramètres
                        </div>
                    </div>
                    <ProfileView v-if="showProfile" />
                    <SettingsView v-else />
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";
import { Component, Vue } from "vue-property-decorator";
import ProfileView from "./Profile.vue";
import SettingsView from "./Settings.vue";

@Component({
    components: {
        FisholaHeader,
        ProfileView,
        FisholaFooter,
        SettingsView,
    },
})
export default class ProfileAndSettingsView extends Vue {
    showProfile = true;

}
</script>

<style lang="less">
@import "../less/main";

.tabs {
    width: 100%;
    display: flex;
    flex-direction: row;
    justify-content: space-evenly;
    padding-top: 20px;

    .tab {
        width: 100%;
        display: flex;
        justify-content: center;
        text-align: center;
        gap: 5px;
        color: @pale-sky;
        padding-bottom: 5px;
        padding-left: 5px;
        padding-right: 5px;
        cursor: pointer;

        &.selected {
            color: @gunmetal;
            border-bottom: 2px solid @pelorous;
        }
    }
}

@media screen and (max-width: 760px) {
    .tab {
        padding-top: 20px;
        margin-top: 0px;
    }
}
</style>
