<template>
    <div class="section shrinked">
        <div v-if="speciesNameForLake.length == 0 || getYears().length == 0">
            Aucune donnée pour ce lac.
        </div>
        <div v-else>
            <h3>TODO Graphique 1 <span v-if="onlyShowUserStats">(stats personnelles)</span><span v-else>(stats globales)</span></h3>
            <p>Par mois de chaque année, par espèce (filtrable) : nombre d'individus capturés (avec distingo relaché/gardé)</p>
            <h3>TODO Graphique 2 <span v-if="onlyShowUserStats">(stats personnelles)</span><span v-else>(stats globales)</span></h3>
            <p>Par mois de chaque année, par espèce (filtrable) : nombre de sortie effectuées avec au moins une prise de l'espèce</p>
            <h3>TODO ajouter lien vers la dashboard pole ECLA </h3>
            <h2>Données brutes : </h2>
            <div  v-for="year in getYears()" :key="year">
                <b>{{year}} </b><br/>
                 <p v-for="month in getMonths(year)" :key="month"> 
                    - {{ month}}  ( {{ getSpecies(year, month).length }} )
                    <ul v-for="specie in getSpecies(year, month)" :key="specie">   
                    - Espece {{ getSpecieNameForLake(specie) }} <br/>
                        * Relâchés : {{ getReleasedCount(year, month, specie) }} <br/>
                       * Conservés: {{ getKeptCount(year, month, specie) }} <br/>
                        * Nombre de sorties : {{ getTripCount(year, month, specie) }} <br/>
                    </ul>
                 </p>
                 
            </div>
        </div>
    </div>
</template>

<script lang="ts">

import { EvolutionMetricsForLake, Month, SpeciesWithAlias } from '@/pojos/BackendPojos';
import DashboardService from '@/services/DashboardService';
import ReferentialService from '@/services/ReferentialService';
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';

@Component
export default class EvolutionMetricsView extends Vue {
  @Prop() lakeId: string;
  @Prop({default: false}) onlyShowUserStats: boolean;
  speciesNameForLake: SpeciesWithAlias[] = [];


  evolutionMetrics: EvolutionMetricsForLake = {
    catchCountPerMonthAndSpecies: {},
    tripCountPerMonthAndSpecies: {}
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
    }
  }
  

    getYears() : string[] {
        return Object.keys(this.evolutionMetrics.catchCountPerMonthAndSpecies);
    }

    getMonths(year: string) : Month[] {
        if (year && this.evolutionMetrics.catchCountPerMonthAndSpecies[year]) {
        return Object.keys(this.evolutionMetrics.catchCountPerMonthAndSpecies[year]) as Month[];
        }
        return [];
    }

    getSpecies(year: string, month: Month): string[] {
        if (year && this.evolutionMetrics.catchCountPerMonthAndSpecies[year] && this.evolutionMetrics.catchCountPerMonthAndSpecies[year][month]) {
            return Object.keys(this.evolutionMetrics.catchCountPerMonthAndSpecies[year][month])
        }
        return [];
    }

    getSpecieNameForLake(specieId: string) {
        const specie = this.speciesNameForLake.find(s => {
            return s.id == specieId;
        });
        return specie ? (specie.alias ?? specie.name) : specieId;
    }

    getReleasedCount(year: string, month: Month, specieId: string) : number {
        if (year && specieId && month &&
        this.evolutionMetrics.catchCountPerMonthAndSpecies[year] 
        && this.evolutionMetrics.catchCountPerMonthAndSpecies[year][month]
        && this.evolutionMetrics.catchCountPerMonthAndSpecies[year][month][specieId]
        ) {
            return this.evolutionMetrics.catchCountPerMonthAndSpecies[year][month][specieId]["false"];
        }
        return 0;
    }
   
   getKeptCount(year: string, month: Month, specieId: string) : number {
        if (year && specieId && month &&
        this.evolutionMetrics.catchCountPerMonthAndSpecies[year] 
        && this.evolutionMetrics.catchCountPerMonthAndSpecies[year][month]
        && this.evolutionMetrics.catchCountPerMonthAndSpecies[year][month][specieId]
        ) {
            return this.evolutionMetrics.catchCountPerMonthAndSpecies[year][month][specieId]["true"];
        }
        return 0;
    }

     getTripCount(year: string, month: Month, specieId: string) : number {
        if (year && specieId && month &&
        this.evolutionMetrics.tripCountPerMonthAndSpecies[year] 
        && this.evolutionMetrics.tripCountPerMonthAndSpecies[year][month]
        && this.evolutionMetrics.tripCountPerMonthAndSpecies[year][month][specieId]
        ) {
            return this.evolutionMetrics.tripCountPerMonthAndSpecies[year][month][specieId];
        }
        return 0;
    }
}
</script>

<style scoped lang="less">

</style>