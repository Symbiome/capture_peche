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
    <div v-if="!containsData" class="not-enough-data">
      Aucune donnée pour ce lac.
    </div>
    <div v-else  id="evolution-graph">
        <select v-model="displayMode" @change="switchMode">
          <option value="tripsCount">Nombre de sorties avec au moins une prise</option>
          <option value="totalCatchesCount">Nombre d'individus capturés (Total)</option>
          <option value="keptCatchesCount">Nombre d'individus capturés (et conservés)</option>
        </select>
        <Bar v-if="chartData && chartOptions" :data="chartData" :options="chartOptions" ref='chart' />
        <h3>TODO ajouter lien vers la dashboard pole ECLA </h3>
    </div>
</template>

<script lang="ts">

import { EvolutionMetricsForLake, EvolutionMetricForSpecieAndMonth, SpeciesWithAlias } from '@/pojos/BackendPojos';
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
  chartOptions : any | null = {
    responsive: true,
    maintainAspectRatio: false,
    parsing: {
      xAxisKey: 'monthYear',
      yAxisKey: this.displayMode
    },
    scales: {
      x: {
        stacked: true,
        grid: {
            color: 'transparent'
        },
        border: {
            color: '#1E9BC4'
        }
      },
      y: {
        stacked: true,
        grid: {
            color: '#DFE6E9'
        },
        border: {
            color: '#1E9BC4'
        }
      }
    },
    plugins: {
      legend: {
        position: 'bottom'
      },
    }
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

  extractYearFromMetric(metric: EvolutionMetricForSpecieAndMonth) {
    const regexp = /20\d{2}$/g;
    let parsedYear = metric.monthYear.match(regexp);
    if (parsedYear) {
      return parseInt(parsedYear[0]);
    }
    return new Date().getFullYear();
  }

  /* Determines the years to display based on the evolution data fetched */
  getYears() : string[] {
    const currentYear = new Date().getFullYear();
    let earliestYear = currentYear;

    for (const specie in this.evolutionMetrics.evolutionPerMonthAndSpecie) {
      if (this.evolutionMetrics.evolutionPerMonthAndSpecie[specie].length) {
        let metricWithEarliestYear = this.evolutionMetrics.evolutionPerMonthAndSpecie[specie].reduce((min, obj) => {
          const year = this.extractYearFromMetric(obj);
          const yearMin = this.extractYearFromMetric(min)
          return  year < yearMin ? obj : min;
        })
        let earliestYearForSpecie = this.extractYearFromMetric(metricWithEarliestYear);
        if (earliestYear > earliestYearForSpecie) {
          earliestYear = earliestYearForSpecie;
        }
      }
    }
    let years : string[] = [];
    while(earliestYear <= currentYear) {
      years.push((earliestYear++).toString());
    }
    return years;
  }

  getMonths() : string[] {
    // Harcoded to match generated string from backend : java.time.Month getDisplayName(TextStyle.SHORT, Locale.FRANCE)
    return ['janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin', 'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.'];
  }

  getLabels() {
    let labels : any[] = [];
    let years = this.getYears();
    let monthsFr = this.getMonths()
    let currentMonthIndex = new Date().getMonth();
    let currentYear = new Date().getFullYear();

    years.forEach((y, i) => {
      monthsFr.forEach((m, j) => {
        if (i < currentYear || j < currentMonthIndex) {
          labels.push(m + ' ' + y)
        }
      });
    })
    return labels;
  }

  getSpecieNameForLake(specieId: string) {
    const specie = this.speciesNameForLake.find(s => {
        return s.id == specieId;
    });
    return specie ? (specie.alias ?? specie.name) : specieId;
  }

  initChartData() {;
    let labels : any[] = this.getLabels();
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
    this.chartOptions = {...this.chartOptions};
    /* The following instructions is added to be sure the chart is updated with the correct option change */
    this.$refs.chart.options.parsing.xAxisKey = this.displayMode
  }
}
</script>

<style scoped lang="less">
@import "../../../less/main";

.not-enough-data {
  font-style: italic;
  font-size: @fontsize-button;
  line-height: calc(@fontsize-button + @line-height-padding-x-large);
  color: @pale-sky;
  text-align: center;
  margin-top: @vertical-margin-large;
}

#evolution-graph {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 50px;
  margin: 0;
  min-height: 500px;
  min-height: 40vh;
  max-height: 80%;
}

select {
  background: transparent;
  padding: 0 10px;
  height: 35px;
  border: 1px solid @pelorous;
  border-radius: 20px;
  margin-left: 10px;
  font-weight: bold;
  font-size: 14px;
  font-family: inherit;
  color: @pelorous;
  cursor: pointer;
  max-width: 80vw;

  @media screen and (min-width: @desktop-min-width) {
    font-size: 16px;
  }

  option {
    color: black;
  }

  &:hover {
    background-color: white;
  }
}

</style>
