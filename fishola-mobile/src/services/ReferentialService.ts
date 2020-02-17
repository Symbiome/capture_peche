import {Lake, Weather, SpeciesWithAlias, Technique, ReleasedFishState} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';

export class LakesAndTripTypes {
    constructor (
        public lakes:Lake[],
        public tripTypes:any[]) {
    }
}

export class SpeciesWithAliasAndTechnique {
    constructor (
        public species:SpeciesWithAlias[],
        public techniques:Technique[]) {
    }
}

export class SpeciesTechniquesAndReleasedFishStates {
    constructor (
        public species:SpeciesWithAlias[],
        public techniques:Technique[],
        public states:ReleasedFishState[]) {
    }
}

export class LakesWeathersTripTypesAndSpecies {
    constructor (
        public lakes:Lake[],
        public weathers:Weather[],
        public tripTypes:any[],
        public species:Map<string, SpeciesWithAlias[]>) {
    }
}

export default class ReferentialService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static getLakes():Promise<Lake[]> {
        return this.backendGetWithCache('/v1/referential/lakes');
    }

    static getLakesIndex():Promise<Map<string, Lake>> {
        return new Promise<Map<string, Lake>>((resolve, reject) => {
            ReferentialService.getLakes()
                .then((lakes:Lake[]) => {
                    let result = new Map<string, Lake>();
                    lakes.forEach((lake:Lake) => {
                        result.set(lake.id, lake);
                    })
                    resolve(result);
                },
                reject);
        });
    }

    static getSpeciesPerLake():Promise<Map<string, SpeciesWithAlias[]>> {
        return new Promise<Map<string, SpeciesWithAlias[]>>((resolve, reject) => {
            this.backendGet(
                '/v1/referential/species-per-lake',
                (map) => {
                    let someMap = new Map<string, SpeciesWithAlias[]>();
                    let lakeIds:string[] = Object.keys(map);
                    lakeIds.forEach(lakeId => {
                        someMap.set(lakeId, map[lakeId]);
                    });
                    resolve(someMap);
                },
                reject);
        });
    }

    static getSpecies(lakeId:string):Promise<SpeciesWithAlias[]> {
        return new Promise<SpeciesWithAlias[]>((resolve, reject) => {
            this.backendGet(
                '/v1/referential/species-per-lake',
                (map) => {
                    let species = map[lakeId];
                    resolve(species);
                },
                reject);
        });
    }

    static getWeathers():Promise<Weather[]> {
        return this.backendGetWithCache('/v1/referential/weathers');
    }

    static getTechniques():Promise<Technique[]> {
        return this.backendGetWithCache('/v1/referential/techniques');
    }

    static getReleasedFishStates():Promise<ReleasedFishState[]> {
        return this.backendGetWithCache('/v1/referential/released-fish-states');
    }

    static getTripTypes():Promise<any[]> {
        let types = [
            {id: 'Border', name: 'Pêche du bord'},
            {id: 'Craft', name: 'Depuis une embarcation'}
        ];
        return Promise.resolve(types);
    }

    static getLakesWeathersTripTypesAndSpecies():Promise<LakesWeathersTripTypesAndSpecies> {
        return new Promise<LakesWeathersTripTypesAndSpecies>((resolve, reject) => {
            Promise
                .all([ReferentialService.getLakes(), ReferentialService.getWeathers(), ReferentialService.getTripTypes(), ReferentialService.getSpeciesPerLake()])
                .then(
                    (data:[Lake[], Weather[], any[], Map<string, SpeciesWithAlias[]>]) => {
                        let result:LakesWeathersTripTypesAndSpecies = new LakesWeathersTripTypesAndSpecies(data[0], data[1], data[2], data[3]);
                        resolve(result);
                    },
                    reject);
        });
    }

    static getLakesAndTripTypes():Promise<LakesAndTripTypes> {
        return new Promise<LakesAndTripTypes>((resolve, reject) => {
            Promise
                .all([ReferentialService.getLakes(), ReferentialService.getTripTypes()])
                .then(
                    (data:[Lake[], any[]]) => {
                        let result:LakesAndTripTypes = new LakesAndTripTypes(data[0], data[1]);
                        resolve(result);
                    },
                    reject);
        });
    }

    static getSpeciesTechniquesAndReleasedFishStates(lakeId:string):Promise<SpeciesTechniquesAndReleasedFishStates> {
        return new Promise<SpeciesTechniquesAndReleasedFishStates>((resolve, reject) => {
            Promise
                .all([ReferentialService.getSpecies(lakeId), ReferentialService.getTechniques(), ReferentialService.getReleasedFishStates()])
                .then(
                    (data:[SpeciesWithAlias[], Technique[], ReleasedFishState[]]) => {
                        let result:SpeciesTechniquesAndReleasedFishStates = new SpeciesTechniquesAndReleasedFishStates(data[0], data[1], data[2]);
                        resolve(result);
                    },
                    reject);
        });
    }

    static getSpeciesAndTechniques(lakeId:string):Promise<SpeciesWithAliasAndTechnique> {
        return new Promise<SpeciesWithAliasAndTechnique>((resolve, reject) => {
            Promise
                .all([ReferentialService.getSpecies(lakeId), ReferentialService.getTechniques()])
                .then(
                    (data:[SpeciesWithAlias[], Technique[]]) => {
                        let result:SpeciesWithAliasAndTechnique = new SpeciesWithAliasAndTechnique(data[0], data[1]);
                        resolve(result);
                    },
                    reject);
        });
    }

}
