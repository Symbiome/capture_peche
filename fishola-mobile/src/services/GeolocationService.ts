import {Lake} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';
import ReferentialService from '@/services/ReferentialService';

import { Plugins, GeolocationPosition, CallbackID, DeviceInfo } from '@capacitor/core';
const { Geolocation } = Plugins;
const { Device } = Plugins;

export class CoordsAndLake {

    constructor (
        public latitude:number,
        public longitude:number,
        public lake:Lake
    ) {
    }

}

export default class GeolocationService extends AbstractFisholaService {

    static watchId?:CallbackID;
    static latestPosition?:GeolocationPosition;
    static latestError?:any;

    constructor () {
        super();
    }

    static canUsePosition():Promise<void> {
        return new Promise<void>((resolve, reject) => {

            Device.getInfo().then(
                (device:DeviceInfo) => {

                    let isNotSecured:boolean =
                            device.platform == "web"
                            && window.location.protocol == 'http:';
                    let isDesktopBrowser:boolean =
                            device.platform == "web"
                            && device.operatingSystem != "android"
                            && device.operatingSystem != "ios";

                    console.info("isNotSecured", isNotSecured);
                    console.info("isDesktopBrowser", isDesktopBrowser);
                    console.info("device", device);

                    if (isNotSecured) {

                         reject("Pas de Geolocation hors contexte sécurisé");

                    } else if (isDesktopBrowser) {

                        reject("Pas de Geolocation sans smartphone");

                    } else {
                        resolve();
                    }
                },
                reject);

        });
    }

    static startWatchingPosition():Promise<boolean> {
        return new Promise<boolean>((resolve, reject) => {
            this.canUsePosition()
                .then(
                    () => {
                        if (GeolocationService.watchId) {
                            console.error("Il y a déjà un watcher en cours");
                            resolve(false);
                        } else {
                            let options = {
                                enableHighAccuracy: false,
                                maximumAge: 20,
                                timeout: 3000
                            };
                            let watchId:CallbackID = Geolocation.watchPosition(options, this.receivePosition);
                            GeolocationService.watchId = watchId;
                            resolve(true);
                        }
                    },
                    reject
                );
        });
    }

    static stopWatchingPosition() {

        console.debug("About to stop watching position", GeolocationService.watchId);
        if (GeolocationService.watchId) {
            Geolocation.clearWatch({id: GeolocationService.watchId});
        }

    }

    static receivePosition(position?:GeolocationPosition, error?:any) {
        console.debug("position", position);
        console.debug("error", error);
        if (position && position != null) {
            GeolocationService.latestPosition = position;
            delete GeolocationService.latestError;
        } else {
            delete GeolocationService.latestPosition;
            GeolocationService.latestError = error;
        }
    }

    static getPosition():Promise<GeolocationPosition> {

        return new Promise<GeolocationPosition>((resolve, reject) => {

            if (this.latestPosition) {
                resolve(this.latestPosition);
            } else if (this.latestError) {
                reject(this.latestError);
            } else {
                reject("Did you start the watcher before?");
            }
        });

    }

    static getClosestLake():Promise<CoordsAndLake> {

        // TODO AThimel 28/05/2020 Gérer un timeout pour laisser le temps au téléphone de capturer le GPS

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

                            console.info("lakes", lakes);
                            console.info("position", position);

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
            console.debug(`Distance pour le ${lake.name}: ${distance}`);
        });
        console.debug(`Winner is: ${result.name}`);
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
                            console.debug("lakes", lakes);

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
