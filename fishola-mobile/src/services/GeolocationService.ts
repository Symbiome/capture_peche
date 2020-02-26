import {Lake} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';
import ReferentialService from '@/services/ReferentialService';

export default class GeolocationService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static getPosition():Promise<any> {
        if (!navigator.geolocation) {
            return Promise.reject("Geoloc non supportée par le navigateur");
        }
        return new Promise<any>((resolve, reject) => {
            navigator.geolocation.getCurrentPosition(resolve, reject);
        });
    }

    static getClosestLake():Promise<Lake> {
        return new Promise<Lake>((resolve, reject) => {
            let promises = [ReferentialService.getLakes(), GeolocationService.getPosition()];
            Promise.all(promises)
                .then(
                    (data:any[]) => {
                        let lakes:Lake[] = data[0];

                        if (!lakes || lakes.length == 0) {
                            reject();
                        } else {
                            let position = data[1];

                            console.log("lakes", lakes);
                            console.log("position", position);

                            const latitude = position.coords.latitude;
                            const longitude = position.coords.longitude;

                            let result = GeolocationService.chooseClosestLake(lakes, latitude, longitude);
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
