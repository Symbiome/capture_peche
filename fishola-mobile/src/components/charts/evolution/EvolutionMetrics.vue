<template>
    <div class="section shrinked">
        <h2 v-if="!global"> Personnal : todo </h2>
        <div v-if="speciesNameForLake.length == 0 || getYears().length == 0">
            Aucune donnée pour ce lac.
        </div>
        <div v-else>
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
  @Prop({default: false}) global: boolean;
  speciesNameForLake: SpeciesWithAlias[] = [];


  evolutionMetricsForLake: EvolutionMetricsForLake = {
    catchCountPerMonthAndSpecies: {},
    tripCountPerMonthAndSpecies: {}
  };

  mounted() {
    this.loadEvolutionData();
  }
  
  @Watch("lakeId")
  async loadEvolutionData() {
    if (this.lakeId) {
        this.speciesNameForLake = await ReferentialService.getSpeciesPlusCustom(this.lakeId)
        this.evolutionMetricsForLake = await DashboardService.loadGlobalEvolutionOrTimeout(this.lakeId);
    }
  }
  

    getYears() : string[] {
        return Object.keys(this.evolutionMetricsForLake.catchCountPerMonthAndSpecies);
    }

    getMonths(year: string) : Month[] {
        if (year && this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year]) {
        return Object.keys(this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year]) as Month[];
        }
        return [];
    }

    getSpecies(year: string, month: Month): string[] {
        if (year && this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year] && this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year][month]) {
            return Object.keys(this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year][month])
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
        this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year] 
        && this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year][month]
        && this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year][month][specieId]
        ) {
            return this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year][month][specieId]["false"];
        }
        return 0;
    }
   
   getKeptCount(year: string, month: Month, specieId: string) : number {
        if (year && specieId && month &&
        this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year] 
        && this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year][month]
        && this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year][month][specieId]
        ) {
            return this.evolutionMetricsForLake.catchCountPerMonthAndSpecies[year][month][specieId]["true"];
        }
        return 0;
    }

     getTripCount(year: string, month: Month, specieId: string) : number {
        if (year && specieId && month &&
        this.evolutionMetricsForLake.tripCountPerMonthAndSpecies[year] 
        && this.evolutionMetricsForLake.tripCountPerMonthAndSpecies[year][month]
        && this.evolutionMetricsForLake.tripCountPerMonthAndSpecies[year][month][specieId]
        ) {
            return this.evolutionMetricsForLake.tripCountPerMonthAndSpecies[year][month][specieId];
        }
        return 0;
    }
}
</script>

<style scoped lang="less">

</style>