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
      Aucune donnée pour ce plan d'eau.
    </div>
    <div v-else  id="evolution-graph">
        <select v-model="displayMode" @change="switchMode">
          <option value="tripsCount">Nombre de sorties avec au moins une prise</option>
          <option value="totalCatchesCount">Nombre d'individus capturés (Total)</option>
          <option value="keptCatchesCount">Nombre d'individus capturés (et conservés)</option>
        </select>
        <Bar v-if="chartData && chartOptions" :data="chartData" :options="chartOptions" ref='chart' />

        <a class="link" href="https://dashboard.ecla.inrae.fr/fishola/">
          Voir les données sur la plateforme ECLA
        </a>
    </div>
</template>

<script lang="ts">

import { EvolutionMetricsForLake, EvolutionMetricForSpecieAndMonth, SpeciesWithAlias } from '@/pojos/BackendPojos';
import DashboardService from '@/services/DashboardService';
import ReferentialService from '@/services/ReferentialService';
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import chartjsPluginDatalables from 'chartjs-plugin-datalabels'
import chartjsPluginZoom from 'chartjs-plugin-zoom'

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

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, chartjsPluginDatalables, chartjsPluginZoom)

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
  chartOptions : any | null = null;

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
        this.initChartOptions();
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

  getLabels() {
    let labels : any[] = [];
    let years = this.getYears();
    let currentMonthIndex = new Date().getMonth();
    let currentYear = new Date().getFullYear();

    years.forEach(year => {
      for (var month = 1 ; month <= 12; month++) {
          if (parseInt(year) < currentYear || month < currentMonthIndex) {
          labels.push((month < 10 ? '0' : '') + month + '/' + year)
        }
      }
    })
    return labels;
  }

  getSpecieNameForLake(specieId: string) {
    const specie = this.speciesNameForLake.find(s => {
        return s.id == specieId;
    });
    return specie ? (specie.alias ?? specie.name) : specieId;
  }

  initChartData() {
    let labels : any[] = this.getLabels();
    let datasets : any[] = [];
    let colors = [
      '#69db7c', // light green
      '#4dabf7', // light blue
      '#ffd43b', // light orange
      '#faa2c1', // light pink
      '#cc5de8', // light purple
      '#1971c2', // dark blue
      '#c2255c', // dark pink
      '#2f9e44', // dark green
      '#f08c00', // dark orange
      '#862e9c', // dark purple
      '#99e9f2', // light cyan
      '#c0eb75', // light lime
      '#03045e', // Various colors then
      '#99582a', '#bb9457', '#cfbaf0', '#9e0059', '#390099',
      '#6a994e', '#7b2cbf', '#daddd8', '#fed766', '#5e6472'
    ];
    let colorIndex = 0;

    if (this.evolutionMetrics && this.evolutionMetrics.evolutionPerMonthAndSpecie) {
      Object.keys(this.evolutionMetrics.evolutionPerMonthAndSpecie).forEach((element) => {
        if (this.evolutionMetrics.evolutionPerMonthAndSpecie[element].length) {
          let dataset = {
            label: this.getSpecieNameForLake(element),
            data: this.evolutionMetrics.evolutionPerMonthAndSpecie[element],
            backgroundColor: colors[colorIndex++ % colors.length]
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

  initChartOptions() {
    this.chartOptions =  {
      responsive: true,
      maintainAspectRatio: false,
      parsing: {
        xAxisKey: 'monthYear',
        yAxisKey: this.displayMode
      },
      interaction: {
        mode: 'x'
      },
      layout: {
        padding: {
          top: 20,
          right: 1 /* to be prevent grid display issue */
        }
      },
      onResize: function(chart, size) {
        // Hide month labels display on low resolution screens
        chart.options.scales.x.ticks.display = (size.width > 1000);
        if (chart.scales.secondX) {
          chart.scales.secondX._labelSizes.widths = [20,20,20,300,20,20];
        }
      },
      scales: {
        x: {
          stacked: true,
          grid: {
            color:  function(context) {
              if (context.tick && context.tick.label == '01') {
                return '#ccc';
              }
            },
            offset: true
          },
          border: {
            color: '#999'
          },
          ticks : {
            maxRotation: 0,
            maxTicksLimit: 36,
            font: {
              size: 16,
              lineHeight: 1.5
            },
            callback: function(value) {
              let label = this.getLabelForValue(value);
              // Display only the month
              return label.split("/")[0];
            }
          }
        },
        secondX: {
          position: 'bottom',
          grid: {
            color: 'transparent',
          },
          border: {
              color: '#ccc'
          },
          ticks : {
            maxRotation: 0,
            font: {
              size: 16,
              weight: 'bold',
              lineHeight: 1.5
            },
            callback: function(value) {
              let label = this.getLabelForValue(value);
              // Display the year only once
              return label.split("/")[0] == '06' ? label.split("/")[1] : null;
            }
          }
        },
        y: {
          stacked: true,
          grid: {
              color: '#DFE6E9'
          },
          border: {
              color: '#999'
          },
          ticks : {
            font: {
              size: 16,
            }
          }
        }
      },
      plugins: {
        legend: {
          position: 'top',
          align:'end',
        },
        tooltip: {
          callbacks: {
            title: function(tooltipItems, data) {
                const l = tooltipItems[0].label,
                    monthsFr = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'],
                    month = monthsFr[parseInt(l.split("/")) - 1],
                    year = l.split("/")[1];
                return month + " " + year;
            },
          }
        },
        datalabels: {
          anchor: 'end',
          align: 'top',
          font: {
            size: 14,
            weight: 'bold'
          },
          formatter: (value, context) => {
            let sum = 0,
                last = null;

            context.chart.data.datasets.forEach((dataset, index) => {
              let siblings = context.chart.isDatasetVisible(index) && dataset.data.filter((option) => {
                return option.monthYear == value.monthYear
              })
              if (siblings && siblings[0] && siblings[0][this.displayMode]) {
                sum += siblings[0][this.displayMode];
                last = siblings[0]
              }
            })

            if (value == last) {
              return sum;
            } else {
              return '';
            }
          }
        },
        zoom: {
          pan: {
            enabled: true,
            mode: 'x',
            threshold: 5,
          },
          zoom: {
            wheel: {
              enabled: true,
            },
            pinch: {
              enabled: true
            },
            mode: 'x',
          },
          limits: {
            x: {
              minRange: 10
            }
          }
        }
      }
    };
  }

  switchMode() {
    this.chartOptions = {...this.chartOptions};
    /* The following instructions is added to be sure the chart is updated with the correct option change */
    this.$refs.chart.options.parsing.yAxisKey = this.displayMode
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
  padding: 5px 10px;
  height: 35px;
  border: 1px solid @pelorous;
  border-radius: 20px;
  font-weight: bold;
  font-size: 14px;
  font-family: inherit;
  background-color: @pelorous;
  color: white;
  cursor: pointer;
  max-width: 80vw;

  @media screen and (min-width: @desktop-min-width) {
    font-size: 16px;
  }

  option {
    color: black;
    background-color: white;
  }

  &:hover {
    background-color: @terra-cotta;
  }
}
.link {
  border-radius: 50px;

  font-style: normal;
  font-weight: bold;
  font-size: @fontsize-button;
  line-height: calc(@fontsize-button + @line-height-padding-x-large);

  border: 1px solid @pelorous;
  padding: @margin-x-small @margin-medium;
  margin-top: @margin-medium;

  background-color: @white-smoke;
  color: @pelorous;
  text-decoration: none;

  &:hover {
    background-color: @terra-cotta;
    color: white;
    border-color: white;
  }
}
</style>
