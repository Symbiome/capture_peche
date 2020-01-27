import {Lake, Weather, SpeciesWithAlias, Technique, ReleasedFishState} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';

export default class ReferentialService extends AbstractFisholaService {

    static instance?:ReferentialService;

    constructor () {
        super();
    }

    static getInstance():ReferentialService {
        if (!this.instance) {
            console.log("Pas encore d'instance partagée, on la créé");
            this.instance = new ReferentialService();
        }
        return this.instance;
    }

    static getLakes(callback:(lakes:Lake[])=>any) {
        this.getInstance().backendGet('/v1/referential/lakes', callback);
    }

    static getLakesIndex(callback:(lakes:Map<string, Lake>)=>any) {
        ReferentialService.getLakes((lakes:Lake[]) => {
            let result = new Map<string, Lake>();
            lakes.forEach((lake:Lake) => {
                result.set(lake.id, lake);
            })
            callback(result);
        });
    }

    // static getAllSpecies(callback:(result:Species[])=>any) {
    //     this.getInstance().backendGet('/v1/referential/species', callback);
    // }

    static getSpeciesPerLake(callback:(result:Map<string, SpeciesWithAlias[]>)=>any) {
        this.getInstance().backendGet('/v1/referential/species-per-lake', (map) => {
            let someMap = new Map<string, SpeciesWithAlias[]>();
            let lakeIds:string[] = Object.keys(map);
            lakeIds.forEach(lakeId => {
                someMap.set(lakeId, map[lakeId]);
            })
            callback(someMap);
        });
    }

    static getSpecies(lakeId:string, callback:(result:SpeciesWithAlias[])=>any) {
        this.getInstance().backendGet('/v1/referential/species-per-lake', (map) => {
            let species = map[lakeId];
            callback(species);
        });
    }

    static getWeathers(callback:(weathers:Weather[])=>any) {
        this.getInstance().backendGet('/v1/referential/weathers', callback);
    }

    static getTechniques(callback:(techniques:Technique[])=>any) {
        this.getInstance().backendGet('/v1/referential/techniques', callback);
    }

    static getReleasedFishStates(callback:(states:ReleasedFishState[])=>any) {
        this.getInstance().backendGet('/v1/referential/released-fish-states', callback);
    }

    static getTripTypes(callback:(result:any[])=>any) {
        let types = [
            {id: 'Border', name: 'Pêche du bord'},
            {id: 'Craft', name: 'Depuis une embarcation'}
        ];
        callback(types);
    }

    static getLakesWeathersTripTypesAndSpecies(callback:(ls:Lake[],ws:Weather[],tts:any[],ss:Map<string, SpeciesWithAlias[]>)=>any) {
        // FIXME AThimel 23/12/2019 Utiliser des promises
        ReferentialService.getLakes((lakes:Lake[]) => {
            ReferentialService.getWeathers((weathers:Weather[]) => {
                ReferentialService.getSpeciesPerLake((species:Map<string, SpeciesWithAlias[]>) => {
                    ReferentialService.getTripTypes((tripTypes:any[]) => {
                        callback(lakes, weathers, tripTypes, species);
                    });
                });
            });
        });
    }

    static getLakesAndTripTypes(callback:(ls:Lake[],tts:any[])=>any) {
        // FIXME AThimel 23/12/2019 Utiliser des promises
        ReferentialService.getLakes((lakes:Lake[]) => {
            ReferentialService.getTripTypes((tripTypes:any[]) => {
                callback(lakes, tripTypes);
            });
        });
    }

    static getSpeciesTechniquesAndReleasedFishStates(lakeId:string, callback:(species:SpeciesWithAlias[],techniques:Technique[],states:ReleasedFishState[])=>any) {
        // FIXME AThimel 23/12/2019 Utiliser des promises
        ReferentialService.getSpecies(lakeId, (species:SpeciesWithAlias[]) => {
            ReferentialService.getTechniques((techniques:Technique[]) => {
                ReferentialService.getReleasedFishStates((states:ReleasedFishState[]) => {
                    callback(species, techniques, states);
                });
            });
        });
    }

}
