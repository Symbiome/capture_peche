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
        <h1>Aide à l'utilisation du back-office de Fishola</h1>
        <p>
          Cette page d'aide propose un mémo des fonctionnalités essentielles du back-office.<br/>
        </p>
      </div>
      <ul id="toc" />

      <h2>Autorisations par type d'administrateur</h2>
        <table>
          <thead>
            <tr>
              <td></td>
              <td></td>
              <td>Administrateurs régionaux</td>
              <td>Administrateurs nationaux</td>
            </tr>
          </thead>
          <tbody>
            <tr>
              <th rowspan="4">Référentiels</th>
              <th>Plans d'eau</th>
              <td></td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
            <tr>
              <th>Espèces</th>
              <td></td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
            <tr>
              <th>Météo</th>
              <td></td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
            <tr>
              <th>Techniques de pêche</th>
              <td></td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
            <tr>
              <th rowspan="2">Paramétrage</th>
              <th>Espèces par plan d'eau</th>
              <td>
                <b-icon icon="check" size="is-medium"></b-icon>
                (restreint aux lacs de l'administrateur)
              </td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
            <tr>
              <th>Maillages et tailles maximales</th>
              <td>
                <b-icon icon="check" size="is-medium"></b-icon>
                (restreint aux lacs de l'administrateur)
              </td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
            <tr>
              <th rowspan="3">Documentations</th>
              <th>Pages éditoriales</th>
              <td></td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
            <tr>
              <th>En téléchargement</th>
              <td></td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
            <tr>
              <th>Communications</th>
              <td>
                <b-icon icon="check" size="is-medium"></b-icon>
                (restreint aux lacs de l'administrateur)
              </td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
            <tr>
              <th>Chiffres clés</th>
              <th></th>
              <td>
                <b-icon icon="check" size="is-medium"></b-icon>
                (restreint aux lacs de l'administrateur)
              </td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
            <tr>
              <th>Sorties</th>
              <th></th>
              <td></td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
            <tr>
              <th>Utilisateurs</th>
              <th></th>
              <td></td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
            <tr>
              <th>Administrateurs</th>
              <th></th>
              <td>
                <b-icon icon="check" size="is-medium"></b-icon>
                (<b>Uniquement si l'administrateur dispose du droit de gestion des administrateurs</b>, et restreint aux lacs de l'administrateur)
              </td>
              <td><b-icon icon="check" size="is-medium"></b-icon></td>
            </tr>
          </tbody>
        </table>

      <h2>Ajouter une espèce <span class="nationalAdmin">Administrateurs nationaux</span></h2>
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

      <h2>Paramétrer les espèces par plan d'eau <span class="regionalAdmin">Administrateurs régionaux</span> <span class="nationalAdmin">Administrateurs nationaux</span></h2>
      <p>
        L'ajout d'une espèce se fait dans un écran dédié, accessible à partir du menu principal :
        <b-navbar-dropdown label="Paramétrage" :active="true">
          <b-navbar-item tag="router-link" :to="{ name: 'species-per-lake' }" :active="true">
            Espèces par plan d'eau
          </b-navbar-item>
          <b-navbar-item tag="router-link" :to="{ name: 'authorized-samples' }">
            Maillages et tailles maximales
          </b-navbar-item>
        </b-navbar-dropdown>

        Cet écran propose d'abord de sélectionner les plans d'eau à afficher pour pouvoir les paramétrer. Le champ de recherche permet d'en sélectionner plusieurs.<br/><br/>

        Les plans d'eau sélectionnés se retrouvent en colonnes du tableau proposé ensuite, avec une ligne par espèce.<br/>
        Pour chacune d'entre elles, il est possible de la décocher pour spécifier qu'elle n'est pas présente sur le plean d'eau.<br/>
        Les espèces présentes peuvent être renommée afin d'utiliser l'appelation utilisée localement.

        <img class="screen-capture" src="/img/help/config-species-per-lake.png" alt="Écran de paramétrage des espèces par plan d'eau" />
      </p>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-facing-decorator";
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
      console.log(allH2Tags[el]);
      toc += "<li><a href=#" +  allH2Tags[el].id + ">" + allH2Tags[el].firstChild.textContent + "</a></li>"
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
      jsPDF: { format: 'a4', orientation: 'landscape' }
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

  .export-button {
    margin: 20px auto;
  }

  h2 {
    padding-top: 3.25rem;
    margin-bottom: 1rem;
    font-size: 24px;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    border-bottom: 1px solid #888;
  }

  #toc {
    padding: 15px 50px;
    margin-top: 20px;
    background-color: #EEE;
    list-style: circle;

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
      padding: 3px 20px;
      border-radius: 5px;
      font-size: 1rem;
      font-weight: bold;
      background-color: #ff8787;
      color: #4d0f0f;
      &:first-of-type {
        margin-left: auto;
        margin-right: 15px;
      }
    }
    .regionalAdmin {
      background-color: #ffe3e3;
      color: #c92a2a;
    }

    p {
      clear: both;
    }

    table {
      tbody {
        tr {
          border-bottom: 1px solid #ccc;
        }
      }
      td, th {
        padding: 3px 15px;
        vertical-align: middle;
        max-width: 30vw;
      }
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
    .export-button {
      position: absolute;
      right: 40px;
      top: 80px;
      z-index: 2;
    }
    #toc {
      position: fixed;
      right: 0;
      top: 140px;
      width: 20%;
    }
  }
</style>
