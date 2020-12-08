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
                  <h1><a href="#about">FISHOLA</a></h1>
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
        <div class="Banner_sec" id="presentation">
            <!--  \\ Begin banner Side -->
            <div class="bannerside">
              <div class="Center">
                    <!--  \\ Begin Left Side -->
                    <div class="leftside">
                        <h3><span>FISHOLA</span></h3>
                        <p>est l'application smartphone pour une gestion durable de la pêche sur les lacs alpins (Léman, lac d’Annecy, du Bourget et d’Aiguebelette)</p>
                        <!-- <a href="#about">MORE DETAILS</a> -->
                    </div>                                        
                    <!--  // End Left Side // -->
                    <!--  \\ Begin Right Side -->
                    <div class="rightside">
                      <iframe class="youtube" width="560" height="315" src="https://www.youtube-nocookie.com/embed/HNl9-I-Wqcg" frameborder="10" allow="encrypted-media" allowfullscreen></iframe>
                    </div>
                    <!--  // End Right Side // -->
              </div>
            </div>

                <h3>FISHOLA est produit par </h3>
                <div class="credits-logos">
                  <a href="https://www.inrae.fr" target="_blank"><img src='/img/credits/inrae.svg'/></a>
                  <a href="https://ofb.gouv.fr/" target="_blank"><img src='/img/credits/ofb.png'/></a>
                  <a href="https://www.codelutin.com" target="_blank"><img src='/img/credits/code-lutin.png'/></a>
                </div>

            <!--  // End banner Side // -->
            <div class="clear"></div>
        </div>
        <!-- // End Banner Section // -->
        <!-- \\ Begin Container \\ -->
        <div id="Container">
            <!-- \\ Begin About Section \\ -->
            <div class="About_sec" id="about">
                <div class="Center">
                    Carte
                    <div class="Line"></div>
                    <!-- // End Tab Side // -->
                </div>
            </div>
            <!-- // End About Section // -->
        <!-- \\ Begin Contribute Section \\ -->
        <div class="Contribute_sec" id="contribute">
            <div class="Center">
                <h2>Comment participer ?</h2>
                <p>Morbi accumsan ipsum velit. Nam nec tellus a odio tincidunt auctor a ornare odio. Sed non  mauris vitae erat consequat auctor eu in elit. Class aptent<br> taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos.</p>    
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
                            <p>COPYRIGHT © 2020 UMR CARRTEL</p>
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

import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class AboutView extends Vue {

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
<style lang="less">

@import "../less/libs/473-november";
@import "../less/libs/473-november-media";
@import "../less/_colors";

body {
  background-color: @white-smoke !important;
}

.Banner_sec .rightside iframe.youtube { margin-left: calc(50% - 280px); }

.Pricing_sec h3 { color: @gunmetal; }

  .credits-logos {
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    margin-bottom: 50px;
    img {
      width: 200px;
      margin-left: 20px;
      margin-right: 20px;
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
</style>
