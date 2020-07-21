<template>
  <div class="lakes">
    <Referential
      name="Utilisateurs" 
      url="/v1/security/users"
      :columns="userColumns"
      :canDelete=true></Referential>
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
export default class UsersVue extends Vue {

  userColumns:any[] = [
    {
      field: 'id',
      label: 'Identifiant',
      visible: false,
      readOnly: true
    },
    {
      field: 'firstName',
      label: 'Prénom',
      readOnly: true
    },
    {
      field: 'lastName',
      label: 'Nom',
      readOnly: true
    },
    {
      field: 'email',
      label: 'E-mail',
      readOnly: true
    },
    {
      field: 'gender',
      label: 'Genre',
      readOnly: true
    },
    {
      field: 'birthYear',
      label: 'Année de naissance',
      readOnly: true
    },
    {
      field: 'excludeFromExports',
      label: 'Exclu des exports',
      isABoolean: true
    }
  ];
}
</script>

<style scoped lang="less">

@import "../less/main";

</style>
