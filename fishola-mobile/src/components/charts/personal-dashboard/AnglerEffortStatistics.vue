<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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
<!--
  Statistiques d'effort de pêche (#210) : sessions et heures par mois, temps
  par technique, CPUE par espèce, contribution au secteur. Mêmes filtres que
  le tableau de bord (année, plan d'eau).
  -->
<template>
  <div>
    <div class="section">
      <div class="shrinked">
        <h2><i class="icon-fishing" />Sessions et heures de pêche par mois</h2>
      </div>
      <div class="not-enough-data" v-if="!loading && !hasMonthlyEffort">
        <span>Pas assez de données</span>
      </div>
      <div class="shrinked effort-chart" v-if="hasMonthlyEffort">
        <Bar :data="monthlyChartData" :options="monthlyChartOptions" />
      </div>
    </div>

    <div class="section">
      <div class="shrinked">
        <h2><i class="icon-fishing" />Temps passé par technique</h2>
      </div>
      <div class="not-enough-data" v-if="!loading && techniqueSlices.length == 0">
        <span>Pas assez de données</span>
      </div>
      <div class="shrinked effort-chart" v-if="techniqueSlices.length > 0">
        <Pie :data="techniqueChartData" :options="techniqueChartOptions" />
      </div>
    </div>

    <div class="section">
      <div class="shrinked">
        <h2><i class="icon-fish" />Prises par heure de pêche (CPUE)</h2>
        <p class="explanation" v-if="stats">
          Nombre de poissons capturés d'une espèce rapporté à vos heures de pêche
          ({{ totalHoursRounded }} h sur la période). Calculé à partir de
          {{ stats.cpueMinCatches }} prises d'une même espèce.
        </p>
      </div>
      <div class="not-enough-data" v-if="!loading && cpueRows.length == 0">
        <span>Pas assez de données</span>
      </div>
      <ul class="shrinked cpue-list" v-if="cpueRows.length > 0">
        <li v-for="row in cpueRows" :key="row.speciesId">
          <span class="cpue-species">{{ row.label }}</span>
          <span v-if="row.catchesPerHour !== undefined" class="cpue-value">
            {{ row.catchesPerHour }} prise(s) / h
          </span>
          <span v-else class="cpue-locked">
            {{ row.catchesCount }} / {{ stats.cpueMinCatches }} prises : encore
            {{ stats.cpueMinCatches - row.catchesCount }} pour débloquer la CPUE
          </span>
        </li>
      </ul>
    </div>

    <div class="section">
      <div class="shrinked">
        <h2><i class="icon-lake" />Ma contribution sur ce secteur</h2>
      </div>
      <div class="not-enough-data" v-if="!loading && !selectedLakeId">
        <span>Sélectionnez un plan d'eau pour voir votre part des sorties et des prises déclarées.</span>
      </div>
      <div class="not-enough-data" v-else-if="!loading && stats && !stats.sectorContribution">
        <span>
          Pas encore assez de pêcheurs sur ce secteur (au moins {{ stats.sectorMinAnglers }})
          pour afficher votre contribution.
        </span>
      </div>
      <div class="shrinked contribution" v-if="selectedLakeId && contribution">
        <div class="contribution-row">
          <div class="contribution-label">
            Sorties : <b>{{ tripsShare }} %</b>
            ({{ contribution.myTripsCount }} sur {{ contribution.totalTripsCount }})
          </div>
          <div class="contribution-bar"><div :style="{ width: tripsShare + '%' }" /></div>
        </div>
        <div class="contribution-row">
          <div class="contribution-label">
            Prises : <b>{{ catchesShare }} %</b>
            ({{ contribution.myCatchesCount }} sur {{ contribution.totalCatchesCount }})
          </div>
          <div class="contribution-bar"><div :style="{ width: catchesShare + '%' }" /></div>
        </div>
        <p class="explanation">
          Sur l'ensemble des {{ contribution.anglersCount }} pêcheurs ayant déclaré une sortie sur ce
          plan d'eau pour la période.
        </p>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {
  AnglerEffortStatistics as EffortStatistics,
  Month,
  SectorContribution,
  SpeciesWithAlias,
  Technique,
} from "@/pojos/BackendPojos";
import AnglerEffortHelpers, { CpueRow, TechniqueSlice } from "@/services/AnglerEffortHelpers";
import Constants from "@/services/Constants";
import DashboardService from "@/services/DashboardService";
import ReferentialService from "@/services/ReferentialService";
import { Component, Prop, Vue, Watch } from "vue-property-decorator";

import {
  Chart as ChartJS,
  ArcElement,
  BarElement,
  CategoryScale,
  Legend,
  LinearScale,
  Tooltip,
} from "chart.js";
import { Bar, Pie } from "vue-chartjs";

ChartJS.register(ArcElement, BarElement, CategoryScale, LinearScale, Legend, Tooltip);

const MONTHS: Month[] = [
  "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
  "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER",
];

const PIE_COLORS = [
  "#1E9BC4", "#E17055", "#1AB7DA", "#69db7c", "#ffd43b",
  "#cc5de8", "#faa2c1", "#1971c2", "#2f9e44", "#f08c00",
];

@Component({
  components: { Bar, Pie },
})
export default class AnglerEffortStatistics extends Vue {
  @Prop() year: number;
  @Prop() selectedLakeId: string;
  @Prop() species: SpeciesWithAlias[];

  stats: EffortStatistics | null = null;
  techniquesIndex: Map<string, Technique> = new Map();
  loading = true;
  private loadSeq = 0;

  mounted() {
    this.loadStatistics();
  }

  @Watch("year")
  @Watch("selectedLakeId")
  async loadStatistics() {
    const seq = ++this.loadSeq;
    this.loading = true;
    try {
      const [stats, techniquesIndex] = await Promise.all([
        DashboardService.loadEffortStatisticsOrTimeout(this.year, this.selectedLakeId || ""),
        ReferentialService.getTechniquesIndex(),
      ]);
      if (seq === this.loadSeq) {
        this.stats = stats;
        this.techniquesIndex = techniquesIndex;
      }
    } catch (e) {
      console.error("Statistiques d'effort indisponibles", e);
      if (seq === this.loadSeq) {
        this.stats = null;
      }
    } finally {
      if (seq === this.loadSeq) {
        this.loading = false;
      }
    }
  }

  get hasMonthlyEffort(): boolean {
    return !!this.stats && Object.keys(this.stats.monthlyEffort).length > 0;
  }

  get totalHoursRounded(): number {
    return this.stats ? AnglerEffortHelpers.round(this.stats.totalHours, 1) : 0;
  }

  get monthlyChartData(): any {
    const series = AnglerEffortHelpers.monthlySeries(this.stats ? this.stats.monthlyEffort : {}, MONTHS);
    return {
      labels: Constants.MONTHS,
      datasets: [
        { label: "Sessions", data: series.trips, backgroundColor: "#1E9BC4", yAxisID: "trips" },
        { label: "Heures", data: series.hours, backgroundColor: "#E17055", yAxisID: "hours" },
      ],
    };
  }

  get monthlyChartOptions(): any {
    const grid = { color: ChartJS.defaults.borderColor as string };
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { position: "top", align: "end" } },
      scales: {
        trips: { position: "left", beginAtZero: true, ticks: { precision: 0 }, grid,
          title: { display: true, text: "Sessions" } },
        hours: { position: "right", beginAtZero: true, grid: { drawOnChartArea: false },
          title: { display: true, text: "Heures" } },
      },
    };
  }

  get techniqueSlices(): TechniqueSlice[] {
    if (!this.stats) {
      return [];
    }
    return AnglerEffortHelpers.techniqueSlices(this.stats.hoursPerTechnique, (id) => {
      const technique = this.techniquesIndex.get(id);
      return technique ? technique.name : "Autre technique";
    });
  }

  get techniqueChartData(): any {
    return {
      labels: this.techniqueSlices.map((slice) => slice.label),
      datasets: [{
        data: this.techniqueSlices.map((slice) => slice.hours),
        backgroundColor: this.techniqueSlices.map((_, index) => PIE_COLORS[index % PIE_COLORS.length]),
      }],
    };
  }

  get techniqueChartOptions(): any {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: "right" },
        tooltip: { callbacks: { label: (context: any) => `${context.label} : ${context.raw} h` } },
        datalabels: { display: false },
      },
    };
  }

  get cpueRows(): CpueRow[] {
    if (!this.stats) {
      return [];
    }
    return AnglerEffortHelpers.cpueRows(this.stats.cpuePerSpecies, (id) => {
      const species = (this.species || []).find((s) => s.id === id);
      return species ? species.alias || species.name : "Espèce inconnue";
    });
  }

  get contribution(): SectorContribution | undefined {
    return this.stats ? this.stats.sectorContribution : undefined;
  }

  get tripsShare(): number {
    return this.contribution
      ? AnglerEffortHelpers.sharePercent(this.contribution.myTripsCount, this.contribution.totalTripsCount)
      : 0;
  }

  get catchesShare(): number {
    return this.contribution
      ? AnglerEffortHelpers.sharePercent(this.contribution.myCatchesCount, this.contribution.totalCatchesCount)
      : 0;
  }
}
</script>

<style scoped lang="less">
.effort-chart {
  position: relative;
  height: 250px;
}

.explanation {
  font-size: @fontsize-small-paragraph;
  color: @pale-sky;
}

.cpue-list {
  list-style: none;
  padding: 0;

  li {
    display: flex;
    justify-content: space-between;
    gap: @margin-small;
    padding: @vertical-margin-xx-small 0;
    border-bottom: 1px solid @solitude;
  }

  .cpue-species {
    font-weight: bold;
  }

  .cpue-value {
    color: @pelorous;
    font-weight: bold;
  }

  .cpue-locked {
    color: @pale-sky;
    font-style: italic;
    text-align: right;
  }
}

.contribution-row {
  margin-bottom: @vertical-margin-small;
}

.contribution-bar {
  height: 14px;
  border-radius: 7px;
  background: @solitude;
  overflow: hidden;

  & > div {
    height: 100%;
    border-radius: 7px;
    background: @pelorous;
  }
}
</style>
