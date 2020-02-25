<template>
  <div class="profile page-with-header-and-footer shifted-background">
    <FisholaHeader v-bind:avatar="false"/>
    <div class="page profile-page">
      <div class="profile-header">
        <Avatar v-bind:initials='profile.initials'/>
        <div class="profile-header-name">
          {{fullName}}
        </div>
      </div>
      <div class="pane">
        <h1>Profil</h1>
        <div class="pane-content">

          <FormInput name="lastName"
                      label="Nom"
                      placeholder="Renseignez votre nom"
                      v-model="profile.lastName"
                      v-bind:error="validationErrors['lastName']"
                      />
          <FormInput name="firstName"
                      label="Prénom"
                      placeholder="Renseignez votre prénom"
                      v-model="profile.firstName"
                      v-bind:error="validationErrors['firstName']"
                      />
          <FormInput name="email"
                      label="E-mail"
                      placeholder="Renseignez votre E-mail"
                      v-model="profile.email"
                      v-bind:error="validationErrors['email']"
                      />
          <FormSelect name="birthYear"
                      label="Année de naissance (optionnelle)"
                      v-bind:options="years"
                      v-model="profile.birthYear"
                      />
          <FormSelect name="gender"
                      label="Sexe (optionnel)"
                      v-bind:options="genders"
                      v-model="profile.gender"/>
          <FormMultiValues name="password"
                           label="Mot de passe"
                           v-bind:values="['********']"/>
        </div>
      </div>
    </div>
    <FisholaFooter button-text="Modifier"
                   v-on:buttonClicked="saveProfile"
                   shortcuts="back,settings,profile"
                   selected="profile" />
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue';

import Avatar from '@/components/common/Avatar.vue';
import FormInput from '@/components/common/FormInput.vue';
import FormSelect from '@/components/common/FormSelect.vue';
import FormMultiValues from '@/components/common/FormMultiValues.vue';

import UserProfile from '@/pojos/UserProfile';
import ProfileService from '@/services/ProfileService';

import FisholaFooter from '@/components/layout/FisholaFooter.vue';

import router from '@/router';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    Avatar,
    FormInput,
    FormSelect,
    FormMultiValues,
    FisholaFooter
  }
})
export default class Profile extends Vue {

  profile:UserProfile = {firstName:'', email:'', initials:''};
  fullName:string = '';

  password:string = '';
  passwordConfirm:string = '';

  validationErrors:any = {
    passwordConfirm : ''
  };

  genders:any[] = [
      {id: 'Female', name: 'Femme'},
      {id: 'Male', name: 'Homme'},
      {id: 'NonBinary', name: 'Non binaire'}
  ];

  years:any[] = [];

  constructor() {
    super();
  }

  mounted() {
    let currentYear = 1900 + new Date().getYear();
    let startYear = currentYear - 100;
    let endYear = currentYear - 10;
    for (let i=startYear; i<=endYear; i++) {
      this.years.push({id: i, name: i});
    }
    this.loadProfile();
  }

  loadProfile() {
    ProfileService.getProfile()
        .then(
          this.profileLoaded,
          () => {
            this.$root.$emit('toaster-warning', 'Vous n\'êtes plus connecté\u00B7e');
            router.push('/login');
          });
  }

  profileLoaded(profile:UserProfile) {
    this.profile = profile;
    this.fullName = UserProfile.fullName(profile);
  }

  saveProfile() {
    ProfileService.saveProfile(this.profile)
      .then(
        () => {
          this.loadProfile();
          this.$root.$emit('profile-updated');
          this.$root.$emit('toaster-success', 'Profil enregistré');
        },
        () => {
          this.$root.$emit('toaster-error', 'Veuillez corriger les erreurs');
        }
      )
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../less/main";

.profile-page {

  .profile-header {
    height: 150px;
    width: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;

    .pastille {
      width: 70px;
      height: 70px;
      font-size: 30px;
      line-height: 35px;
      color: @gunmetal;
    }
    .profile-header-name {
      margin-top: 10px;
      font-size: 22px;
      line-height: 30px;
      color: @white;
    }
  }
}

</style>
