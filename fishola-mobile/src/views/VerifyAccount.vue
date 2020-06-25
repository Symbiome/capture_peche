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
  <div class="verify page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="verify-page page">
      <div class="pane">
        <div class="pane-content large">
            <h1>Verify</h1>
            <p> {{ token }} </p>
        </div>
      </div>
    </div>
    <FisholaFooter back-event="onBackButton"
                   v-on:onBackButton="backToLogin"
                   shortcuts="back"/>
  </div>
</template>

<script lang="ts">
import Constants from '@/services/Constants';
import Helpers from '@/services/Helpers';

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';
import router from '../router';

@Component({
  components: {
    FisholaHeader,
    FisholaFooter
  }
})
export default class VerifyAccount extends Vue {
  @Prop() token!:string;

  backToLogin() {
    router.push('/login');
  }
}
</script>
<style lang="less">

@import "../less/main";

</style>