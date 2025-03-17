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
    <div class="my-trips  shifted-background">
        <FisholaHeader />

        <div class="page-with-header my-trips-page">
            <div class="pane pane-only">
                <div class="pane-content large rounded">
                    <h1 class="no-margin-pane hide-on-mobile">Profil et Paramètres</h1>
                    <div class="main-tabs">
                        <div class="tab" :class="visualizationMode == 'profile' ? 'selected' : ''"
                            @click="changeVisualizationMode('profile')">
                            Mon profil
                        </div>
                        <div class="tab" :class="visualizationMode == 'profile' ? '' : 'selected'"
                            @click="changeVisualizationMode('settings')">
                            Paramètres
                        </div>
                    </div>
                    <div class="profile-header keyboardSensitive" v-if="visualizationMode == 'profile' && profile">
                        <Avatar v-bind:initials="profile.initials" class="profile-avatar" />
                        <div class="profile-header-name">
                            {{ fullName }}
                        </div>
                    </div>
                    <ProfileView class="pane-content" v-if="visualizationMode == 'profile' && profile"
                        :profile="profile" @profile-updated="loadProfile" />
                    <SettingsView class="pane-content" v-else-if="profile" />
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";
import { Component, Prop, Vue } from "vue-property-decorator";
import ProfileView from "./Profile.vue";
import SettingsView from "./Settings.vue";
import { RouterUtils } from "@/router/RouterUtils";
import ProfileService from "@/services/ProfileService";
import UserProfile from "@/pojos/UserProfile";
import router from "@/router";
import Avatar from "@/components/common/Avatar.vue";

@Component({
    components: {
        FisholaHeader,
        ProfileView,
        FisholaFooter,
        SettingsView,
        Avatar
    },
})
export default class ProfileAndSettingsView extends Vue {
    @Prop()
    visualizationMode: string;

    profile: UserProfile = {
        firstName: "",
        email: "",
        initials: "",
        sampleBaseId: "",
        offlineMarker: false,
        acceptsMailNotifications: false,
        lastNewsSeenDate: new Date(),
    };
    fullName: string = "";

    mounted() {
        this.loadProfile()
    }

    loadProfile() {
        ProfileService.getProfile().then((profile) => {
            this.profile = profile
            this.fullName = UserProfile.fullName(profile);
        }), () => {
            this.$root.$emit("toaster-warning", "Vous n'êtes plus connecté\u00B7e");
            RouterUtils.pushRouteNoDuplicate(router, "/login");
        };
    }


    changeVisualizationMode(newMode: string) {
        if (this.visualizationMode !== newMode) {
            this.$router.push({ params: { visualizationMode: newMode } });
        }
    }

}
</script>

<style lang="less">
@import "../less/main";

.profile-header {
    background-image: url("~/public/img/background_transparent.png");
    background-repeat: no-repeat;
    background-color: @pelorous;
    background-size: cover;
    background-position: center;

    height: 150px;
    margin-left: -1 * @margin-large;
    margin-bottom: 10px;
    margin-top: -5px;
    width: calc(100% + (2 * @margin-large));

    @media screen and (min-width: @desktop-min-width) {
        width: calc(100% + (2 * @margin-large-desktop));
        margin-left: -1 * @margin-large-desktop;
    }

    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;

    &.keyboardShowing {
        display: none;
    }

    .pastille {
        width: 70px;
        height: 70px;
        font-size: @pastille-size;
        line-height: calc(@pastille-size + @line-height-padding-large);
        color: @gunmetal;
    }

    .profile-header-name {
        margin-top: @vertical-margin-small;
        font-size: @fontsize-title;
        line-height: calc(@fontsize-title + @line-height-padding-xx-large);
        color: @white;
    }

    @media (max-height: 600px) {
        height: 100px;

        .pastille {
            width: 60px;
            height: 60px;
        }
    }
}

@media screen and (min-width: @desktop-min-width) {
    .profile-header {
        height: 200px;
    }
}
</style>
