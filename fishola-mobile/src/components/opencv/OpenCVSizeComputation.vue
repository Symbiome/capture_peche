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
  <div class="page-with-header-and-footer shifted-background">
    <FisholaHeader/>
    <div class="page">
      <div class="pane pane-only">

        <div class="pane-content large rounded">

            <h1 class="no-margin-pane"> OpenCV - Mesure de poissons (Test) </h1>
            <div class="measure-tests">
                Lefmost object size(mm)
                <input type="text"  style="width:100px"  id="leftSizeObjectSizeMm" value="133"/>
                Resize image size
                <input type="text"  style="width:100px"  id="fixedSize" value="150"/><br/>
                Min % of detected forms (between 0 and 1)
                <input type="text" style="width:100px" id="minCoverrage" value="0.15"/>
                <br/>
                <div class="inputoutput">
                <img style="display:none" id="imageSrc" alt="No Image" />
                <div class="caption">imageSrc <input type="file" id="fileInput" name="file" /></div>
                </div>
                <p id="status">OpenCV.js is loading...</p>
                <div class="inputoutput">
                    <div style="float:left;background-color:yellow;">
                        <canvas  id="canvasOutput1" ></canvas><br/>
                        <caption>Photo originale</caption>
                    </div>
                    <div style="clear:both"/>
                        <div style="float:left;background-color:green;">
                        <canvas  id="canvasOutput2" ></canvas><br/>
                        <caption>Photo traitée</caption>
                    </div>

                    <div style="clear:both"/>
                    <div style="float:left;background-color:green;">
                        <canvas  id="canvasOutput3" ></canvas><br/>
                        <caption>Calcul</caption>
                    </div>
                </div>
            </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue';
import FisholaFooter from '@/components/layout/FisholaFooter.vue';
import { Component, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    FisholaFooter
  }
})
export default class OpenCVSizeComputation extends Vue {

}
</script>

<style scoped lang="less">

@import "../../less/main";
.measure-tests {
  padding: 20px;
  padding-left:30px;
}
</style>