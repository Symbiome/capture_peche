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
                     v-model="model.category"/>
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
                   v-model="withPicture" />
            <label for="feedback-with-picture"></label>
            <label for="feedback-with-picture" class="real-label">Inclure une copie d'écran</label>
          </div>

          <div class="bottom-page-spacer"></div>
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

import {Feedback} from '@/pojos/BackendPojos.ts';
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
export default class FeedbackModal extends Vue {

  display = false;

  model:Feedback = { category: 'BUG', id: 'FAKE' };
  withPicture:boolean = false;

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
    this.model = { category: 'BUG', id: 'FAKE' };
    this.loadProfile();
  }

  loadProfile() {
    ProfileService.getProfile()
      .then(this.profileLoaded, () => this.display = true);
  }

  profileLoaded(profile:UserProfile) {
    this.model.email = profile.email;
    this.model.userId = profile.email;
    this.display = true;
  }

  closeFeedback() {
    this.display = false;
  }

  sendClicked() {

    this.closeFeedback();

    if (this.withPicture) {
      let rootElement:HTMLElement = this.castRootElement(document.querySelector("#root"));
      html2canvas(rootElement)
        .then((canvas:any) => {
          let pngPicture = canvas.toDataURL("image/png")
          this.model.screenshot = pngPicture;
          this.sendFeedback();
        });
    } else {
      this.sendFeedback();
    }

  }

  castRootElement(whatever:any):HTMLElement {
    return whatever;
  }

  addEnvInfo() {
    this.model.browser = this.getBrowserNameAndVersion();
    this.model.os = this.getOperatingSystemNameAndVersion();
    this.model.platform = navigator.platform;
    this.model.screenResolution = screen.width + "x" + screen.height;
    this.model.displaySize = window.innerWidth + "x" + window.innerHeight;
    this.model.locale = navigator.language;
  }

  getOperatingSystemNameAndVersion() {
    let userAgent = navigator.userAgent;
    let os = userAgent;
    if (userAgent.indexOf("Windows NT 10.0")!=-1) os="Windows 10";
    if (userAgent.indexOf("Windows NT 6.3")!=-1) os="Windows 8.1";
    if (userAgent.indexOf("Windows NT 6.2")!=-1) os="Windows 8";
    if (userAgent.indexOf("Windows NT 6.1")!=-1) os="Windows 7";
    if (userAgent.indexOf("Windows NT 6.0")!=-1) os="Windows Vista";
    if (userAgent.indexOf("Windows NT 5.1")!=-1) os="Windows XP";
    if (userAgent.indexOf("Windows NT 5.0")!=-1) os="Windows 2000";
    if (userAgent.indexOf("Mac")!=-1) os="Mac/iOS";
    if (userAgent.indexOf("X11")!=-1) os="UNIX";
    if (userAgent.indexOf("Linux")!=-1) os="Linux";
    if (userAgent.indexOf("Android")!=-1) os="Android";
    return os;
  }

  getBrowserNameAndVersion() {
    var ua = navigator.userAgent, tem,
        M = ua.match(/(opera|chrome|safari|firefox|msie|trident(?=\/))\/?\s*([\d]+)/i) || [];
    if(/trident/i.test(M[1])){
      tem=  /\brv[ :]+(\d+(\.\d+)?)/g.exec(ua) || [];
      return 'IE '+(tem[1] || '');
    }
    M= M[2]? [M[1], M[2]]:[navigator.appName, navigator.appVersion, '-?'];
    if((tem= ua.match(/version\/([\d]+)/i))!= null) M[2]= tem[1];
    return M.join(' ');
  }

  sendFeedback() {
    this.addEnvInfo();
    this.model.date = new Date();
    this.model.location = window.location.href;
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

        color: @gunmetal;
        font-size: 12px;
        line-height: 16px;

        p {
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
