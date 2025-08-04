<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
    <div class="section shrinked">
        <div v-if="!containsData">
          Aucune donnée pour ce lac.
        </div>
        <div v-else>
            <select v-model="displayMode" @change="switchMode">
              <option value="tripsCount">Nombre de sorties avec au moins une prise</option>
              <option value="totalCatchesCount">Nombre d'individus capturés (Total)</option>
              <option value="keptCatchesCount">Nombre d'individus capturés (et conservés)</option>
            </select>
            <Bar v-if="chartData && chartOptions" :data="chartData" :options="chartOptions" ref='chart' />
            <h3>TODO ajouter lien vers la dashboard pole ECLA </h3>
        </div>
    </div>
</template>

<script lang="ts">

import { EvolutionMetricsForLake, Month, SpeciesWithAlias } from '@/pojos/BackendPojos';
import DashboardService from '@/services/DashboardService';
import ReferentialService from '@/services/ReferentialService';
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';

import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale
} from 'chart.js'
import { Bar } from 'vue-chartjs'
import { months } from 'moment';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend)

@Component({
  components: {
    Bar,
  },
})
export default class EvolutionMetricsView extends Vue {
  @Prop() lakeId: string;
  @Prop({default: false}) onlyShowUserStats: boolean;
  speciesNameForLake: SpeciesWithAlias[] = [];
  allSpecies: string[];

  displayMode = 'tripsCount';
  containsData:boolean = false;
  chartData : any | null = null;
  chartOptions : any | null =  {
    responsive: true,
    parsing: {
      xAxisKey: 'monthYear',
      yAxisKey: this.displayMode
    },
    scales: {
      x: {
        stacked: true,
      },
      y: {
        stacked: true
      }
    },
  };

  evolutionMetrics: EvolutionMetricsForLake = {
    evolutionPerMonthAndSpecie: {},
  };

  mounted() {
    this.loadEvolutionData();
    
  }

  @Watch("lakeId")
  async loadEvolutionData() {
    if (this.lakeId) {
        this.speciesNameForLake = await ReferentialService.getSpeciesPlusCustom(this.lakeId);
        if (this.onlyShowUserStats) {
            this.evolutionMetrics = await DashboardService.loadUserEvolutionOrTimeout(this.lakeId);
        } else {
            this.evolutionMetrics = await DashboardService.loadGlobalEvolutionOrTimeout(this.lakeId);
        }
        this.$emit("loaded", true);

        let checkForData = false;
        for (const specie in this.evolutionMetrics.evolutionPerMonthAndSpecie) {
          if (this.evolutionMetrics.evolutionPerMonthAndSpecie[specie].length) {
            checkForData = true;
            break;
          }
        }
        this.containsData = checkForData;

        this.initChartData();
    }
  }

  getYears() : string[] {
    return ['2020', '2021', '2022', '2023', '2024'];
  }

  getMonths() : string[] {
      // if (year && this.evolutionMetrics.catchCountPerMonthAndSpecies[year]) {
      // return Object.keys(this.evolutionMetrics.catchCountPerMonthAndSpecies[year]) as Month[];
      // }
      // return [];
    return ['janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin', 'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.'];
  }

  getSpecieNameForLake(specieId: string) {
    const specie = this.speciesNameForLake.find(s => {
        return s.id == specieId;
    });
    return specie ? (specie.alias ?? specie.name) : specieId;
  }

  initChartData() {

    let years = this.getYears();
    let monthsFr = this.getMonths();
    let labels : any[] = [];
    years.forEach(y => {
      monthsFr.forEach(m => {
        labels.push(m + ' ' + y)
      });
    })

    let datasets : any[] = [];
    let colors = ['#1971c2', '#099268', '#66a80f', '#a9e34b', '#ffd43b', '#9c36b5', '#e03131',
        '#4dabf7', '#38d9a9', '#a9e34b', '#ffd43b', '#ffd43b', '#da77f2', '#ff8787'];

    if (this.evolutionMetrics && this.evolutionMetrics.evolutionPerMonthAndSpecie) {
      Object.keys(this.evolutionMetrics.evolutionPerMonthAndSpecie).forEach((element, i) => {
        if (this.evolutionMetrics.evolutionPerMonthAndSpecie[element].length) {
          let dataset = {
            label: this.getSpecieNameForLake(element),
            data: this.evolutionMetrics.evolutionPerMonthAndSpecie[element],
            backgroundColor: colors[i % colors.length]
          }
          datasets.push(dataset);
        }
      });
    }
    this.chartData = {
      labels: labels,
      datasets: datasets
    }
  }

  switchMode() {
    this.chartOptions = {
      responsive: true,
      parsing: {
        xAxisKey: 'monthYear',
        yAxisKey: this.displayMode
      },
      scales: {
        x: {
          stacked: true,
        },
        y: {
          stacked: true
        }
      },
    }
    /* The following instructions is added to be sure the chart is updated with the correct option change */
    this.$refs.chart.options.parsing.yAxisKey = this.displayMode
  }
}
</script>

<style scoped lang="less">
@import "../../../less/main";

select {
  background: transparent;
  padding: 0 10px;
  height: 35px;
  border: 1px solid @pelorous;
  border-radius: 20px;
  margin-left: 10px;
  font-weight: bold;
  font-size: 16px;
  font-family: inherit;
  color: @pelorous;
  cursor: pointer;

  option {
    color: black;
  }

  &:hover {
    background-color: white;
  }
}

</style>
