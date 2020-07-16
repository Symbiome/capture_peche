<template>
  <div class="lakes">
    <Referential
      name="Lacs" 
      url="/v1/referential/lakes"
      :columns="lakeColumns"
      :createFunction=createLake
      :deleteFunction=deleteLake
      ></Referential>
  </div>
</template>

<script lang="ts">
import Referential from '@/components/Referential.vue'

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    Referential
  }
})
export default class LakesVue extends Vue {

  lakeColumns:any[] = [
    {
      field: 'id',
      label: 'Identifiant',
      visible: false,
      readOnly: true
    },
    {
      field: 'name',
      label: 'Nom'
    },
    {
      field: 'exportAs',
      label: 'Nom d\'export'
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

  createLake() {
    this.$buefy.dialog.alert('Create lake !');
  }

  deleteLake(id: string) {
    this.$buefy.dialog.alert('Delete lake ' + id + '!');
  }
}
</script>

<style scoped lang="less">

@import "../../less/main";

</style>
