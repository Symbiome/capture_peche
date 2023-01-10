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
  <div>
    <div class="disclaimer">
      <span> Statistiques de l’ensemble des utilisateurs </span>
    </div>

    <div class="section shrinked">
      <h2><i class="icon-fish" />Espèces pêchées</h2>
      <DistributionChart
        :distribution="caughtSpeciesDistribution"
        legend="Capturés"
        greenLegend="conservés"
      ></DistributionChart>
    </div>

    <div class="section">
      <div class="shrinked avg-size">
        <h2>
          <i class="icon-size" />Taille moyenne
          <span class="hide-if-small">par espèce</span> (cm)
        </h2>
        <MaillageLegend
          :selectedLakeUUID="selectedLakeUUID"
          :maillages="maillages"
        />
      </div>
      <div class="not-enough-data" v-if="monthlySizesOptions.length == 0">
        <span>Pas assez de données</span>
      </div>
      <OptionsList
        :items="monthlySizesOptions"
        v-if="monthlySizesOptions.length > 0"
        v-on:item-selected="onMonthlySizeSelected"
      ></OptionsList>
      <div class="shrinked" v-if="monthlySizes">
        <HistogramChart
          :values="monthlySizes"
          :orderedMonths="orderedMonths"
        ></HistogramChart>
      </div>
    </div>

    <div class="update-hour" v-if="showUpdateHour">
      <span v-if="showUpdateHour">
        Heure de dernière mise à jour : {{ updateHour }}
      </span>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";

import {
  GlobalDashboard,
  SpeciesWithAlias,
  Month,
  Maillage,
} from "@/pojos/BackendPojos";
import { GlobalDashboardAndSpecies } from "@/services/DashboardService";
import Helpers from "@/services/Helpers";
import DistributionEntry from "@/pojos/DistributionEntry";
import OptionItem from "@/pojos/OptionItem";

import OptionsList from "@/components/common/OptionsList.vue";
import DistributionChart from "@/components/charts/DistributionChart.vue";
import HistogramChart from "@/components/charts/HistogramChart.vue";
import MaillageLegend from "./MaillageLegend.vue";

@Component({
  components: {
    DistributionChart,
    HistogramChart,
    OptionsList,
    MaillageLegend,
  },
})
export default class GlobalDashboardComponent extends Vue {
  speciesIndex: { [index: string]: SpeciesWithAlias } = {};
  caughtSpeciesDistribution: DistributionEntry[] = [];
  orderedMonths: Month[] | null = null;
  monthlySizesOptions: OptionItem[] = [];
  monthlySizes: { [P in Month]?: number } | null = null;
  updateHour: string = "";

  @Prop() dashboardData: GlobalDashboardAndSpecies;
  @Prop({ default: false }) showUpdateHour: boolean;
  @Prop() selectedLakeUUID: string;

  maillages: Maillage[] = [];

  constructor() {
    super();
  }

  mounted() {
    this.dashboardDataChanged();
  }

  @Watch("dashboardData")
  dashboardDataChanged() {
    this.speciesIndex = {};
    this.caughtSpeciesDistribution = [];
    this.orderedMonths = null;
    this.monthlySizesOptions = [];
    this.monthlySizes = null;

    const speciesAliases = this.dashboardData.dashboard.speciesAliases;
    this.dashboardData.species.forEach((species) => {
      this.speciesIndex[species.id] = species;

      const aliases = speciesAliases[species.id];
      if (aliases) {
        species.alias = aliases.join(" ou ");
      }
    });

    const speciesCount = this.dashboardData.dashboard.caughtSpeciesCount;
    const distribution = this.dashboardData.dashboard.caughtSpeciesDistribution;
    const releasedDistribution =
      this.dashboardData.dashboard.caughtAndReleasedSpeciesDistribution;
    Object.keys(distribution).forEach((speciesId) => {
      const species: SpeciesWithAlias = this.speciesIndex[speciesId];
      const percent: number = Math.round(distribution[speciesId]);
      const releasedPercent: number =
        Math.round(releasedDistribution[speciesId]) | 0;
      const keptPercent: number = percent - releasedPercent;
      const count: number = speciesCount[speciesId];
      const keptCount: number =
        percent == 0 ? 0 : Math.round((count * keptPercent) / percent);
      const keptLabel: string = "conservé" + (keptCount > 1 ? "s" : "");
      const entry: DistributionEntry = new DistributionEntry(
        speciesId,
        species.name,
        percent,
        keptPercent,
        count,
        species.alias,
        keptCount,
        keptLabel
      );
      this.caughtSpeciesDistribution.push(entry);
    });

    this.orderedMonths = this.dashboardData.dashboard.orderedMonths;
    const monthlySizesSpecies: string[] = Object.keys(
      this.dashboardData.dashboard.monthlySizesPerMaillage
    );
    monthlySizesSpecies.forEach((speciesId) => {
      const species: SpeciesWithAlias = this.speciesIndex[speciesId];
      this.monthlySizesOptions.push({
        id: species.id,
        name: species.name,
        alias: species.alias,
        whatever:
          this.dashboardData.dashboard.monthlySizesPerMaillage[speciesId],
      });
    });
    this.monthlySizesOptions = Vue.lodash.orderBy(
      this.monthlySizesOptions,
      "name"
    );

    let date = this.parseLocalDateTime(this.dashboardData.dashboard.computedOn);
    this.updateHour = Helpers.formatToHour(date);
  }

  onMonthlySizeSelected(item: OptionItem) {
    this.monthlySizes = item.whatever;
    this.maillages = [];
    // @ts-ignore
    Object.keys(this.monthlySizes).forEach((month) => {
      // @ts-ignore
      Object.keys(this.monthlySizes[month]).forEach((maillage) => {
        if (this.maillages.indexOf(maillage as Maillage) == -1) {
          this.maillages.push(maillage as Maillage);
        }
      });
    });
    this.maillages = this.maillages.sort();
  }

  parseLocalDateTime(input: any): Date {
    return Helpers.parseLocalDateTime(input);
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "../../less/main";

.disclaimer {
  font-style: italic;
  font-size: @fontsize-button;
  line-height: calc(@fontsize-button + @line-height-padding-x-large);
  color: @pale-sky;
  text-align: center;
  margin-top: @vertical-margin-large;
  padding-left: @margin-large;
  padding-right: @margin-large;
}

.update-hour {
  font-style: italic;
  font-size: @fontsize-paragraph;
  line-height: calc(@fontsize-paragraph + @line-height-padding-x-large);
  color: @pale-sky;
  text-align: right;
  margin-top: @vertical-margin-large;
  padding-right: @margin-large;

  @media screen and (min-width: @desktop-min-width) {
    padding-right: @margin-large-desktop;
  }
}
</style>
