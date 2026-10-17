<!--
  #%L
  Carnet de pêche :: Mobile
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
  <div class="DesignHolder force-light-theme" v-on:scroll="scrolled" id="about-scroll-container">
    <!-- \\ Begin Holder \\ -->
    <!-- Page vitrine : reste en thème clair (#208), ses styles sont figés. -->
    <!-- \\ Begin Frame \\ -->
    <div class="LayoutFrame" id="top">
      <!-- \\ Begin Header \\ -->
      <header class="smaller" v-if="!wrappedInTab">
        <div class="site-logo site-logo-smartphone">
          <h1>
            <a href="#/about" @click.prevent="scrollToSection('top')"><CarnetLogo class="about-logo" /></a>
          </h1>
        </div>

        <div class="burger" v-if="closed" @click="closed = !closed">
          <i class="icon-menu"></i>
        </div>

        <div class="menu" :class="{ closed: closed }">
          <div class="site-logo site-logo-desktop">
            <h1>
              <a href="#/about" @click.prevent="scrollToSection('top'); closed = true"><CarnetLogo class="about-logo" /></a>
            </h1>
          </div>

          <nav class="Navigation">
            <ul>
              <li class="close hide-on-desktop">
                <div class="plus" v-on:click="closeMenu">+</div>
              </li>
              <li v-bind:class="activeSection == 'presentation' ? 'active' : ''">
                <a href="#/about" @click.prevent="scrollToSection('presentation'); closed = !closed">Présentation</a>
                <span class="menu-item-bg"></span>
              </li>
              <li v-bind:class="activeSection == 'communications' ? 'active' : ''">
                <a href="#/about" @click.prevent="scrollToSection('communications'); closed = !closed">Communications</a>
                <span class="menu-item-bg"></span>
              </li>
              <li v-bind:class="activeSection == 'contribute' ? 'active' : ''">
                <a href="#/about" @click.prevent="scrollToSection('contribute'); closed = !closed">Comment participer ?</a>
                <span class="menu-item-bg"></span>
              </li>
              <li v-bind:class="activeSection == 'contact' ? 'active' : ''">
                <a href="#/about" @click.prevent="scrollToSection('contact'); closed = !closed">Contact</a>
                <span class="menu-item-bg"></span>
              </li>
            </ul>
          </nav>
          <div class="authentication">
            <button v-on:click="goToLogin">Connexion</button>
          </div>
        </div>
      </header>
      <!-- // End Header // -->

      <!-- \\ Begin Banner Section \\ -->
      <div class="Title_sec" id="title" v-if="!wrappedInTab">
        <!--  \\ Begin banner Side -->
        <div class="title-banner">
          <div class="main-title">Sentinelles des cours d'eau</div>
          <div class="sub-title">
            Avec Carnet de pêche, chaque sortie aide à préserver les rivières
          </div>
        </div>
      </div>
      <!-- // End Banner Section // -->

      <!-- \\ Begin Presentation Section \\ -->
      <div class="Presentation_sec" id="presentation">
        <div class="Center">
          <h3><span>Carnet de pêche</span></h3>
          <h2>Un projet de science citoyenne</h2>
          <p>
            Les données anonymisées de vos prises et de vos parties de pêche servent à prendre des
            mesures de préservation des cours d'eau, pour une pêche en rivière durable et abondante.
          </p>
          <p>
            Le pêcheur qui utilise l'application est une véritable sentinelle des cours d'eau.
          </p>
        </div>
      </div>
      <!-- // End Presentation Section // -->

      <!-- \\ Begin Container \\ -->
      <div id="Container">
        <!-- \\ Begin About Section \\ -->
        <div class="About_sec" id="about">
          <div class="Center">
            <div class="key-figures" ref="keyFigures">
              <div class="kf-item">
                <div class="kf-number">
                  <Counter :n="tripsCount"></Counter>
                </div>
                <div class="kf-label">sorties</div>
              </div>
              <div class="kf-item">
                <div class="kf-number">
                  <Counter :n="catchsCount"></Counter>
                </div>
                <div class="kf-label">captures</div>
              </div>
              <div class="kf-item">
                <div class="kf-number">
                  <Counter :n="picturesCount"></Counter>
                </div>
                <div class="kf-label">photos</div>
              </div>
            </div>
            <div class="map">
              <MapLibreMarkersMap :markers="mapMarkers" :center="center" :zoom="9" />
            </div>
            <!-- // End Tab Side // -->
          </div>
        </div>
        <!-- // End About Section // -->
        <!-- \\ Begin Communications Section \\ -->
        <div class="Communications_sec" id="communications" v-if="!wrappedInTab">
          <div class="Center">
            <h3>Communications</h3>
          </div>
          <CommunicationsOnAboutPage :news="news" />
        </div>
        <!-- // End Contribute Section // -->

        <!-- \\ Begin Contribute Section \\ -->
        <div class="Contribute_sec" id="contribute">
          <div class="Center">
            <h2>Comment participer ?</h2>
            <div class="contribute-editable" v-html="contributeText"></div>
            <div class="consult-faq">
              Pour plus d'informations
              <div class="consult-faq-button" @click="goFaq">
                Consulter la FAQ
              </div>
            </div>
          </div>
        </div>
        <!-- // End Contribute Section // -->

        <!-- \\ Begin Contact Section \\ -->
        <div class="Contact_sec" id="contact">
          <div class="Contactside"></div>
          <!-- \\ Begin Get Section \\ -->
          <div class="Get_sec">
            <div class="Center">
              <h2>Nous contacter</h2>
              <p>
                Une suggestion, une question sur l'application ? Écrivez-nous
                par l'intermédiaire du formulaire ci-dessous.
              </p>
              <div class="Line"></div>
            </div>
            <div class="Mid">
              <!-- \\ Begin Left Side \\ -->
              <div class="Leftside">
                <form action="#">
                  <fieldset>
                    <legend style="display:none">Formulaire de contact</legend>
                    <p>
                      <input type="email" v-model="contactEmail" placeholder="Votre e-mail" class="field noshadow" />
                    </p>
                    <p>
                      <textarea cols="2" rows="4" v-model="contactMessage" placeholder="Votre message"
                        class="noshadow"></textarea>
                    </p>
                    <p>
                      <input type="button" value="Envoyer" class="button noshadow send-button"
                        v-on:click="sendContact" />
                    </p>
                  </fieldset>
                </form>
              </div>
              <!-- // End Left Side // -->
            </div>
          </div>
          <!-- // End Get Section // -->
        </div>
        <!-- // End Contact Section // -->

        <!-- \\ Begin Producted_by Section \\ -->
        <div class="Producted_by_sec" id="producted_by">
          <div class="producers">
            <h3>Porteur du projet</h3>
            <p>Carnet de pêche est produit par l'Union des Bassins Rhône-Méditerranée-Corse (UFBRMC).</p>
            <div class="credits-logos">
              <a href="https://www.peche-auvergne-rhonealpes.fr/actualite/3754/1913-union-des-bassins-rhone-mediterranee-corse.htm"
                target="_blank" rel="noopener"><img class="logo-ufbrmc" src="/img/logo/ufbrmc.jpg"
                  alt="Union des Bassins Rhône-Méditerranée-Corse" /></a>
            </div>
          </div>
          <div class="Line"></div>
          <div class="producers">
            <h3>Développement</h3>
            <p>Application développée par Symbiome.</p>
            <div class="credits-logos">
              <a href="https://symbiome.eu" target="_blank" rel="noopener"><img class="logo-symbiome"
                  src="/img/logo/symbiome.png" alt="Symbiome" /></a>
            </div>
          </div>
        </div>
        <!-- // End Producted_by Section // -->

        <!-- \\ Begin Footer \\-->
        <footer>
          <div class="Cntr">
            <p>© Union des Bassins Rhône-Méditerranée-Corse · Application développée par Symbiome</p>
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
import Counter from "@/components/common/Counter.vue";
import ProfileService from "@/services/ProfileService";
import AboutService from "@/services/AboutService";
import FeedbackService from "@/services/FeedbackService";
import { RouterUtils } from "@/router/RouterUtils";

import MapLibreMarkersMap, { MapMarker } from "@/components/common/MapLibreMarkersMap.vue";

import { Component, Prop, Vue } from "vue-facing-decorator";
import debounce from "lodash/debounce";
import EventBus from "@/services/EventBus";
import { WaterEntity as Lake, Feedback, NewsBean } from "@/pojos/BackendPojos";
import DocumentationService from "../services/DocumentationService";
import CommunicationsOnAboutPage from "./CommunicationsOnAboutPage.vue";
import CarnetLogo from "@/components/layout/CarnetLogo.vue";

@Component({
  components: {
    MapLibreMarkersMap,
    Counter,
    CommunicationsOnAboutPage,
    CarnetLogo,
  },
})
export default class AboutView extends Vue {
  @Prop({ default: false }) wrappedInTab: boolean;
  // Centre par défaut [lng, lat] (recadré sur les marqueurs s'il y en a).
  center: [number, number] = [5.890511, 46.071623];

  contributeText: string =
    "<p>Le plus simple est de télécharger l'application et de l'utiliser pour saisir vos captures.</p>";
  tripsCount: number = 0;
  realTripsCount: number = 442;
  catchsCount: number = 0;
  realCatchsCount: number = 1726;
  picturesCount: number = 0;
  realPicturesCount: number = 212;
  lakes: Lake[] = [];
  news: NewsBean[] = [];
  catchsCountPerLakeId: { [index: string]: number } = {};

  contactEmail: string = "";
  contactMessage: string = "";

  projectVersion: string = import.meta.env.VITE__PACKAGE_JSON_VERSION;
  gitRevision: string = import.meta.env.VITE__GIT_REVISION;
  frontendVersion: string = `${this.projectVersion} (${this.gitRevision})`;

  closed: boolean = true;
  keyFiguresObserver?: IntersectionObserver;

  constructor() {
    super();
  }

  created() {
    AboutService.getKeyFigures().then(
      (kf) => {
        console.debug("Chiffres clés", kf);
        this.realTripsCount = kf.tripsCount;
        this.realCatchsCount = kf.catchsCount;
        this.realPicturesCount = kf.picturesCount;
        this.contributeText = kf.contributeText;
        kf.lakes.forEach((l) => this.lakes.push(l));
        this.catchsCountPerLakeId = kf.catchsCountPerLakeId;
      },
      (_error) => {
        // On a pas pu récupérer les informations du back, on ajoute quand même les marqueurs sur la carte
        this.lakes.push({
          id: "leman",
          name: "Léman",
          exportAs: "whatever",
          latitude: 46.439783,
          longitude: 6.480641,
        });
        this.lakes.push({
          id: "bourget",
          name: "Lac du Bourget",
          exportAs: "whatever",
          latitude: 45.7249,
          longitude: 5.8684,
        });
        this.lakes.push({
          id: "annecy",
          name: "Lac d'Annecy",
          exportAs: "whatever",
          latitude: 45.856166,
          longitude: 6.173468,
        });
        this.lakes.push({
          id: "aiguebelette",
          name: "Lac dAiguebelette",
          exportAs: "whatever",
          latitude: 45.5508,
          longitude: 5.8015,
        });
      }
    );
    DocumentationService.getNews().then((zeNews) => {
      this.news = zeNews;
    });
  }

  get mapMarkers(): MapMarker[] {
    return (this.lakes || [])
      .filter((l) => l.latitude != null && l.longitude != null)
      .map((l) => {
        const name = this.escapeHtml(l.name);
        const count = this.catchsCountPerLakeId[l.id];
        const html = count ? `<div>${name} : ${count} captures</div>` : `<div>${name}</div>`;
        return { lat: l.latitude, lng: l.longitude, popupHtml: html, title: l.name };
      });
  }

  escapeHtml(value: string): string {
    return (value || "").replace(/[&<>"']/g, (c) => (
      { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c] as string
    ));
  }

  sendContact() {
    if (!this.contactEmail) {
      EventBus.emit("toaster-error", "L'email est obligatoire");
      return;
    }
    if (!this.contactMessage) {
      EventBus.emit("toaster-error", "Le message est obligatoire");
      return;
    }

    let feedback: Feedback = {
      id: "" + new Date().getTime(),
      category: "CONTACT",
      date: new Date(),
      frontendVersion: this.frontendVersion,
      email: this.contactEmail,
      description: this.contactMessage,
    };

    FeedbackService.sendFeedbackNoAsync(feedback).then(
      () => {
        EventBus.emit(
          "toaster-success",
          "Votre message a bien été envoyé à l'équipe projet, merci",
          10000
        );
      },
      (_error) => {
        EventBus.emit(
          "toaster-error",
          "Une erreur est survenue, merci de réessayer ultérieurement"
        );
      }
    );
  }

  sectionIds: string[] = [
    "presentation",
    "communications",
    "contribute",
    "contact",
  ];
  activeSection: string = "";
  refreshSelectedSectionTimer: any = undefined;

  scrolled() {
    if (this.refreshSelectedSectionTimer) {
      this.refreshSelectedSectionTimer.cancel();
    }
    this.refreshSelectedSectionTimer = debounce(
      this.refreshSelectedSection,
      100
    );
    this.refreshSelectedSectionTimer();
  }

  refreshSelectedSection() {
    const elem = document.getElementById("about-scroll-container");
    if (elem) {
      let foundSection: string = "";
      this.sectionIds.forEach((sectionId) => {
        const sectionElem = document.getElementById(sectionId);
        if (sectionElem && elem.scrollTop >= sectionElem.offsetTop - 73) {
          // 73 étant la hauteur du header
          foundSection = sectionId;
        }
      });
      this.activeSection = foundSection;
    }
  }

  mounted() {
    const keyFigures = this.$refs.keyFigures as HTMLElement | undefined;
    if (keyFigures) {
      this.keyFiguresObserver = new IntersectionObserver((entries) =>
        this.keyFiguresItemsVisibilityChanged(entries.some((entry) => entry.isIntersecting))
      );
      this.keyFiguresObserver.observe(keyFigures);
    }
  }

  beforeUnmount() {
    this.keyFiguresObserver?.disconnect();
  }

  scrollToSection(sectionId: string) {
    document.getElementById(sectionId)?.scrollIntoView({ behavior: "smooth" });
  }

  keyFiguresItemsVisibilityChanged(isVisible: boolean) {
    if (isVisible) {
      this.tripsCount = this.realTripsCount;
      this.catchsCount = this.realCatchsCount;
      this.picturesCount = this.realPicturesCount;
    }
  }

  goToLogin() {
    this.closed = true;
    ProfileService.fetchProfile().then(
      (_profile) => {
        RouterUtils.pushRouteNoDuplicate(this.$router, RouterUtils.homeRoute());
      },
      (_status) => {
        RouterUtils.pushRouteNoDuplicate(this.$router, "/login");
      }
    );
  }

  closeMenu() {
    this.closed = true;
  }

  goFaq() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/documentation/faq");
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
@import "../less/libs/473-november";
@import "../less/libs/473-november-media";
@import "../less/_colors";
@import "../less/_responsive";

.about-logo {
  width: 150px;
}

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
    width: 100%;
    height: 73px;

    display: flex;
    flex-direction: row;
    justify-content: space-between;

    .site-logo-smartphone {
      @media only screen and (min-width: 900px) {
        display: none;
      }
    }

    .burger {
      color: @gunmetal;
      display: flex;
      flex-direction: column;
      justify-content: center;
      cursor: pointer;
      font-weight: 300;

      @media only screen and (min-width: 900px) {
        display: none;
      }
    }

    .menu {
      width: 100%;
      height: 73px;

      display: flex;
      flex-direction: row;
      justify-content: space-between;

      .site-logo-desktop {
        @media only screen and (max-width: 899px) {
          display: none;
        }

        @media only screen and (min-width: 900px) and (max-width: 999px) {
          width: 140px;
          padding-top: 20px;
        }
      }

      .Navigation {
        width: unset;
        float: initial;

        li {
          height: 73px;
        }

        li a {
          padding: 22px 30px;

          @media only screen and (min-width: 1124px) and (max-width: 1200px) {
            padding: 22px 20px;
          }

          @media only screen and (min-width: 800px) and (max-width: 1323px) {
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

      @media only screen and (max-width: 899px) {
        position: absolute;
        z-index: 2;
        top: 0;
        right: 0;
        width: 250px;
        height: 100vh;
        height: 100dvh; /* Good height for mobile screens, with navigation bar, if supported */
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        align-items: right;
        background-color: @white-smoke;

        &.closed {
          display: none;
        }

        .Navigation {
          li {
            width: 100%;
            text-align: right;
          }
        }

        .authentication {
          height: 81px;

          padding: 20px;
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

.Navigation li:hover span,
.Navigation li.active span {
  opacity: 1;
}

.Navigation li:hover a,
.Navigation li.active a {
  color: @white;
}

.Title_sec {
  width: 100%;
  background: url(img/about-coregones5.jpg) top center no-repeat;
  height: 680px;
  position: relative;
  background-size: cover;

  .title-banner {
    background: #020a3058;
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
      font-size: 26px;
      line-height: 30px;
      margin: 20px;
    }
  }
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

    @media only screen and (max-width: 530px) {
      flex-direction: column;
    }

    .kf-item {
      width: 33%;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      margin-left: 50px;
      margin-right: 50px;

      .kf-number {
        color: @accent-text;
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
    // avoid the map controls and credits to be above the header
    position: relative;
    z-index: 5;

    margin-top: 30px;
    height: 600px;

    @media only screen and (min-width: 600px) and (max-width: 899px) {
      height: 400px;
    }

    @media only screen and (max-width: 599px) {
      height: 300px;
    }
  }
}

.Contribute_sec {
  background: url(img/about-background.jpg) top center no-repeat;
  background-size: cover;
}

.Get_sec {
  background: url(img/about-background.jpg) top center no-repeat;
  background-size: cover;
}

.Contribute_sec p,
.Contact_sec p {
  font-size: 18px;
}

.About_sec h3 {
  color: @gunmetal;
}

.Communications_sec {
  background-color: @solitude;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding-top: 30px;
  padding-bottom: 30px;

  .Center {
    h3 {
      color: @accent-text;
      font-size: 57px;
      font-family: @font-family;
      font-weight: 700;
      margin: 0px;
      text-transform: uppercase;
      line-height: 60px;
      padding-bottom: 50px;

      @media only screen and (max-width: 1023px) {
        padding-bottom: 20px;
        font-size: 42px;
      }
    }
  }
}

.Get_sec .Leftside textarea,
.Get_sec .Leftside input.field {
  color: @gunmetal;
}

.Get_sec .Leftside textarea {
  height: 205px;
}

.Get_sec .Leftside {
  float: none;
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
}

.Presentation_sec {
  background: @white-smoke;
  padding: 70px 20px;
  text-align: center;

  .Center {
    max-width: 760px;
    margin: 0 auto;
  }

  h3 span {
    color: @accent-text;
    font-size: 42px;
    font-weight: 600;
    text-transform: uppercase;
  }

  h2 {
    color: @gunmetal;
    font-size: 30px;
    margin: 24px 0 16px;
  }

  p {
    color: @gunmetal;
    font-size: 20px;
    line-height: 30px;
    margin: 0 0 16px;
  }

  @media only screen and (max-width: 630px) {
    padding: 40px 16px;

    h3 span {
      font-size: 30px;
    }

    h2 {
      font-size: 24px;
    }

    p {
      font-size: 17px;
      line-height: 26px;
    }
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

  .producers {
    h3 {
      text-align: center;
      font-weight: lighter;
      color: @accent-text;
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

  .producers {
    h3 {
      font-size: 24px;
    }

    p {
      text-align: center;
      color: @gunmetal;
      margin: 0 20px 20px;
    }

    .logo-ufbrmc {
      height: 140px;
    }

    .logo-symbiome {
      height: 60px;
      max-width: 80vw;
    }
  }

  .Line {
    border: solid 1px @accent-text;
    height: 2px;
    width: 252px;
    margin: 30px;
  }
}

.contribute-editable {
  margin-top: 50px;

  /** :deep() permet de contourner le scoped qui empêche le code injecté de bénéficier du style */
  :deep(.cards) {
    display: flex;
    flex-direction: row;
    justify-content: center;
    width: 100%;

    .card {
      height: fit-content;
      max-width: 350px;
      width: 80%;
      margin: 15px;
      padding: 30px;
      min-height: 300px;
      background-color: @white-smoke-alpha-20;
      border: 1px solid @accent-text;

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

    @media only screen and (max-width: 1300px) {
      flex-direction: column;
      align-items: center;

      .card {
        height: fit-content;
        width: 80%;
        padding: 20px;

        p {
          margin-top: 5px;
          margin-bottom: 5px;
        }
      }
    }
  }
}

footer {
  background: @carnet-navy;
}

.consult-faq {
  padding-top: 25px;
  display: flex;
  gap: 10px;
  flex-direction: column;
  align-items: center;

  .consult-faq-button {
    background-color: white;
    color: @accent-text;
    border: 1px solid @accent-text;
    width: 200px;
    font-weight: bolder;
    padding: 10px;
    cursor: pointer;

    &:hover {
      color: white;
      background-color: @carnet-blue;
      border: 1px solid white;
    }
  }
}

.close {
  display: flex;
  flex-direction: row-reverse;
  align-items: center;
  padding: 22px 30px;

  height: @pastille-size;
  margin: @vertical-margin-small;
  width: 100%;
  color: @gunmetal;

  div.plus {
    font-size: @pastille-size;
    width: fit-content;
    transform: rotate(45deg);
  }
}

.noshadow {
  box-shadow: none !important;
}

.send-button {
  margin-left: 0px;
}
</style>
