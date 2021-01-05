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

    <div class="section shrinked">
      <h2><i class="icon-fish" />Espèces pêchées</h2>
      <DistributionChart :distribution="caughtSpeciesDistribution"></DistributionChart>
    </div>

    <div class="section">
      <div class="shrinked">
        <h2><i class="icon-size" />Historique des tailles (cm)</h2>
      </div>
      <div class="not-enough-data" v-if="monthlySizesOptions.length == 0">
        <span>Pas assez de données</span>
      </div>
      <OptionsList :items="monthlySizesOptions"
                    v-if="monthlySizesOptions.length > 0"
                    v-on:item-selected="onMonthlySizeSelected"></OptionsList>
      <div class="shrinked" v-if="monthlySizes">
        <HistogramChart :values="monthlySizes"
                        :orderedMonths="orderedMonths"></HistogramChart>
      </div>
    </div>

  </div>
</template>

<script lang="ts">

import { Component, Prop, Vue } from 'vue-property-decorator';

import {GlobalDashboard, SpeciesWithAlias, Month} from '@/pojos/BackendPojos';
import {GlobalDashboardAndSpecies} from '@/services/DashboardService';
import DistributionEntry from '@/pojos/DistributionEntry';
import OptionItem from '@/pojos/OptionItem';

import OptionsList from '@/components/common/OptionsList.vue';
import DistributionChart from '@/components/charts/DistributionChart.vue';
import HistogramChart from '@/components/charts/HistogramChart.vue';


@Component({
  components: {
    DistributionChart,
    HistogramChart,
    OptionsList
  }
})
export default class GlobalDashboardComponent extends Vue {

  speciesIndex:{ [index: string]: SpeciesWithAlias } = {};
  caughtSpeciesDistribution:DistributionEntry[] = [];
  orderedMonths: Month[] | null = null;
  monthlySizesOptions: OptionItem[] = [];
  monthlySizes: { [P in Month]?: number } | null = null;

  @Prop() dashboardData:GlobalDashboardAndSpecies;

  constructor() {
    super();
  }

  created() {
    const speciesAliases = this.dashboardData.dashboard.speciesAliases;
    this.dashboardData.species.forEach((species) => {
      this.speciesIndex[species.id] = species;

      const aliases = speciesAliases[species.id];
      if (aliases) {
        species.alias = aliases.join(' ou ');
      }
    });

    const speciesCount = this.dashboardData.dashboard.caughtSpeciesCount;
    const distribution = this.dashboardData.dashboard.caughtSpeciesDistribution;
    const releasedDistribution = this.dashboardData.dashboard.caughtAndReleasedSpeciesDistribution;
    Object.keys(distribution)
      .forEach((speciesId) => {
        const species:SpeciesWithAlias = this.speciesIndex[speciesId];
        const percent:number = Math.round(distribution[speciesId]);
        const releasedPercent:number = Math.round(releasedDistribution[speciesId]) | 0;
        const count:number = speciesCount[speciesId];
        const entry:DistributionEntry = new DistributionEntry(speciesId, species.name, percent, releasedPercent, count, species.alias);
        this.caughtSpeciesDistribution.push(entry);
    });

    this.orderedMonths = this.dashboardData.dashboard.orderedMonths;
    const monthlySizesSpecies:string[] = Object.keys(this.dashboardData.dashboard.monthlySizes);
    monthlySizesSpecies.forEach((speciesId) => {
      const species:SpeciesWithAlias = this.speciesIndex[speciesId];
      this.monthlySizesOptions.push({
        id: species.id,
        name: species.name,
        alias: species.alias,
        whatever: this.dashboardData.dashboard.monthlySizes[speciesId]
      });
    });
    this.monthlySizesOptions = Vue.lodash.orderBy(this.monthlySizesOptions, 'name');

  }

  onMonthlySizeSelected(item:OptionItem) {
    this.monthlySizes = item.whatever;
  }

}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">

@import "../../less/main";

</style>
