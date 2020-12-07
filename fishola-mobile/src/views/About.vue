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
        <header>
            <div class="Center">
                <div class="site-logo">
                <h1><a href="#">FISHOLA</a></h1>
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
                                <a href="#about">Comment participer ?</a>
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
                            <li>
                                <a href="#contact">Contact</a>
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
                      <iframe class="youtube" width="560" height="315" src="https://www.youtube-nocookie.com/embed/wEqkiAeDhHI" frameborder="10" allow="encrypted-media" allowfullscreen></iframe>
                    </div>
                    <!--  // End Right Side // -->
              </div>
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
                    <h3>FISHOLA est produit par </h3>
                    <div class="credits-logos">
                      <a href="https://www.inrae.fr" target="_blank"><img src='/img/credits/inrae.svg'/></a>
                      <a href="https://ofb.gouv.fr/" target="_blank"><img src='/img/credits/ofb.png'/></a>
                      <a href="https://www.codelutin.com" target="_blank"><img src='/img/credits/code-lutin.png'/></a>
                    </div>
                    <div class="Line"></div>  
                    <div class="welcome-apps">
                        <a href="https://play.google.com/store/apps/details?id=fr.inrae.fishola" target="_blank"><img class="app" src="/img/473-november/GooglePlay.png" alt="GooglePlay" /></a>
                        <a href="https://apps.apple.com/fr/app/fishola/id1521226635" target="_blank"><img class="app" src="/img/473-november/AppStore.png" alt="AppStore" /></a>
                    </div>
                  
                    <!-- // End Tab Side // -->
                </div>
            </div>
            <!-- // End About Section // -->
        <!-- \\ Begin Services Section \\ -->
        <div class="Services_sec" id="faq">
            <div class="Center">
                <h2>our Services</h2>
                <p> Morbi accumsan ipsum velit. Nam nec tellus a odio tincidunt auctor a ornare odio. Sed non  mauris vitae erat consequat auctor eu in elit. Class aptent<br> taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos.</p>    
                <div class="Line"></div>
                <!-- \\ Begin Services Side  \\ -->
                <div class="Serviceside">
                    <ul>
                      <li class="Development"><a href="#services"><h4>DEVELOPMENT</h4></a></li>
                      <li class="Desdin"><a href="#services"><h4>DESIGN</h4></a></li>
                      <li class="Concept"><a href="#services"><h4>CONCEPT</h4></a></li>
                      <li class="System"><a href="#services"><h4>SYSTEM</h4></a></li>
                    </ul>
                </div>
                <!-- // End Services Side // -->
            </div>                
        </div>
        <!-- // End Services Section // -->
        <!-- \\ Begin Pricing Section \\ -->
        <div class="Pricing_sec" id="pricing">
            <div class="Center">
                <h2>Pricing</h2>
                <p>All plans come with unlimited disk space. Our support can be as quick as 15 minutes to get a response. Sed non<br>
                mauris vitae erat consequat auctor eu in elit. Class aptent taciti sociosqu.</p>
                <div class="Line"></div>
                <!-- \\ Begin Pricing Side \\ -->
                <div class="Pricingside">
                    <ul>
                        <li>
                            <div class="Basic">
                              <h5>basic</h5>
                            </div>
                            <div class="Dollar">
                              <h2>$27.50</h2>
                            </div>
                            <div class="Band">
                              <p>2,000 GB <span>Band width</span></p>
                            </div>
                            <div class="Band">
                              <p>32 GB<span>memory</span></p>
                            </div>
                            <div class="Band">
                              <p>Support<span>24 Hours</span></p>
                            </div>
                            <div class="Band last">
                              <p>Update<span>$20</span></p>
                            </div>
                            <div class="Order">
                              <a href="#">order now</a>
                            </div>
                        </li>
                        <li>
                            <div class="Basic">
                              <h5>Biz</h5>
                            </div>
                            <div class="Dollar">
                              <h2>$44.50</h2>
                            </div>
                            <div class="Band">
                              <p>5,500 GB <span>Band width</span></p>
                            </div>
                            <div class="Band">
                              <p>64 GB<span>memory</span></p>
                            </div>
                            <div class="Band">
                              <p>Support<span>1 Hour</span></p>
                            </div>
                            <div class="Band last">
                              <p>Update<span>$10</span></p>
                            </div>
                            <div class="Order">
                              <a href="#">order now</a>
                            </div>
                        </li>
                        <li>
                            <div class="Basic">
                              <h5>Pro</h5>
                            </div>
                            <div class="Dollar">
                              <h2>$72.50</h2>
                            </div>
                            <div class="Band">
                              <p>12,000 GB <span>Band width</span></p>
                            </div>
                            <div class="Band">
                              <p>128 GB<span>memory</span></p>
                            </div>
                            <div class="Band">
                              <p>Support<span>15 Mins</span></p>
                            </div>
                            <div class="Band last">
                              <p>Update<span>Free</span></p>
                            </div>
                            <div class="Order">
                              <a href="#">order now</a>
                            </div>
                        </li>
                    </ul>                  
                </div>
                <!-- // End Pricing Side // -->
            </div>
        </div>
        <!-- // End Pricing Section // -->
        <!-- \\ Begin Contact Section \\ -->
        <div class="Contact_sec" id="contact">
            <div class="Contactside">
                <div class="Center">
                    <h2>contact us</h2>
                    <p>Sed non neque elit. Sed ut imperdiet nisi. Proin condimentum fermentum nunc. Etiam pharetra, erat sed fermentum<br>
                    feugiat velit mauris egestas quamut aliquam massa nisl quis neque. Suspendisse in orci enim.</p>
                    <div class="Line"></div>
                </div>  

            </div>

                <div class="Map">
                
                    <div id="GoogleMap"></div>
                
                </div>
                
                <!-- \\ Begin Get Section \\ -->
                <div class="Get_sec">
                    <div class="Mid">          
                        <!-- \\ Begin Left Side \\ -->
                        <div class="Leftside">
                            <form action="#">
                                <fieldset>
                                    <p><input type="text" value="" placeholder="NAME" class="field"></p>
                                    <p><input type="email" value="" placeholder="EMAIL" class="field"></p>
                                    <p><input type="text" value="" placeholder="TITLE" class="field"></p>
                                    <p><textarea cols="2"  rows="2" placeholder="MESSAGE"></textarea></p>
                                    <p><input type="submit" value="send" class="button"></p>
                                </fieldset>
                            </form>
                        </div>
                        <!-- // End Left Side // -->
                        <!-- \\ Begin Right Side \\ -->
                        <div class="Rightside">
                            <h3>Get in touch !</h3>
                                <address>
                                    990 Proin Gravida Street, Aliquet Snean Tate,<br>Lincoln Way, San Francisco, California.
                                </address>  
                                <address class="Number">
                                    (+001) 001 002 0034, (+002) 009 008 0760<br>(+003) 456 050 0670 
                                </address>  
                                <address class="Email">
                                    <a href="mailto:info@november.com">info@november.com</a>
                                </address>  
                                <div class="clear"></div>
                                <ul>
                                    <li><a rel="nofollow" href="http://www.facebook.com/templatemo"
                                   target="_parent"><img src="img/facebook-icn.png" alt="image"></a></li>
                                    <li><a href="#"><img src="img/twitter-icn.png" alt="image"></a></li>
                                    <li><a href="#"><img src="img/google-plus-icn.png" alt="image"></a></li>
                                </ul>
                        </div>
                        <!-- // End Right Side // -->
                    </div>
                    <!-- \\ Begin Footer \\-->
                    <footer>
                        <div class="Cntr">                
                            <p>COPYRIGHT © 2084 COMPANY NAME. DESIGN: <a rel="nofollow" href="http://www.templatemo.com" target="_parent">TEMPLATEMO</a></p>
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

import Constants from '@/services/Constants'
import ProfileService from '@/services/ProfileService'
import router from '@/router'

import { Component, Prop, Vue } from 'vue-property-decorator';

export default class AboutView extends Vue {

  mounted() {
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

.About_sec h3 { color: @gunmetal; }


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
