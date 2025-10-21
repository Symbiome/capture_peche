<!--
  #%L
  Fishola :: Admin
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
  <div id="help" ref="document">

    <b-button type="is-primary" @click="exportToPDF" class="export-button">
      Télécharger en PDF
    </b-button>

    <div id="help-content">
      <div class="intro">
        <h1>Aide</h1>
      </div>

      <h2>Ajouter une espèce</h2>
      <div class="nationalAdmin">Administrateurs nationaux</div>
      <!-- <div class="regionalAdmin">Administrateurs régionaux</div> -->
      <p>
        L'ajout d'une espèce se fait dans un écran dédié, accessible à partir du menu principal :
        <b-navbar-dropdown label="Référentiels" :active="true">
          <b-navbar-item tag="router-link" :to="{ name: 'lakes' }">
            Plans d'eau
          </b-navbar-item>
          <b-navbar-item tag="router-link" :to="{ name: 'species' }" :active="true">
            Espèces
          </b-navbar-item>
          <b-navbar-item tag="router-link" :to="{ name: 'weathers' }">
            Météo
          </b-navbar-item>
          <b-navbar-item tag="router-link" :to="{ name: 'techniques' }">
            Techniques de pêche
          </b-navbar-item>
        </b-navbar-dropdown>

        Cet écran présente un tableau contenant toutes les espèces existantes dans Fishola.

        Un bouton <b>Nouveau</b> est suité en bas à droite de la page pour accéder au formulaire d'ajout d'une espèce.
        <img class="screen-capture" src="/img/help/create-specie-form.png" alt="Formulaire de création d'une espèce" />
      </p>
    </div>

    <ul id="toc" />
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import html2pdf from "html2pdf.js";

@Component({
  components: {}
})
export default class Help extends Vue {
  
  mounted() {
    this.generateTableOfContent();
  }

  generateTableOfContent() {
    let allH2Tags = document.getElementsByTagName('h2');
    let toc = "";

    Object.keys(allH2Tags).forEach((el) => {
      allH2Tags[el].id = "toc-" + el;
      toc += "<li><a href=#" +  allH2Tags[el].id + ">" + allH2Tags[el].textContent + "</a></li>"
    })
    document.getElementById("toc").innerHTML = toc;
  }

  exportToPDF() {
    let content = document.getElementById("help-content");
    content.classList.add('print');
    html2pdf(content, {
      margin: 20,
      filename: "Fishola-aide.pdf",
      image: { type: 'jpeg', quality: 1 },
      pagebreak: {  mode: "css", before: "h2" },
    }).then(() => {
      content.classList.remove('print');
    });
  }

}
</script>

<style scoped lang="less">
  .only-print {
    display: none;
  }

  h2 {
    padding-top: 3.25rem;
    font-size: 24px;
  }

  .export-button {
    position: absolute;
    right: 40px;
    top: 80px;
    z-index: 2;
    box-shadow: 0 0 5px 15px #fff;
  }

  #toc {
    position: fixed;
    right: 0;
    top: 140px;
    background-color: #EEE;

    :deep(li) {
      margin-bottom: 5px;
      a {
        color: #888;
        font-weight: bold;
        &:hover {
          color: @terra-cotta;
        }
      }
    }
  }

  #help-content {
    .navbar-item.has-dropdown {
      margin: 20px auto;
      width: fit-content;
      display: flex;
      flex-direction: column;

      .navbar-item.is-active {
        background-color: @pelorous;
        color:white !important;
      }
    }
    :deep(.navbar-dropdown) {
      position: relative;
      display: block !important;
    }

    .screen-capture {
      margin: 20px auto;
      display: block;
      padding: 5px;
      border: 1px solid #eee;
    }

    .nationalAdmin,
    .regionalAdmin {
      float: left;
      padding: 3px 20px;
      margin: 5px 10px 5px 0;
      border-radius: 5px;
      font-weight: bold;
      background-color: #ff8787;
      color: #4d0f0f;
    }
    .regionalAdmin {
      background-color: #ffe3e3;
      color: #c92a2a;
    }

    p {
      clear: both;
    }
    .print {
      .intro {
        display: flex;
        height: 100%;
        width: 100%;
        align-items: center;
        justify-content: center;
      }
      .only-print {
        display: initial;
      }
    }
  }

  @media (min-width: 768px) {
    #help {
      max-width: 80%;
      text-align: justify;
    }
    #toc {
      width: 20%;
      padding: 50px 20px;
    }
  }
</style>
