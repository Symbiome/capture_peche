<!--
  #%L
  Fishola :: Admin
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
  <div class="lakes">
    <Referential
      name="Lacs"
      url="/v1/referential/lakes"
      :editable="isNationalAdmin"
      :columns="lakeColumns"
      :createElement=createLake
      ></Referential>
  </div>
</template>

<script lang="ts">
import Referential from '@/components/Referential.vue'
import BackendService from '@/services/BackendService';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    Referential
  }
})
export default class LakesVue extends Vue {
  isNationalAdmin = false;

  lakeColumns:any[] = [
    {
      field: 'id',
      label: 'Identifiant',
      visible: false,
      readOnly: true
    },
    {
      field: 'lakeCode',
      label: 'Code Lac',
      searchable: true,
    },
    {
      field: 'name',
      label: 'Nom',
      searchable: true,
    },
    {
      field: 'exportAs',
      label: 'Nom d\'export',
      visible: false,
    },
    {
      field: 'latitude',
      label: 'Latitude'
    },
    {
      field: 'longitude',
      label: 'Longitude'
    }
  ];

  createLake(): any {
    return {
      'lakeCode': '',
      'name': 'Nouveau lac',
      'exportAs': 'NouveauLac',
      'latitude': 45.5,
      'longitude': 5.8
    };
  }

  mounted() {
    BackendService.backendGet("/v1/admin/check").then(
      (admin) => {
        this.isNationalAdmin = admin.isNationalAdmin
      }
    );
  }
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
