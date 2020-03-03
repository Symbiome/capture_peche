<template>
  <div class="feedback page-with-header-and-footer" v-bind:class="display ? '' : 'feedback-hidden'">
    <div class="page feedback-page">
      <div class="pane">
        <h1>Des retours ?</h1>
        <div class="pane-content">
          <p>Si vous voyez une anomalie ou si vous avez simplement une remarque à faire, remplissez ce formulaire pour nous faire parvenir votre avis.</p>
          <p>N'hésitez pas à en abuser !</p>

          <FormRadio name="category"
                     label="Catégorie"
                     v-bind:options="categories"
                     v-model="model.categoryId"/>
          <FormInput name="email"
                     label="E-mail (optionnel)"
                     placeholder="Indiquez votre e-mail pour rester informé"
                     v-model="model.email"/>
          <FormTextarea name="description"
                        label="Description"
                        placeholder="Écrivez une description"
                        v-model="model.description"/>
          <div class="form-checkbox">
            <input type="checkbox"
                   id="feedback-with-picture"
                   class="pelorous-checkbox"
                   v-model="model.withPicture" />
            <label for="feedback-with-picture"></label>
            <label for="feedback-with-picture" class="real-label">Inclure une copie d'écran</label>
          </div>

        </div>
      </div>
    </div>
    <FisholaFooter shortcuts="back,credits,feedback"
                   button-icon="icon-send"
                   button-text="Envoyer"
                   v-on:buttonClicked="sendClicked"
                   back-event="onBackButton"
                   v-on:onBackButton="closeFeedback"
                   selected="feedback" />
  </div>
</template>

<script lang="ts">

import FisholaHeader from '@/components/layout/FisholaHeader.vue'
import FisholaFooter from '@/components/layout/FisholaFooter.vue'

import FormInput from '@/components/common/FormInput.vue'
import FormRadio from '@/components/common/FormRadio.vue'
import FormTextarea from '@/components/common/FormTextarea.vue'

import FeedbackBean from '@/pojos/FeedbackBean.ts'
import UserProfile from '@/pojos/UserProfile.ts';

import ProfileService from '@/services/ProfileService';

import html2canvas from 'html2canvas';

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  components: {
    FisholaHeader,
    FisholaFooter,
    FormInput,
    FormRadio,
    FormTextarea
  }
})
export default class Feedback extends Vue {

  display = false;

  model:FeedbackBean = {categoryId: 'BUG', withPicture: false};

  categories:any[] = [
    {id:'BUG', name:'Bug'},
    {id:'ERGO', name:'Ergonomie'},
    {id:'OTHER', name:'Autre'}
  ];

  created() {
  }

  mounted() {
    this.$root.$on('open-feedback', this.openFeedback);
    this.$root.$on('close-feedback', this.closeFeedback);
  }

  beforeDestroy() {
    this.$root.$off('open-feedback');
    this.$root.$off('close-feedback');
  }

  openFeedback() {
    this.model = { categoryId: 'BUG', withPicture: false };
    this.loadProfile();
  }

  loadProfile() {
    ProfileService.getProfile()
      .then(this.profileLoaded, () => this.display = true);
  }

  profileLoaded(profile:UserProfile) {
    this.model.email = profile.email;
    this.display = true;
  }

  closeFeedback() {
    this.display = false;
  }

  sendClicked() {

    this.closeFeedback();

    if (this.model.withPicture) {
      html2canvas(document.querySelector("#root"))
        .then((canvas:any) => {
          let pngPicture = canvas.toDataURL("image/png")
          this.model.picture = pngPicture;
          this.sendFeedback();
        });
    } else {
      this.sendFeedback();
    }

  }

  sendFeedback() {
    ProfileService.sendFeedback(this.model)
      .then(() => {
        this.$root.$emit('toaster-success', 'Votre retour a été enregistré, merci');
      });
  }

}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.feedback {
  position: absolute;
  top: 50px;
  left: 0px;
  z-index: 50;
  background-color: transparent;
  width: 100%;
  height: calc(100% - 50px);
  padding-top: 0px;

  &.feedback-hidden {
    display: none;
  }

  .page {
    height: calc(100% - @footer-height);

    .pane {
      margin-top: 0px;

      .pane-content {

        p {
          color: @gunmetal;
          font-size: 12px;
          line-height: 16px;
          text-align: center;
        }

        .form-checkbox {
          width: 100%;

          .real-label {
            margin-left: 10px;
          }
        }
      }
    }

  }
}

</style>
