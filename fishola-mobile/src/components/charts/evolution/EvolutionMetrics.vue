<template>
    <div>
        <h2>Evolution</h2>
        <div v-if="speciesNameForLake.length == 0 || getYears().length == 0">
            Aucune donnée pour ce lac.
        </div>
        <div v-else>
            <div  v-for="year in getYears()" :key="year">
                <b>{{year}} ( {{ getSpecies(year).length }} )</b><br/>
                 <p v-for="specie in getSpecies(year)" :key="specie"> 
                    - Espece {{ getSpecieNameForLake(specie) }} <br/>
                    <ul v-for="month in getMonths(year, specie)" :key="month">
                        {{ month}} : 
                        Données brutes : {{ getValue(year, specie, month) }}
                        <br/>
                        Nombre de prises : {{ getCount(year, specie, month)}}
                    </ul>
                 </p>
                 
            </div>
        </div>
    </div>
</template>

<script lang="ts">

import { EvolutionMetricsForLake, Month, SpeciesWithAlias } from '@/pojos/BackendPojos';
import ReferentialService from '@/services/ReferentialService';
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class EvolutionMetricsView extends Vue {
    @Prop() evolutionMetricsForLake: EvolutionMetricsForLake;
    @Prop() lakeId: string;


  speciesNameForLake: SpeciesWithAlias[] = [];

  mounted() {
    ReferentialService.getSpecies(this.lakeId).then(result => {
        this.speciesNameForLake = result;
    })
  };
  

    getYears() : string[] {
        return Object.keys(this.evolutionMetricsForLake.monthlySizesPerMaillageAndYear);
    }

    getSpecies(year: string): string[] {
        if (year && this.evolutionMetricsForLake.monthlySizesPerMaillageAndYear[year]) {
            return Object.keys(this.evolutionMetricsForLake.monthlySizesPerMaillageAndYear[year])
        }
        return [];
    }

    getMonths(year: string, specieId: string) : Month[] {
         if (year && specieId && this.evolutionMetricsForLake.monthlySizesPerMaillageAndYear[year] && this.evolutionMetricsForLake.monthlySizesPerMaillageAndYear[year][specieId]) {
            return Object.keys(this.evolutionMetricsForLake.monthlySizesPerMaillageAndYear[year][specieId]) as Month[];
        }
        return [];
    }

    getSpecieNameForLake(specieId: string) {
        const specie = this.speciesNameForLake.find(s => {
            return s.id == specieId;
        });
        return specie ? (specie.alias ?? specie.name) : "";
    }

    getCount(year: string, specieId: string, month: Month) : number {
        const value = this.getValue(year, specieId, month);
        return (value["MAILLEE"] ? parseFloat(Object.keys(value["MAILLEE"])[0]) : 0)
        + (value["NON_MAILLEE"] ? parseFloat(Object.keys(value["NON_MAILLEE"])[0]) : 0)
        + (value["NON_DEFINI"] ? parseFloat(Object.keys(value["NON_DEFINI"])[0]) : 0)
    }

    getSize(year: string, specieId: string, month: Month) {
        
    }

    getValue(year: string, specieId: string, month: Month) {
        if (year && specieId && month &&
        this.evolutionMetricsForLake.monthlySizesPerMaillageAndYear[year] 
        && this.evolutionMetricsForLake.monthlySizesPerMaillageAndYear[year][specieId]
        && this.evolutionMetricsForLake.monthlySizesPerMaillageAndYear[year][specieId][month]
        ) {
        return this.evolutionMetricsForLake.monthlySizesPerMaillageAndYear[year][specieId][month]
        }
        return {};
    }
}
</script>

<style scoped lang="less">

</style>