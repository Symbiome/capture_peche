<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
  <div class="picture-source-chooser">
    <div class="transparent-background" @click="$emit('close')"></div>
    <div class="pane popup-content">
      <div class="pane-content">
        <h1>Mesure automatique</h1>
        <div class="preconisations">
          <h4>Préconisations</h4>
          SLIDER
        </div>
        <div class="bottom-actions">
          <h4>Ajouter une image</h4>
          <div class="picture-source-item">
            <i class="pic icon-photo" />Depuis la gallerie
          </div>
          <div class="picture-source-item">
            <i class="pic icon-photo" />Depuis l'appareil photo
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";

@Component({
  components: {},
})
export default class MeasurementPicturePopup extends Vue {
  created() {}
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../../less/main";

.picture-source-chooser {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 100vh;
  width: 100%;
  z-index: 99;
  background-color: rgba(0, 0, 0, 0.6);

  @media screen and (min-width: @desktop-min-width) {
    width: calc(100% - @desktop-menu-width);
    margin-left: @desktop-menu-width;
  }

  .transparent-background {
    height: 30vh;
  }
  .popup-content {
    margin-top: 0px !important;
    height: 70vh;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;

    .bottom-actions {
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      background-color: @solitude;
      padding-left: @margin-large;
      padding-right: @margin-large;
      padding-bottom: @margin-medium;

      @media screen and (min-width: @desktop-min-width) {
        padding-left: @margin-large-desktop;
        padding-right: @margin-large-desktop;
      }

      .picture-source-item {
        padding-bottom: 5px;

        .pic {
          color: @pale-sky !important;
          padding-right: 10px;
          font-size: large;
        }
      }
    }
  }
}
</style>
