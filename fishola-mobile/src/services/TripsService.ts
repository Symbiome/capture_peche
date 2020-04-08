import TripMeta from '@/pojos/TripMeta';
import TripSpecies from '@/pojos/TripSpecies';
import TripMain from '@/pojos/TripMain';
import {TripLight, TripMode, TripBean, CatchBean} from '@/pojos/BackendPojos';
import Helpers from '@/pojos/Helpers';
import Constants from '@/services/Constants';
import AbstractFisholaService from '@/services/AbstractFisholaService';
import PicturesService from '@/services/PicturesService';
import CatchSummary from '@/pojos/CatchSummary';
import ReferentialService from './ReferentialService';

import moment from 'moment';
import ProfileService from './ProfileService';
import TripSummary from '@/pojos/TripSummary';

export class TripsAndCount {
    constructor (
        public trips:TripLight[],
        public count:number) {
    }
}

export default class TripsService extends AbstractFisholaService {

    static instance?:TripsService;

    constructor () {
        super();
    }

    static getInstance():TripsService {
        if (!this.instance) {
            console.log("Pas encore d'instance partagée, on la créé");
            this.instance = new TripsService();
        }
        return this.instance;
    }

    static newTrip(mode:TripMode, callback:(id:string)=>any) {

        let newTrip:TripMeta = {
            id: Constants.NEW_TRIP_ID,
            mode: mode,
        }

        if (mode == 'Live') {
            var options = {weekday: "long", month: "long", day: "numeric"};
            let now = new Date();

            newTrip.name = "Sortie du " + now.toLocaleDateString('fr-FR', options);
            newTrip.date = now;
            newTrip.startedAt = moment().format(moment.HTML5_FMT.TIME_SECONDS);
        }

        this.getDatabase()
            .onCreationTrip
            .put(newTrip)
            .then(id => callback(id));
    }

    static newLiveTrip(callback:(id:string)=>any) {
        TripsService.newTrip('Live', callback);
    }

    static newAfterwardsTrip(callback:(id:string)=>any) {
        TripsService.newTrip('Afterwards', callback);
    }

    static getTrip(id:any, callback:(trip:any)=>any) {
        if (id == Constants.NEW_TRIP_ID || id == Constants.RUNNING_ID) {
            this.getDatabase()
                .onCreationTrip
                .get(id)
                .then((aaa) => {
                    if (!aaa.speciesIds) {
                        aaa.speciesIds = [];
                    }
                    callback(aaa);
                });
        } else {
            this.getDatabase()
                .dirtyTrips
                .get(id)
                .then((aaa) => {
                    if (aaa) {
                        console.log("Sortie trouvée dans les dirtyTrips :", aaa);
                        callback(aaa);
                    } else {
                        // Il faut charger le trip depuis le back
                        let url:string = `/v1/trips/${id}`;
                        this.backendGet(url)
                            .then((bbb:any) => {
                                console.log("Sortie récupérée depuis le back :", bbb);
                                callback(TripsService.backendTripToTrip(bbb));
                            });
                    }
                });
        }
    }

    static backendCatchToCatchBean(realDate:moment.Moment, input:any):CatchBean {

        let result:any = {
            tripId: input.tripId,
            id: input.id,
            speciesId: input.speciesId,
            size: input.size,
            weight: input.weight,
            keep: input.keep,
            releasedStateId: input.releasedStateId,
            techniqueId: input.techniqueId,
            description: input.description,
            sampleId: input.sampleId,
            hasPicture: input.hasPicture,
            latitude: input.latitude,
            longitude: input.longitude,
            caughtAt: input.caughtAt
        };

        return result;
    }

    static backendTripToTrip(input:any):TripBean {
        let realDate = Helpers.parseLocalDate(input.date);
        let realCreatedOn = Helpers.parseLocalDateTime(input.createdOn);

        let catchs:CatchBean[] = [];
        if (input.catchs) {
            input.catchs.forEach((aCatch:any) => {
                let aCatchBean:CatchBean = TripsService.backendCatchToCatchBean(moment(realDate), aCatch);
                catchs.push(aCatchBean);
            });
        }

        let result:any = {
            id: input.id,
            createdOn: realCreatedOn,
            mode: input.mode,
            type: input.type,
            name: input.name,
            lakeId: input.lakeId,
            weatherId: input.weatherId,
            date: realDate,
            speciesIds: input.speciesIds || [],
            techniqueIds: input.techniqueIds || [],
            catchs: catchs,
            startedAt: input.startedAt,
            finishedAt: input.finishedAt,
        };

        if (input.modifiableUntil) {
            result.modifiableUntil = Helpers.parseLocalDateTime(input.modifiableUntil);
        }
        return result;
    }

    static storedTripToLight(input:TripBean):TripLight {
        let seconds:number = Helpers.computeDurationInSeconds(input.startedAt, input.finishedAt);
        let catchsCount:number = input.catchs ? input.catchs.length : 0;

        let result:TripLight = <any> input;
        result.modifiable = true;
        result.durationInSeconds = seconds;
        result.catchsCount = catchsCount;

        return result;
    }

    static backendTripToLight(input:any):TripLight {
        let realDate = Helpers.parseLocalDate(input.date);
        input.date = realDate;
        return input;
    }

    static listTrips(sortDown:boolean, searchTerm:string, rawPageIndex?:number):Promise<TripsAndCount> {

        let pageIndex =  rawPageIndex || 0;

        return new Promise<TripsAndCount>((resolve, reject) => {

            let result:TripLight[] = [];

            let dirtyTripsIds:string[] = [];

            this.getDatabase()
                .dirtyTrips
                .toArray((trips) => {
                    trips.forEach(trip => {
                        let tripLight:TripLight = TripsService.storedTripToLight(trip);
                        dirtyTripsIds.push(tripLight.id);
                        if (pageIndex === 0) {
                            result.push(tripLight);
                        }
                    });
                });

            let page = {
                pageNumber: pageIndex,
                pageSize: 10,
                desc: sortDown,
                term: searchTerm
            };

            this.backendGetWithArgs('/v1/trips', page)
                .then(
                    (trips:any) => {
                        console.log("Sorties récupérées depuis le back :", trips);
                        trips.elements.forEach((trip:TripLight) => {
                            if (dirtyTripsIds.indexOf(trip.id) == -1) {
                                let tl:TripLight = TripsService.backendTripToLight(trip);
                                result.push(tl);
                            }
                        })
                        let count:number = dirtyTripsIds.length + trips.count;
                        let tripsAndCount = new TripsAndCount(result, count);
                        resolve(tripsAndCount);
                    },
                    reject
                );
        });
    }

    /**
     * Termine la phase d'ajout de captures sur une sortie (trip.id == Constants.RUNNING_ID)
     */
    static finishTripCatchs(trip:TripMain, callback: () => void) {
        if (trip.id == Constants.RUNNING_ID) {
            if (trip.mode == 'Live') {
                trip.finishedAt = moment().format(moment.HTML5_FMT.TIME_SECONDS);
            }

            let tripBean:TripBean = <TripBean>trip;
            if (!tripBean.techniqueIds) {
                tripBean.techniqueIds = [];
            }
            if (trip.catchs) {
                trip.catchs.forEach((c) => {
                    if (tripBean.techniqueIds.indexOf(c.techniqueId) == -1) {
                        tripBean.techniqueIds.push(c.techniqueId);
                    }
                });
            }

            this.getDatabase()
                .onCreationTrip
                .put(tripBean)
                .then((aaa) => {
                    console.log(aaa);
                    callback();
                });
        } else {
            throw 'Ne doit être appelé que sur une sortie en cours de création. Id=' + trip.id;
        }
    }

    static saveTrip0(trip:any):Promise<string> {
        return new Promise<string>((resolve, reject) => {
            if (trip.id == Constants.NEW_TRIP_ID || trip.id == Constants.RUNNING_ID) {
                this.getDatabase()
                    .onCreationTrip
                    .put(trip)
                    .then((savedId) => {
                        console.log("Sauvegardé dans onCreationTrip", savedId);
                        resolve(savedId);
                    },
                    reject);
            } else {
                let tripBean:TripBean = <TripBean>trip;
                this.setSaveDelayMarker(tripBean);
                this.getDatabase()
                    .dirtyTrips
                    .put(tripBean)
                    .then((savedId) => {
                        console.log("Sauvegardé dans dirtyTrips", savedId);
                        resolve(savedId);
                    },
                    reject);
            }
        });
    }

    static saveTripMeta(trip:TripMeta, callback: () => void) {
        this.saveTrip0(trip)
            .then((aaa) => {
                console.log(aaa);
                callback();
            });
    }

    static saveTripSpecies(trip:TripSpecies, callback: () => void) {
        this.saveTrip0(trip)
            .then((aaa) => {
                console.log(aaa);
                callback();
            });
    }

    static saveTrip(trip:TripBean, callback: () => void) {
        this.saveTrip0(trip)
            .then((aaa) => {
                console.log(aaa);
                callback();
            });
    }

    static deleteDirtyTrip() {
        this.getDatabase().onCreationTrip.delete(Constants.NEW_TRIP_ID);
    }

    static deleteRunningTrip() {
        this.getDatabase().onCreationTrip.delete(Constants.RUNNING_ID);
    }

    static cancelCreations() {
        this.deleteDirtyTrip();
        this.deleteRunningTrip();
    }

    /**
     * Appelé quand on a terminé l'initialisation de la sortie (meta + espèces)
     */
    static finishTripBootstrap(trip:TripSpecies, callback: (id:string) => void) {
        if (trip.id == Constants.NEW_TRIP_ID) {
            if (trip.mode == 'Live') {
                trip.startedAt = moment().format(moment.HTML5_FMT.TIME_SECONDS);
            }

            trip.id = Constants.RUNNING_ID;
            TripsService.saveTripSpecies(trip, () => {
                this.deleteDirtyTrip();
                callback(trip.id!);
            });
        } else {
            TripsService.saveTripSpecies(trip, () => {callback(trip.id!);});
        }
    }

    static removeSaveDelayMarker(someObject:any) {
        delete someObject.saveDelayMarker;
    }

    static setSaveDelayMarker(someObject:any) {
        someObject.saveDelayMarker = new Date();
    }

    /**
     * Indique si la sauvegarde de la sortie doit être différée ou non
     */
    static shouldDelaySave(someObject:any):boolean {
        if (!someObject.saveDelayMarker) {
            return false;
        }
        let delayMoment = moment(someObject.saveDelayMarker);
        let now = moment();
        let durationMillis = now.diff(delayMoment);
        let duration = moment.duration(durationMillis);
        // On diffère les sauvegardes non explicites de moins de 1h
        let result = duration.minutes() < 60;
        return result;
    }

    /**
     * Appelé quand la sortie est complète et qu'on veut la sauvegarder sur le serveur (trip.id == Constants.RUNNING_ID)
     */
    static sendTrip(trip:TripBean, callback: () => void) {
        if (trip.id == Constants.RUNNING_ID) {
            trip.id = '' + new Date().getTime();
        }
        this.removeSaveDelayMarker(trip);
        this.getDatabase()
            .dirtyTrips
            .put(trip)
            .then((aaa) => {
                console.log("Nouvelle sortie dans les dirtyTrips", aaa);
                this.cancelCreations();
                callback();
            });
    }

    static syncTrips():Promise<boolean> {

        let result:Promise<boolean> = new Promise<boolean>((resolve, reject) => {
            let someTripsSaved:boolean = false;
            let allPromises:Promise<void>[] = [];

            this.getDatabase()
                .dirtyTrips
                .toArray((dirtyTrips) => {
                    if (dirtyTrips && dirtyTrips.length > 0) {
                        console.log(`${dirtyTrips.length} sorties à synchroniser`);
                        dirtyTrips
                            .forEach((dirtyTrip:TripBean) => {
                                if (this.shouldDelaySave(dirtyTrip)) {
                                    console.log(`On ignore la sortie qui est peut-être encore en cours de modif`, dirtyTrip.id);
                                } else {
                                    let promise = this.syncTrip(dirtyTrip);
                                    promise.then(() => {
                                        this.getDatabase().dirtyTrips.delete(dirtyTrip.id!);
                                        someTripsSaved = true;
                                        console.log("Trip saved !");
                                    },
                                    (error) => {
                                        console.log("Arf, môrche pô :(", error);
                                    });
                                    allPromises.push(promise);
                                }
                            });

                        Promise.all(allPromises)
                            .then(() => {
                                resolve(someTripsSaved)
                            }, reject);
                    } else {
                        console.log("Aucune sortie à synchroniser");
                        resolve(false);
                    }
                });
            });

        return result;
    }

    static hasOtherSpecies(trip:TripBean):boolean {
        if (trip.otherSpecies) {
            return true;
        }
        if (trip.catchs && trip.catchs.length > 0) {

            for (let i:number = 0; i<trip.catchs.length; i++) {
                let someCatch = trip.catchs[i];
                if (someCatch.otherSpecies) {
                    return true;
                }
            }
        }

        return false;
    }

    static syncTrip(trip:TripBean):Promise<void> {
        return new Promise((resolve, reject) =>  {
            console.log("On essaye de sauvegarder la sortie", trip);
            if (trip.createdOn) {
                this.backendPut(`/v1/trips/${trip.id}`, trip)
                    .then(
                        (r) => {
                            PicturesService.checkForPicturesToRename(r);
                            if (this.hasOtherSpecies(trip)) {
                                ReferentialService.clearSpeciesCustomCache();
                            }
                            resolve();
                        },
                        reject
                        );
            } else {
                this.backendPost('/v1/trips', trip)
                    .then(
                        (r) => {
                            PicturesService.checkForPicturesToRename(r);
                            if (this.hasOtherSpecies(trip)) {
                                ReferentialService.clearSpeciesCustomCache();
                            }
                            resolve();
                        },
                        reject
                        );
            }
        });
    }

    static getTripAndCatch(tripId:any, catchId:any, callback:(t:TripBean,c:CatchSummary) => void) {
        TripsService.getTrip(tripId, (trip:TripBean) => {
            let result:CatchSummary = {id:catchId};
            if (trip.catchs) {
                trip.catchs.forEach((someCatch:CatchBean) => {
                    if (catchId == someCatch.id) {
                        result = someCatch;
                    }
                });
            }
            if (trip.mode == 'Live' && result.id == Constants.NEW_CATCH_ID) {
                result.caughtAt = moment().format(moment.HTML5_FMT.TIME_SECONDS);
            }
            callback(trip, result);
        });
    }

    static saveCatch(tripId:any, aCatch:CatchBean, callback:(catchId:string) => void) {
        TripsService.getTrip(tripId, (trip:TripBean) => {
            if (!trip.catchs) {
                trip.catchs = [];
            }
            let found = false;
            for (let i:number = 0; i<trip.catchs.length; i++) {
                let someCatch = trip.catchs[i];
                if (aCatch.id == someCatch.id) {
                    found = true;
                    trip.catchs[i] = aCatch;
                }
            }
            if (!found) {
                if (aCatch.id == Constants.NEW_CATCH_ID) {
                    aCatch.id = new Date().getTime().toString();
                }
                trip.catchs.push(aCatch);
            }
            TripsService.saveTrip(trip, () => {
                callback(aCatch.id);
            });
        });
    }

    static deleteCatch(tripId:any, catchId:any, callback:() => void) {
        PicturesService.deletePicture(catchId);
        TripsService.getTrip(tripId, (trip:TripBean) => {
            if (!trip.catchs) {
                trip.catchs = [];
            }
            let foundAtIndex:number = -1;
            for (let i:number = 0; i<trip.catchs.length; i++) {
                let someCatch = trip.catchs[i];
                if (catchId == someCatch.id) {
                    foundAtIndex = i;
                }
            }
            if (foundAtIndex >= 0) {
                trip.catchs.splice(foundAtIndex, 1);
            }
            TripsService.saveTrip(trip, () => {
                callback();
            });
        });
    }

    static deleteTrip(tripId:any, callback:() => void) {
        this.getDatabase().dirtyTrips.delete(tripId);
        this.backendDelete(`/v1/trips/${tripId}`).then(callback);
    }

    static deleteTrips(tripIds:string[]):Promise<void> {
        console.log("Supprime les sorties", tripIds);
        return this.backendDelete(`/v1/trips`, tripIds);
    }

    static getRunningTrip():Promise<TripMain> {
        return new Promise<TripMain>((resolve, reject) => {
            this.getDatabase()
                .onCreationTrip
                .get(Constants.RUNNING_ID)
                .then((runningTrip) => {
                    if (runningTrip) {
                        if (runningTrip.mode == 'Live') {
                            let dateMoment = moment(runningTrip.date);
                            let todayMoment = moment();
                            if (dateMoment.dayOfYear() == todayMoment.dayOfYear()) {
                                resolve(runningTrip);
                            } else {
                                console.log("Il y avait une sortie live en cours, on la bascule en sortie à posteriori");
                                runningTrip.mode = 'Afterwards';
                                this.saveTrip(runningTrip, () => {
                                    resolve(runningTrip);
                                })
                            }
                        } else {
                            resolve(runningTrip);
                        }
                    } else {
                        reject("Pas de running trip");
                    }
                });
        }); 
    }

    static hasRunningTrip():Promise<boolean> {
        return new Promise<boolean>((resolve, reject) => {
            this.getRunningTrip()
                .then(
                    () => resolve(true), 
                    () => resolve(false)
                );
        }); 
    }

    static newSampleId():Promise<string> {
        return new Promise<string>((resolve, reject) => {
            ProfileService.getProfile()
                .then((profile) => {
                    let now = moment();
                    // TODO AThimel 01/04/2020 Pour l'instant on garanti l'unicité sur la base de :
                    // TODO AThimel 01/04/2020   [jour-du-mois] * 1440 + [heure] * 60 + [minute]
                    // TODO AThimel 01/04/2020 Quand on saura en assurer l'unicité, utiliser une séquence propre au [sampleBaseId]
                    let number = now.date()*(24*60) + now.hours()*60 + now.minutes();
                    let result = profile.sampleBaseId + "-" + number;
                    resolve(result);
                },
                reject);
        });
    }

}
