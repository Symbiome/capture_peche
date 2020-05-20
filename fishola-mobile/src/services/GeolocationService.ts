import {Lake} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';
import ReferentialService from '@/services/ReferentialService';

import { Plugins, GeolocationPosition } from '@capacitor/core';
const { Geolocation } = Plugins;

export class CoordsAndLake {

    constructor (
        public latitude:number,
        public longitude:number,
        public lake:Lake
    ) {
    }

}

export default class GeolocationService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static getPosition():Promise<GeolocationPosition> {

        let isNotSecured:boolean = (window.location.protocol == 'http:');

        if (isNotSecured) {

            return Promise.reject("Pas de Geolocation hors contexte sécurisé");

        } else {

            return new Promise<any>((resolve, reject) => {

                console.log("Before getCurrentPosition", new Date());

                let positionPromise:Promise<GeolocationPosition> = Geolocation.getCurrentPosition({
                    enableHighAccuracy: false,
                    maximumAge: 20,
                    timeout: 10000
                });
                positionPromise.then(
                    (position:GeolocationPosition) => {

                        console.log("Got currentPosition", new Date());
                        console.log("position", JSON.stringify(position));
                        resolve(position);
                        /* enableHighAccuracy: true
                        {
                            "coords":{
                                "latitude":47.1552435,
                                "longitude":-1.5262437,
                                "accuracy":21.48699951171875,
                                "altitude":77.79999542236328,
                                "altitudeAccuracy":2,
                                "speed":0.0025068377144634724,
                                "heading":315.3136291503906
                            },
                            "timestamp":1589819559643
                        }

                        */

                        /* enableHighAccuracy: false
                        {
                            "coords":{
                                "latitude":47.1552424,
                                "longitude":-1.5262452,
                                "accuracy":21.347000122070312,
                                "altitude":77.79999542236328,
                                "altitudeAccuracy":2,
                                "speed":0.012332512997090816,
                                "heading":274.6920471191406
                            },
                            "timestamp":1589819653984
                        }
                        */
                    },
                    (failure) => {
                        console.error("Unable to get current position", failure);
                        reject(failure);
                    })

                // if (!navigator.geolocation) {
                //     return Promise.reject("Geoloc non supportée par le navigateur");
                // }
                // return new Promise<any>((resolve, reject) => {
                //     navigator.geolocation.getCurrentPosition(resolve, reject);
                // });

                /* De base :

                    GeolocationPosition {
                        coords: { (GeolocationCoordinates)
                            accuracy: 22.951000213623047
                            altitude: 77.5
                            altitudeAccuracy: null
                            heading: 263.546142578125
                            latitude: 47.1552407
                            longitude: -1.5262464
                            speed: 0.0011239566374570131
                        }
                        timestamp: 1589884798399 (number)
                    }

                */
            });
        }

    }

    static getClosestLake():Promise<CoordsAndLake> {
        return new Promise<CoordsAndLake>((resolve, reject) => {
            Promise.all(
                [
                    ReferentialService.getLakes(),
                    GeolocationService.getPosition()
                ])
                .then(
                    (data:[Lake[], GeolocationPosition]) => {
                        let lakes:Lake[] = data[0];

                        if (!lakes || lakes.length == 0) {
                            reject();
                        } else {
                            let position:GeolocationPosition = data[1];

                            console.log("lakes", lakes);
                            console.log("position", position);

                            const latitude = position.coords.latitude;
                            const longitude = position.coords.longitude;

                            let closestLake = GeolocationService.chooseClosestLake(lakes, latitude, longitude);
                            let result = new CoordsAndLake(latitude, longitude, closestLake);
                            resolve(result);
                        }
                    },
                    reject
                );
        });
    }

    static computeDistance(latitudeA:number, longitudeA:number, latitudeB:number, longitudeB:number):number {
        let result = Math.sqrt(
            Math.pow(latitudeB - latitudeA, 2)
            +
            Math.pow(longitudeB - longitudeA, 2)
        );
        return result;
    }

    static chooseClosestLake(lakes:Lake[], latitude:number, longitude:number):Lake {
        let result:Lake = lakes[0];
        let minDistance = 999999;
        lakes.forEach((lake) => {
            let distance = this.computeDistance(lake.latitude, lake.longitude, latitude, longitude);
            if (distance < minDistance) {
                minDistance = distance;
                result = lake;
            }
            console.log(`Distance pour le ${lake.name}: ${distance}`);
        });
        console.log(`Winner is: ${result.name}`);
        return result;
    }

    static getClosestLakeMock():Promise<Lake> {
        return new Promise<Lake>((resolve, reject) => {
            let promises = [ReferentialService.getLakes()];
            Promise.all(promises)
                .then(
                    (data:any[]) => {
                        let lakes:Lake[] = data[0];

                        if (!lakes || lakes.length == 0) {
                            reject();
                        } else {
                            console.log("lakes", lakes);

                            // Les Sorinières
                            const latitude = 47.1464206;
                            const longitude = -1.5245706;

                            // // Seynod
                            // const latitude = 45.8345;
                            // const longitude = 6.0974;

                            // // Thonnon-les-bains
                            // const latitude = 46.3185;
                            // const longitude = 6.4764;

                            // // Alby-sur-Chéran
                            // const latitude = 45.7618;
                            // const longitude = 6.0205;

                            let result = GeolocationService.chooseClosestLake(lakes, latitude, longitude);
                            resolve(result);
                        }
                    },
                    reject
                );
        });
    }

}
