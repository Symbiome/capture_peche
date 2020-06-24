/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
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

export class LakesWeathersTripTypesSpeciesAndTechniques {
    constructor (
        public lakes:Lake[],
        public weathers:Weather[],
        public tripTypes:any[],
        public species:Map<string, SpeciesWithAlias[]>,
        public techniques:Technique[]) {
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
            this.backendGetWithCache('/v1/referential/species-per-lake')
                .then(
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

    static getSpeciesWithoutLake():Promise<SpeciesWithAlias[]> {
        return this.backendGetWithCache('/v1/referential/species');
    }

    static getSpeciesCustom():Promise<SpeciesWithAlias[]> {
        return this.backendGetWithCache('/v1/referential/species-custom');
    }

    static getSpeciesCustomNoCache():Promise<SpeciesWithAlias[]> {
        return this.backendGet('/v1/referential/species-custom');
    }

    static getAllSpecies():Promise<SpeciesWithAlias[]> {
        return new Promise<SpeciesWithAlias[]>((resolve, reject) => {
            Promise
                .all([
                        ReferentialService.getSpeciesWithoutLake(),
                        ReferentialService.getSpeciesCustom()
                    ])
                .then((data:[SpeciesWithAlias[], SpeciesWithAlias[]]) => {
                    let result:SpeciesWithAlias[] = [];

                    data[0].forEach((value) => result.push(value));
                    data[1].forEach((value) => result.push(value));

                    resolve(result);
                },
                reject);
        });
    }

    static getAllSpeciesNoCache():Promise<SpeciesWithAlias[]> {
        return new Promise<SpeciesWithAlias[]>((resolve, reject) => {
            Promise
                .all([
                        ReferentialService.getSpeciesWithoutLake(),
                        ReferentialService.getSpeciesCustomNoCache()
                    ])
                .then((data:[SpeciesWithAlias[], SpeciesWithAlias[]]) => {
                    let result:SpeciesWithAlias[] = [];

                    data[0].forEach((value) => result.push(value));
                    data[1].forEach((value) => result.push(value));

                    resolve(result);
                },
                reject);
        });
    }

    static clearSpeciesCustomCache() {
        this.clearCache('/v1/referential/species-custom');
    }

    static getSpeciesPerLakePlusCustom():Promise<Map<string, SpeciesWithAlias[]>> {
        return new Promise<Map<string, SpeciesWithAlias[]>>((resolve, reject) => {
            Promise
                .all([ReferentialService.getSpeciesPerLake(), ReferentialService.getSpeciesCustom()])
                .then((data:[Map<string, SpeciesWithAlias[]>, SpeciesWithAlias[]]) => {
                    let result:Map<string, SpeciesWithAlias[]> = new Map<string, SpeciesWithAlias[]>();

                    let custom:SpeciesWithAlias[] = data[1];

                    let perLake = data[0];
                    perLake.forEach((value, lakeId) => {
                        if (lakeId != "offlineMarker") {
                            let lakeSpecies:SpeciesWithAlias[] = [];
                            value.forEach((s) => lakeSpecies.push(s));
                            // Quel que soit le lac, on ajoute les espèces custom à la liste
                            custom.forEach((s) => lakeSpecies.push(s));
                            result.set(lakeId, lakeSpecies);
                        }
                    });

                    resolve(result);
                },
                reject);
        });
    }

    static getSpecies(lakeId:string):Promise<SpeciesWithAlias[]> {
        return new Promise<SpeciesWithAlias[]>((resolve, reject) => {
            this.getSpeciesPerLake().then(
                (map) => {
                    let species = map.get(lakeId);
                    resolve(species);
                },
                reject);
        });
    }

    static getSpeciesPlusCustom(lakeId:string):Promise<SpeciesWithAlias[]> {
        return new Promise<SpeciesWithAlias[]>((resolve, reject) => {
            this.getSpeciesPerLakePlusCustom().then(
                (map) => {
                    let species = map.get(lakeId);
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

    static getTechniquesIndex():Promise<Map<string, Technique>> {
        return new Promise<Map<string, Technique>>((resolve, reject) => {
            ReferentialService.getTechniques()
                .then((techniques:Technique[]) => {
                    let result = new Map<string, Technique>();
                    techniques.forEach((t:Technique) => {
                        result.set(t.id, t);
                    })
                    resolve(result);
                },
                reject);
        });
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

    static getLakesWeathersTripTypesSpeciesAndTechniques():Promise<LakesWeathersTripTypesSpeciesAndTechniques> {
        return new Promise<LakesWeathersTripTypesSpeciesAndTechniques>((resolve, reject) => {
            Promise
                .all([
                    ReferentialService.getLakes(),
                    ReferentialService.getWeathers(),
                    ReferentialService.getTripTypes(),
                    ReferentialService.getSpeciesPerLakePlusCustom(),
                    ReferentialService.getTechniques()])
                .then(
                    (data:[Lake[], Weather[], any[], Map<string, SpeciesWithAlias[]>, Technique[]]) => {
                        let result:LakesWeathersTripTypesSpeciesAndTechniques = new LakesWeathersTripTypesSpeciesAndTechniques(data[0], data[1], data[2], data[3], data[4]);
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

    static getSpeciesAndTechniques(lakeId?:string):Promise<SpeciesWithAliasAndTechnique> {
        return new Promise<SpeciesWithAliasAndTechnique>((resolve, reject) => {
            let speciesPromise = lakeId ? ReferentialService.getSpeciesPlusCustom(lakeId) : ReferentialService.getAllSpecies();
            Promise
                .all([speciesPromise, ReferentialService.getTechniques()])
                .then(
                    (data:[SpeciesWithAlias[], Technique[]]) => {
                        let result:SpeciesWithAliasAndTechnique = new SpeciesWithAliasAndTechnique(data[0], data[1]);
                        resolve(result);
                    },
                    reject);
        });
    }

    static prepareCaches():Promise<void> {
        let allPromises:Promise<void>[] = [
            this.prepareCache('/v1/referential/lakes'),
            this.prepareCache('/v1/referential/species-per-lake'),
            this.prepareCache('/v1/referential/species'),
            this.prepareCache('/v1/referential/species-custom'),
            this.prepareCache('/v1/referential/weathers'),
            this.prepareCache('/v1/referential/techniques'),
            this.prepareCache('/v1/referential/released-fish-states')
        ];
        return new Promise<void>((resolve, reject) => {
            Promise.all(allPromises).then(
                () => resolve()
                ,reject
            );
        });
    }

}
