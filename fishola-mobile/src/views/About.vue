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
<!-- \\ Begin Holder \\ -->
<div class="DesignHolder">
  <!-- \\ Begin Frame \\ -->
  <div class="LayoutFrame">
        <!-- \\ Begin Header \\ -->
        <header class="smaller">
            <div class="Center">
                <div class="site-logo">
                  <h1><a href="#about"><img src="/img/logo/logo-ligne-positif.svg" alt="FISHOLA"/></a></h1>
                </div>
              <div id="mobile_sec">
                <div class="menumobile">
                    <!-- \\ Begin Navigation \\ -->
                    <nav class="Navigation">
                        <ul>
                            <li class="active">                                
                                <a href="#presentation">Présentation</a>
                                <span class="menu-item-bg"></span>
                            </li>
                            <li>
                                <a href="#contribute">Comment participer ?</a>
                                <span class="menu-item-bg"></span>
                            </li>
                            <li>
                                <a href="#contact">Contact</a>
                                <span class="menu-item-bg"></span>
                            </li>
                            <li>
                                <a href="#faq">FAQ</a>
                                <span class="menu-item-bg"></span>
                            </li>
                            <li>
                                <a href="#infos">Infos</a>
                                <span class="menu-item-bg"></span>
                            </li>
                        </ul>
                    </nav>
                    <!-- // End Navigation // -->
                </div>
              </div>
            <div class="clear"></div>
          </div>
        </header>
        <!-- // End Header // -->

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
            <p>est l'application smartphone pour une gestion durable de la pêche sur les lacs alpins (Léman, lac d’Annecy, du Bourget et d’Aiguebelette). Ce texte sera modifiable dans l'interface d'administration.</p>
            <!-- <a href="#about">MORE DETAILS</a> -->
          </div>
          <div class="right-panel">
            <img src="/img/dashboard.png" alt="Tableau de bord" />
          </div>
        </div>
        <!-- // End Banner Section // -->

        <!-- \\ Begin Banner Section \\ -->
        <div class="Video_sec">
          <iframe class="youtube" width="560" height="315" src="https://www.youtube-nocookie.com/embed/HNl9-I-Wqcg" frameborder="10" allow="encrypted-media" allowfullscreen></iframe>
        </div>
        <!-- // End Banner Section // -->

        <!-- \\ Begin Producted_by Section \\ -->
        <div class="Producted_by_sec" id="producted_by">
          <div class="Center">
            <h3>FISHOLA est produit par </h3>
            <div class="credits-logos">
              <a href="https://www.inrae.fr" target="_blank"><img src='/img/credits/inrae.svg'/></a>
              <a href="https://ofb.gouv.fr/" target="_blank"><img src='/img/credits/ofb.png'/></a>
              <a href="https://www.codelutin.com" target="_blank"><img src='/img/credits/code-lutin.svg'/></a>
            </div>
          </div>
        </div>
        <!-- // End Producted_by Section // -->

        <!-- \\ Begin Container \\ -->
        <div id="Container">
            <!-- \\ Begin About Section \\ -->
            <div class="About_sec" id="about">
                <div class="Center">
                  <div class="key-figures">
                    <div class="kf-item">
                      <div class="kf-number">
                        123
                      </div>
                      <div class="kf-label">
                        sorties
                      </div>
                    </div>
                    <div class="kf-item">
                      <div class="kf-number">
                        666
                      </div>
                      <div class="kf-label">
                        captures
                      </div>
                    </div>
                    <div class="kf-item">
                      <div class="kf-number">
                        42
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

                        <l-marker :lat-lng="lemanPos">
                          <l-popup>
                            <div >
                              Léman
                            </div>
                          </l-popup>
                        </l-marker>

                        <l-marker :lat-lng="annecyPos">
                          <l-popup>
                            <div >
                              Lac d'Annecy
                            </div>
                          </l-popup>
                        </l-marker>

                        <l-marker :lat-lng="bourgetPos">
                          <l-popup>
                            <div >
                              Lac du Bourget
                            </div>
                          </l-popup>
                        </l-marker>

                        <l-marker :lat-lng="aiguebelettePos">
                          <l-popup>
                            <div >
                              Lac d'Aiguebelette
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
                <p>Ce texte sera modifiable dans l'interface d'administration. Ce texte sera modifiable dans l'interface d'administration. Ce texte sera modifiable dans l'interface d'administration. Ce texte sera modifiable dans l'interface d'administration. Ce texte sera modifiable dans l'interface d'administration.</p>
                <!-- <div class="Line"></div> -->
            </div>                
        </div>
        <!-- // End Contribute Section // -->

        <!-- \\ Begin Pricing Section \\ -->
        <div class="Pricing_sec" id="pricing">
            <div class="Center">
                <!-- // End Pricing Side // -->
                <h3>Applications</h3>
                <div class="welcome-apps">
                    <a href="https://play.google.com/store/apps/details?id=fr.inrae.fishola" target="_blank"><img class="app" src="/img/473-november/GooglePlay.png" alt="GooglePlay" /></a>
                    <a href="https://apps.apple.com/fr/app/fishola/id1521226635" target="_blank"><img class="app" src="/img/473-november/AppStore.png" alt="AppStore" /></a>
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
                        <p>Sed non neque elit. Sed ut imperdiet nisi. Proin condimentum fermentum nunc. Etiam pharetra, erat sed fermentum<br>
                        feugiat velit mauris egestas quamut aliquam massa nisl quis neque. Suspendisse in orci enim.</p>
                        <div class="Line"></div>
                    </div>  
                    <div class="Mid">          
                        <!-- \\ Begin Left Side \\ -->
                        <div class="Leftside">
                            <form action="#">
                                <fieldset>
                                    <p><input type="email" value="" placeholder="Votre e-mail" class="field"></p>
                                    <p><textarea cols="2"  rows="2" placeholder="Votre message"></textarea></p>
                                    <p><input type="submit" value="Envoyer" class="button"></p>
                                </fieldset>
                            </form>
                        </div>
                        <!-- // End Left Side // -->
                        <!-- \\ Begin Right Side \\ -->
                        <div class="Rightside">
                            <h3>Get in touch !</h3>
                                <address>
                                    UMR CARRTEL<br/>
                                    75 bis Avenue de Corzent<br/>
                                    74200 Thonon les Bains
                                </address>  
                                <address class="Number">
                                    +33 (0)4 50 26 78 00
                                </address>  
                                <div class="clear"></div>
                                <ul>
                                    <li><a rel="nofollow" href="https://www.facebook.com/UMR-Carrtel-1625760484103771/" target="_blank"><img src="/img/473-november/facebook-icn.png" alt="Facebook"></a></li>
                                    <li><a rel="nofollow" href="https://twitter.com/UmrCarrtel" target="_blank"><img src="/img/473-november/twitter-icn.png" alt="Twitter"></a></li>
                                    <li><a rel="nofollow" href="https://www.youtube.com/c/UmrCarrtel" target="_blank"><img src="/img/473-november/youtube-icn.png" alt="Youtube"></a></li>
                                </ul>
                        </div>
                        <!-- // End Right Side // -->
                    </div>

                    <!-- \\ Begin Footer \\-->
                    <footer>
                        <div class="Cntr">                
                            <p>COPYRIGHT © 2020 INRAE UMR CARRTEL</p>
                        </div>
                    </footer>
                    <!-- // End Footer // -->
                </div>
                <!-- // End Get Section // -->
            
            </div>
            <!-- // End Contact Section // -->
        </div>
        <!-- // End Container // -->
  </div>
  <!-- // End Layout Frame // -->

</div>
</template>

<script lang="ts">

import Constants from '@/services/Constants';
import ProfileService from '@/services/ProfileService';
import router from '@/router';

import { latLng, Icon } from 'leaflet';

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

@Component({
  components: {
    LMap,
    LTileLayer,
    LMarker,
    LPopup
  }
})
export default class AboutView extends Vue {

  center = latLng(46.071623, 5.890511);

  lemanPos = latLng(46.439783, 6.480641);
  bourgetPos = latLng(45.7249, 5.8684);
  annecyPos = latLng(45.856166, 6.173468);
  aiguebelettePos = latLng(45.5508, 5.8015);

constructor() {
    super();
  }

  mounted() {
    var jquery = document.createElement('script');
    jquery.type = 'text/javascript';
    jquery.src = '/js/jquery-1.11.0.min.js';
    jquery.async = false;
    document.body.appendChild(jquery);
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = '/js/global.js';
    document.body.appendChild(script);
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/libs/473-november";
@import "../less/libs/473-november-media";
@import "../less/_colors";
@import url("~leaflet/dist/leaflet.css");

.DesignHolder {
  background-color: @white-smoke !important;
}

.LayoutFrame header {
  z-index: 9999;
  background: @white;

  &.smaller {
    height: 73px;
  }
}
header.smaller .Navigation li { height: 73px; }

header.smaller .Navigation li a { padding: 22px 41px; }

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

.Title_sec {
	width: 100%;
	background: url(/img/fishes.jpg) top center no-repeat;
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
      font-size: 36px; color: #fff; font-family: 'Open Sans', sans-serif; font-weight: 800; text-transform: uppercase; margin: 0px; line-height: 34px;
      span {
        color:#E17055; font-family: 'Open Sans', sans-serif; font-weight: 600; display: block;
      }
    }
    p {
      padding: 16px 0px 26px 0px;
      font-size: 16px;
      color: @gunmetal;
      font-family: 'Open Sans', sans-serif;
      font-weight: 600; font-style: italic;
      margin: 0px;
      word-spacing: 2px;
      line-height: 22px;
    }

  }
  .right-panel {
    width: 25%;

    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
  }
}

.Video_sec {
  width: 100%;
  background: @pelorous;
  height: 315px;
  display: flex;
  flex-direction: row;
  justify-content: center;
}

.Producted_by_sec {

  background-color: @solitude;
  height: 300px;
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;

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

    img {
      width: 200px;
      margin-left: 20px;
      margin-right: 20px;
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

    .kf-item {
      width: 33%;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      margin-left: 50px;
      margin-right: 50px;
      .kf-number {
        color: #E17055;
        font-size: 50px;
        font-weight: 530;
        height: 59px;
      }
      .kf-label {
        color: @gunmetal;
        font-size: 18px;
      }
    }
  }
  .map {
    margin-top: 30px;
  }
}

.Contribute_sec p,
.Contact_sec p, {
  font-size: 18px;
}
.Pricing_sec h3,
.About_sec h3 {
  color: @gunmetal;
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
</style>
