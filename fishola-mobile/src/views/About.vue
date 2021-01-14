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
<!-- \\ Begin Holder \\ -->
<div class="DesignHolder" v-on:scroll="scrolled" id="about-scroll-container">
  <!-- \\ Begin Frame \\ -->
  <div class="LayoutFrame" id="top">
        <!-- \\ Begin Header \\ -->
        <header class="smaller">
          <div class="site-logo">
            <h1><a href="#/about" v-scroll-to="{el: '#top', container: '#about-scroll-container'}"><img src="/img/logo/logo-ligne-positif.svg" alt="FISHOLA"/></a></h1>
          </div>
          <nav class="Navigation">
            <ul>
              <li v-bind:class="activeSection=='presentation'?'active':''">
                <a href="#/about" v-scroll-to="{el: '#presentation', container: '#about-scroll-container'}">Présentation</a>
                <span class="menu-item-bg"></span>
              </li>
              <li v-bind:class="activeSection=='contribute'?'active':''">
                <a href="#/about" v-scroll-to="{el: '#contribute', container: '#about-scroll-container'}">Comment participer ?</a>
                <span class="menu-item-bg"></span>
              </li>
              <li v-bind:class="activeSection=='contact'?'active':''">
                <a href="#/about" v-scroll-to="{el: '#contact', container: '#about-scroll-container'}">Contact</a>
                <span class="menu-item-bg"></span>
              </li>
              <li>
                <a href="#/faq">FAQ</a>
                <span class="menu-item-bg"></span>
              </li>
              <li>
                <a href="#/news">Actualités</a>
                <span class="menu-item-bg"></span>
              </li>
            </ul>
          </nav>
          <div class="authentication">
            <button v-on:click="goToLogin">Connexion</button>
          </div>
        </header>
        <!-- // End Header // -->

        <div class="under-header-zone">
          FISHOLA : La science tous ensemble
        </div>

        <!-- \\ Begin Banner Section \\ -->
        <div class="Title_sec" id="title">
            <!--  \\ Begin banner Side -->
            <div class="title-banner">
              <div class="main-title">
                La science tous ensemble
              </div>
              <div class="sub-title">
                En utilisant FISHOLA vous participez à la compréhension et la préservation des lacs
              </div>
            </div>
        </div>
        <!-- // End Banner Section // -->

        <!-- \\ Begin Banner Section \\ -->
        <div class="Banner_sec" id="presentation">
          <!--  \\ Begin banner Side -->
          <div class="left-panel">
            <h3><span>FISHOLA</span></h3>
            <p>{{titleText}}</p>
            <!-- <a href="#about">MORE DETAILS</a> -->
          </div>
          <div class="right-panel">
            <img src="/img/dashboard.png" alt="Tableau de bord" />
            <img src="/img/trip.png" alt="Sortie" />
            <!-- <img src="/img/trips.png" alt="Liste des sorties" /> -->
          </div>
        </div>
        <!-- // End Banner Section // -->

        <!-- \\ Begin Video Section \\ -->
        <div class="Video_sec">
          <iframe class="youtube"
                  width="640"
                  height="360"
                  src="https://www.youtube-nocookie.com/embed/HNl9-I-Wqcg"
                  frameborder="10"
                  allow="encrypted-media"
                  allowfullscreen></iframe>
        </div>
        <!-- // End Video Section // -->

        <!-- \\ Begin Container \\ -->
        <div id="Container">
            <!-- \\ Begin About Section \\ -->
            <div class="About_sec" id="about">
                <div class="Center">
                  <div class="key-figures" v-observe-visibility="keyFiguresItemsVisibilityChanged">
                    <div class="kf-item">
                      <div class="kf-number">
                        <Counter :n="tripsCount"></Counter>
                      </div>
                      <div class="kf-label">
                        sorties
                      </div>
                    </div>
                    <div class="kf-item">
                      <div class="kf-number">
                        <Counter :n="catchsCount"></Counter>
                      </div>
                      <div class="kf-label">
                        captures
                      </div>
                    </div>
                    <div class="kf-item">
                      <div class="kf-number">
                        <Counter :n="picturesCount"></Counter>
                      </div>
                      <div class="kf-label">
                        photos
                      </div>
                    </div>
                  </div>
                    <!-- <div class="Line"></div> -->
                    <div style="height: 600px; width: 100%" class="map">
                      <l-map
                        :zoom="9"
                        :center="center"
                        :options="{
                          zoomSnap: 0.5
                        }"
                        style="height: 100%; width: 100%;"
                      >
                        <l-tile-layer
                          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                          attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
                        />

                        <l-marker v-for="l in lakes" v-bind:key="l.id" :lat-lng="asLatLng(l)">
                          <l-popup>
                            <div >
                              {{l.name}}
                              <span v-if="catchsCountPerLakeId[l.id]">
                               : {{catchsCountPerLakeId[l.id]}} captures
                              </span>
                            </div>
                          </l-popup>
                        </l-marker>

                      </l-map>
                    </div>
                    <!-- // End Tab Side // -->
                </div>
            </div>
            <!-- // End About Section // -->
        <!-- \\ Begin Contribute Section \\ -->
        <div class="Contribute_sec" id="contribute">
            <div class="Center">
                <h2>Comment participer ?</h2>
                <div class="contribute-editable" v-html="contributeText"></div>
            </div>                
        </div>
        <!-- // End Contribute Section // -->

        <!-- \\ Begin Pricing Section \\ -->
        <div class="Pricing_sec" id="pricing">
            <div class="Center">
                <!-- // End Pricing Side // -->
                <h3>Applications</h3>
                <div class="welcome-apps">
                    <a href="https://play.google.com/store/apps/details?id=fr.inrae.fishola" target="_blank"><img class="app" src="/img/GooglePlay.png" alt="GooglePlay" /></a>
                    <a href="https://apps.apple.com/fr/app/fishola/id1521226635" target="_blank"><img class="app" src="/img/AppStore.png" alt="AppStore" /></a>
                </div>
            </div>
        </div>
        <!-- // End Pricing Section // -->
        <!-- \\ Begin Contact Section \\ -->
        <div class="Contact_sec" id="contact">
            <div class="Contactside">

            </div>
                <!-- \\ Begin Get Section \\ -->
                <div class="Get_sec">
                    <div class="Center">
                        <h2>Nous contacter</h2>
                        <p>La communication et l'échange sont au c&oelig;ur de l'application FISHOLA. Vous pouvez nous suivre sur les différents réseaux sociaux ou nous contacter par l'intermédiaire du formulaire ci-dessous.</p>
                        <div class="Line"></div>
                    </div>  
                    <div class="Mid">          
                        <!-- \\ Begin Left Side \\ -->
                        <div class="Leftside">
                            <form action="#">
                                <fieldset>
                                    <p><input type="email" v-model="contactEmail" placeholder="Votre e-mail" class="field"></p>
                                    <p><textarea cols="2"  rows="4" v-model="contactMessage"  placeholder="Votre message"></textarea></p>
                                    <p><input type="button" value="Envoyer" class="button" v-on:click="sendContact"></p>
                                </fieldset>
                            </form>
                        </div>
                        <!-- // End Left Side // -->
                        <!-- \\ Begin Right Side \\ -->
                        <div class="Rightside">
                            <h3>Contactez-nous !</h3>
                            <p>Une suggestion, une question ou une envie de démarrer un projet avec nous, n'hésitez pas à nous contacter !</p>
                                <address>
                                    UMR CARRTEL (INRAE - USMB)<br/>
                                    75bis Avenue de Corzent<br/>
                                    74200 Thonon-les-Bains (France)
                                </address>  
                                <address class="Number">
                                    +33 (0)4 50 26 78 00
                                </address>  
                                <div class="clear"></div>
                                <p>Retrouvez-nous sur :</p>
                                <ul>
                                    <li><a rel="nofollow" href="https://www.facebook.com/Fishola-103641538377918" target="_blank"><img src="/img/facebook-icn.png" alt="Facebook"></a></li>
                                    <li><a rel="nofollow" href="https://twitter.com/FisholaFr" target="_blank"><img src="/img/twitter-icn.png" alt="Twitter"></a></li>
                                    <li><a rel="nofollow" href="https://www.youtube.com/UmrCarrtel" target="_blank"><img src="/img/youtube-icn.png" alt="Youtube"></a></li>
                                </ul>
                        </div>
                        <!-- // End Right Side // -->
                    </div>

                </div>
                <!-- // End Get Section // -->
            
            </div>
            <!-- // End Contact Section // -->


            <!-- \\ Begin Producted_by Section \\ -->
            <div class="Producted_by_sec" id="producted_by">
              <div class="partners">
                <h3>Nos partenaires</h3>
                <div class="credits-logos">
                  <a href="https://www.annecylacpeche.com" target="_blank"><img src='/img/credits/88b994/ALP.png' alt="AAPPMA - Annecy Lac Pêche"/></a>
                  <a href="https://www.peche-leman-apallf.com" target="_blank"><img src='/img/credits/88b994/APALLF.png' alt="APALLF"/></a>
                  <a href="https://fipal.ch" target="_blank"><img src='/img/credits/88b994/FIPAL.jpg' alt="Fédération Internationale des Pêcheurs Amateurs du Léman"/></a>
                  <a href="http://www.pecheurs-chamberiens.fr" target="_blank"><img src='/img/credits/88b994/AAPPMA-pecheurs-chamberiens.png' alt="AAPPMA - Pêcheurs chambériens"/></a>
                  <a href="https://aappma-aix-les-bains.fr" target="_blank"><img src='/img/credits/88b994/AAPPMA-Bourget.png' alt="AAPPMA - Aix-les-Bains"/></a>
                  <a href="http://www.aappma-aiguebelette.org" target="_blank"><img src='/img/credits/88b994/AAPPMA-Aiguebelette.jpg' alt="AAPPMA - Aiguebelette"/></a>
                </div>
              </div>
              <div class="Line"></div>
              <div class="producers">
                <h3>FISHOLA est produit par</h3>
                <div class="credits-logos">
                  <a href="https://www6.lyon-grenoble.inrae.fr/carrtel" target="_blank"><img src='/img/credits/88b994/CARRTEL.png' alt="CARRTEL"/></a>
                  <a href="https://www.inrae.fr" target="_blank"><img src='/img/credits/inrae.svg' alt="INRAE"/></a>
                  <a href="https://www.univ-smb.fr" target="_blank"><img src='/img/credits/88b994/USMB.png' alt="USMB"/></a>
                  <a href="https://professionnels.ofb.fr/fr/pole-ecla-ecosystemes-lacustres" target="_blank"><img src='/img/credits/88b994/ECLA.png' alt="ECLA"/></a>
                  <a href="https://ofb.gouv.fr" target="_blank"><img src='/img/credits/ofb.png' alt="OFB"/></a>
                  <a href="https://si-ola.inra.fr" target="_blank"><img src='/img/credits/88b994/OLA.png' alt="OLA"/></a>
                  <a href="https://www.codelutin.com" target="_blank"><img src='/img/credits/code-lutin.svg'/></a>
                  <a href="https://www.cisalb.fr" target="_blank"><img src='/img/credits/88b994/CISALB.png' alt="CISALB"/></a>
                </div>
              </div>
            </div>
            <!-- // End Producted_by Section // -->

            <!-- \\ Begin Footer \\-->
            <footer>
                <div class="Cntr">
                    <p>COPYRIGHT © 2021 UMR CARRTEL (INRAE - USMB) - Pôle ECLA</p>
                </div>
            </footer>
            <!-- // End Footer // -->
        </div>
        <!-- // End Container // -->
  </div>
  <!-- // End Layout Frame // -->

</div>
</template>

<script lang="ts">

import Constants from '@/services/Constants';
import Counter from '@/components/common/Counter.vue'
import ProfileService from '@/services/ProfileService';
import AboutService from '@/services/AboutService';
import FeedbackService from '@/services/FeedbackService';
import router from '@/router';

import { latLng, LatLng, Icon } from 'leaflet';

type D = Icon.Default & {
  _getIconUrl?: string;
};

delete (Icon.Default.prototype as D)._getIconUrl;

Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

import { LMap, LTileLayer, LMarker, LPopup, LTooltip } from 'vue2-leaflet';

import { Component, Prop, Vue } from 'vue-property-decorator';
import { Lake, Feedback } from '@/pojos/BackendPojos';

@Component({
  components: {
    LMap,
    LTileLayer,
    LMarker,
    LPopup,
    Counter
  }
})
export default class AboutView extends Vue {

  center = latLng(46.071623, 5.890511);

  titleText:string = "est l'application smartphone pour une gestion durable de la pêche sur les lacs alpins (Léman, lac d’Annecy, du Bourget et d’Aiguebelette).";
  contributeText:string = "<p>Le plus simple est de télécharger l'application et de l'utiliser pour saisir vos captures.</p>";
  tripsCount:number = 0;
  realTripsCount:number = 125;
  catchsCount:number = 0;
  realCatchsCount:number = 633;
  picturesCount:number = 0;
  realPicturesCount:number = 72;
  lakes:Lake[] = [];
  catchsCountPerLakeId: { [index: string]: number } = {};

  contactEmail:string = '';
  contactMessage:string = '';

  // version:string = process.env.VUE_APP_VERSION;
  projectVersion:string = process.env.VUE_APP_PROJECT_VERSION;
  gitRevision:string = process.env.VUE_APP_GIT_REVISION;
  frontendVersion:string = `${this.projectVersion} (${this.gitRevision})`;

  constructor() {
    super();
  }

  created() {

    AboutService.getKeyFigures()
    .then(
      (kf) => {
        console.debug("Chiffres clés", kf);
        this.realTripsCount = kf.tripsCount;
        this.realCatchsCount = kf.catchsCount;
        this.realPicturesCount = kf.picturesCount;
        this.titleText = kf.titleText;
        this.contributeText = kf.contributeText;
        kf.lakes.forEach((l) => this.lakes.push(l));
        this.catchsCountPerLakeId = kf.catchsCountPerLakeId;
      },
      (error) => {
        // On a pas pu récupérer les informations du back, on ajoute quand même les marqueurs sur la carte
        this.lakes.push({id:'leman', name:"Léman", exportAs:'whatever', latitude:46.439783, longitude:6.480641});
        this.lakes.push({id:'bourget', name:"Lac du Bourget", exportAs:'whatever', latitude:45.7249, longitude:5.8684});
        this.lakes.push({id:'annecy', name:"Lac d'Annecy", exportAs:'whatever', latitude:45.856166, longitude:6.173468});
        this.lakes.push({id:'aiguebelette', name:"Lac dAiguebelette", exportAs:'whatever', latitude:45.5508, longitude:5.8015});
      }
    );
  }

  asLatLng(lake:Lake):LatLng {
    let result = latLng(lake.latitude, lake.longitude);
    return result;
  }

  sendContact() {
    if (!this.contactEmail) {
      this.$root.$emit('toaster-error', 'L\'email est obligatoire');
      return;
    }
    if (!this.contactMessage) {
      this.$root.$emit('toaster-error', 'Le message est obligatoire');
      return;
    }

    let feedback:Feedback = { 
      id: '' + new Date().getTime(),
      category: 'CONTACT',
      date: new Date(),
      frontendVersion: this.frontendVersion,
      email: this.contactEmail,
      description: this.contactMessage
    };

    FeedbackService.sendFeedbackNoAsync(feedback)
      .then(
        () => {
          this.$root.$emit('toaster-success', 'Votre message a bien été envoyé à l\'équipe projet, merci', 10000);
        },
        (error) => {
          this.$root.$emit('toaster-error', 'Une erreur est survenue, merci de réessayer ultérieurement');
        });
  }

  sectionIds:string[] = ['presentation', 'contribute', 'contact'];
  activeSection:string = '';
  refreshSelectedSectionTimer:any = undefined;

  scrolled() {
    if (this.refreshSelectedSectionTimer) {
      this.refreshSelectedSectionTimer.cancel();
    }
    this.refreshSelectedSectionTimer = Vue.lodash.debounce(this.refreshSelectedSection, 100);
    this.refreshSelectedSectionTimer();
  }

  refreshSelectedSection() {
    const elem = document.getElementById('about-scroll-container');
    if (elem) {
      let foundSection:string = '';
      this.sectionIds.forEach((sectionId) => {
        const sectionElem = document.getElementById(sectionId);
        if (sectionElem && elem.scrollTop >= (sectionElem.offsetTop - 73)) { // 73 étant la hauteur du header
          foundSection = sectionId;
        }
      });
      this.activeSection = foundSection;
    }
  }

  keyFiguresItemsVisibilityChanged (isVisible:boolean) {
    if (isVisible) {
      this.tripsCount = this.realTripsCount;
      this.catchsCount = this.realCatchsCount;
      this.picturesCount = this.realPicturesCount;
    }
  }

  goToLogin () {
    ProfileService.fetchProfile()
      .then(
        (profile) => {
          router.push('/trips');
        },
        (status) => {
          router.push('/login');
        }
      );
  }
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/libs/473-november";
@import "../less/libs/473-november-media";
@import "../less/_colors";
@import "../less/_responsive";
@import url("~leaflet/dist/leaflet.css");

#about-scroll-container {
  overflow: auto;
  height: 100%;
  overflow-x: hidden;
}
.DesignHolder {
  background-color: @white-smoke !important;
}

.LayoutFrame header {
  z-index: 998; // Juste en dessous du toaster
  background: @white;

  padding-left: 20px;
  padding-right: 20px;

  &.smaller {
    height: 73px;

    display: flex;
    flex-direction: row;
    justify-content: space-between;

    .site-logo {
      @media only screen and (max-width: 1023px) {
        width: 150px;
      }
    }

    .Navigation {
      width: unset;
      float: initial;

      li { height: 73px; }

      li a {
        padding: 22px 30px; 

        @media only screen and (min-width: 1024px) and (max-width: 1200px) {
          padding: 22px 20px; 
        }
        @media only screen and (max-width: 1023px) {
          padding: 22px 10px;
          font-size: 14px;
        }
      }

    }

    .authentication {
      height: 100%;

      display: flex;
      flex-direction: column;
      justify-content: center;

      button {
        height: 41px;
        border-radius: 20px;

        font-style: normal;
        font-weight: bold;
        font-size: 18px;
        line-height: 25px;

        color: @white;
        background-color: @terra-cotta;

        border: 0px;
        padding-left: @margin-medium;
        padding-right: @margin-medium;


        @media only screen and (max-width: 1023px) {
          height: 31px;
          font-size: 14px;
        }
      }
    }

  }
}

.Navigation li a {
  color: @gunmetal;
}
.Navigation li span {
	background: @pelorous;
}
.Navigation li:hover span, .Navigation li.active span {
  opacity: 1;
}
.Navigation li:hover a, .Navigation li.active a {
  color: @white;
}

.under-header-zone {
  height: 73px;
  text-align: center;
  color: @gunmetal;
  font-size: 48px;
}

.Title_sec {
	width: 100%;
	background: url(/img/about-coregones5.jpg) top center no-repeat;
	height: 680px;
	position: relative;
  background-size: cover;
  
  .title-banner {
    color: @white;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    .main-title {
      font-weight: bold;
      font-size: 64px;
      line-height: 87px;
      margin: 20px;
    }
    .sub-title {
      font-size: 22px;
      line-height: 30px;
      margin: 20px;
    }
  }
}

.Banner_sec {
  height: 620px;
  background: @white-smoke;
  display: flex;
  flex-direction: row;
  justify-content: space-evenly;
  align-items: center;
  padding: 20px;

  .left-panel {
    width:  25%;
    h3 {
      font-size: 42px;
      color: @white;
      font-weight: 800;
      text-transform: uppercase;
      margin: 0px;
      line-height: 34px;

      span {
        color: @terra-cotta;
        font-weight: 600;
        display: block;
      }
    }
    p {
      padding: 16px 0px 26px 0px;
      font-size: 22px;
      color: @gunmetal;
      font-weight: 400;
      font-style: italic;
      margin: 0px;
      word-spacing: 2px;
      line-height: 22px;
    }

  }
  .right-panel {
    width: 50%;

    display: flex;
    flex-direction: row;
    justify-content: space-evenly;
    align-items: center;

    img {
      border-radius: 20px;
    }
  }
}

.Video_sec {
  width: 100%;
  background: @pelorous;
  height: 360px;
  display: flex;
  flex-direction: row;
  justify-content: center;
}

.About_sec {
  padding-top: 40px;
  .key-figures {
    width: 100%;
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    padding-top: 30px;
    padding-bottom: 30px;

    .kf-item {
      width: 33%;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      margin-left: 50px;
      margin-right: 50px;
      .kf-number {
        color: @terra-cotta;
        font-size: 50px;
        font-weight: 530;
        height: 59px;
      }
      .kf-label {
        color: @gunmetal;
        font-size: 22px;
        font-weight: 400;
      }
    }
  }
  .map {
    margin-top: 30px;
  }
}
.Contribute_sec{ background: url(/img/about-background.jpg) top center no-repeat; background-size: cover; }
.Get_sec { background: url(/img/about-background.jpg) top center no-repeat; background-size: cover; }

.Contribute_sec p,
.Contact_sec p {
  font-size: 18px;
}
.Pricing_sec h3,
.About_sec h3 {
  color: @gunmetal;
}

.Get_sec .Leftside textarea,
.Get_sec .Leftside input.field {
  color: @gunmetal;
}

.Get_sec .Leftside textarea {
  height: 205px;
}

.Get_sec .Rightside {
  padding-top: 2px;
  h3 {
    padding-bottom: 5px;
  }
  p {
    padding: 10px 0px 10px 0px;
    font-size: 16px;
  }
  ul {
    padding-top: 0px;
  }
}

.welcome-apps {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  margin-top: 50px;
  img {
    width: 200px;
    margin-left: 20px;
    margin-right: 20px;
  }
}

.Producted_by_sec {

  background-color: @solitude;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding-top: 30px;
  padding-bottom: 30px;

  .partners, .producers {
    h3 {
      text-align: center;
      font-weight: lighter;
      color: @pelorous;
    }

    .credits-logos {
      display: flex;
      flex-direction: row;
      justify-content: center;
      align-items: center;
      flex-wrap: wrap;

      img {
        margin-left: 20px;
        margin-right: 20px;
      }
    }
  }

  .partners {
    h3 {
      font-size: 42px;
    }
    img {
      max-height: 140px;
      max-width: 240px;
    }
  }

  .producers {
    h3 {
      font-size: 24px;
    }
    img {
      min-height: 50px;
      min-width: 90px;
      max-height: 90px;
      max-width: 150px;
    }
  }

  .Line { border: solid 1px #E17055; height: 2px; width: 252px; margin: 30px; }

}

.contribute-editable {
  margin-top: 50px;

  /** /deep/ permet de contourner le scoped qui empêche le code injecté de bénéficier du style */
  /deep/ .cards {
    display: flex;
    flex-direction: row;
    justify-content: center;
    width: 100%;

    .card {
      height: fit-content;
      width: 350px;
      margin: 15px;
      padding: 30px;
      border-radius: 15px;
      background-color: @white-smoke-alpha-20;
      box-shadow: 5px 5px 5px @gunmetal;

      h4 {
        margin: 5px;
      }

      p {
        text-align: left;
      }

      &:hover {
        background-color: @white-smoke;
        color: @gunmetal;
        // cursor: pointer;
      }
    }
  }

}

footer {
  background: #0c2b34;
}

</style>
