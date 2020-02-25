import TripMeta from '@/pojos/TripMeta';
import TripSpecies from '@/pojos/TripSpecies';
import TripMain from '@/pojos/TripMain';
import {TripLight, TripMode, TripBean, CatchBean} from '@/pojos/BackendPojos';
import Helpers from '@/pojos/Helpers';
import Constants from '@/services/Constants';
import AbstractFisholaService from '@/services/AbstractFisholaService';
import PicturesService from '@/services/PicturesService';
import CatchSummary from '@/pojos/CatchSummary';

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
            id: Constants.DIRTY_ID,
            mode: mode,
        }

        if (mode == 'Live') {
            var options = {weekday: "long", month: "long", day: "numeric"};
            let now = new Date();

            newTrip.name = "Sortie du " + now.toLocaleDateString('fr-FR', options);
            newTrip.date = now;
            newTrip.startedAt = now;
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
        if (id == Constants.DIRTY_ID || id == Constants.RUNNING_ID) {
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

    static backendCatchToCatchBean(realDate:Date, input:any):CatchBean {

        let result:any = {
            id: input.id,
            speciesId: input.speciesId,
            size: input.size,
            weight: input.weight,
            keep: input.keep,
            releasedStateId: input.releasedStateId,
            techniqueId: input.techniqueId,
            description: input.description,
            withSample: input.withSample,
            hasPicture: input.hasPicture
        };

        if (input.caughtAt) {
            let timeArray = input.caughtAt.split(':');
            let caughtAt = new Date(realDate);
            caughtAt.setHours(timeArray[0], timeArray[1], timeArray[2]);

            result.caughtAt = caughtAt;
        }

        return result;
    }

    static backendTripToTrip(input:any):TripBean {
        let realDate = new Date(input.date);
        let realCreatedOn = Helpers.parseLocalDateTime(input.createdOn);

        let catchs:CatchBean[] = [];
        if (input.catchs) {
            input.catchs.forEach((aCatch:any) => {
                let aCatchBean:CatchBean = TripsService.backendCatchToCatchBean(realDate, aCatch);
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
            catchs: catchs,
        };

        if (input.startedAt) {
            let startTimeArray = input.startedAt.split(':');
            let startedAt = new Date(realDate);
            startedAt.setHours(startTimeArray[0], startTimeArray[1], startTimeArray[2]);
            
            result.startedAt = startedAt;

            if (input.finishedAt) {
                let endTimeArray = input.finishedAt.split(':');
                let finishedAt = new Date(realDate);
                finishedAt.setHours(endTimeArray[0], endTimeArray[1], endTimeArray[2]);
                // Cas particulier d'une pêche qui se termine après minuit
                if (finishedAt.getTime() < startedAt.getTime()) {
                    finishedAt.setDate(finishedAt.getDate() + 1);
                }

                result.finishedAt = finishedAt;
            }
        }

        if (input.modifiableUntil) {
            result.modifiableUntil = Helpers.parseLocalDateTime(input.modifiableUntil);
        }
        return result;
    }

    static storedTripToLight(input:TripBean):TripLight {
        let seconds:number = Math.floor((input.finishedAt.getTime() - input.startedAt.getTime())/1000);
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

    static listTrips(sortDown:boolean, callback:(trips:TripLight[], count:number)=>any) {

        let result:TripLight[] = [];

        let dirtyTripsIds:string[] = [];

        this.getDatabase()
            .dirtyTrips
            .toArray((trips) => {
                trips.forEach(trip => {
                    let tripLight:TripLight = TripsService.storedTripToLight(trip);
                    dirtyTripsIds.push(tripLight.id);
                    result.push(tripLight);
                });

        });

        let page = {
            pageNumber: 0,
            pageSize: -1,
            desc: sortDown
        };

        this.backendGetWithArgs('/v1/trips', page, (trips:any) => {
            console.log("Sorties récupérées depuis le back :", trips);
            trips.elements.forEach((trip:TripLight) => {
                if (dirtyTripsIds.indexOf(trip.id) == -1) {
                    let tl:TripLight = TripsService.backendTripToLight(trip);
                    result.push(tl);
                }
            })
            let count:number = dirtyTripsIds.length + trips.count;
            callback(result, count);
        });
    }

    static saveTripMain(trip:TripMain, callback: () => void) {
        if (trip.id == Constants.DIRTY_ID || trip.id == Constants.RUNNING_ID) {
            this.getDatabase()
                .onCreationTrip
                .put(trip)
                .then((aaa) => {
                    console.log(aaa);
                    callback();
                });
        } else {
            let tripBean:TripBean = <TripBean>trip;
            this.getDatabase()
                .dirtyTrips
                .put(tripBean)
                .then((aaa) => {
                    console.log(aaa);
                    callback();
                });
        }
    }

    static saveTripMeta(trip:TripMeta, callback: () => void) {
        if (trip.id == Constants.DIRTY_ID || trip.id == Constants.RUNNING_ID) {
            this.getDatabase()
                .onCreationTrip
                .put(trip)
                .then((aaa) => {
                    console.log(aaa);
                    callback();
                });
        } else {
            let tripBean:TripBean = <TripBean>trip;
            this.getDatabase()
                .dirtyTrips
                .put(tripBean)
                .then((aaa) => {
                    console.log(aaa);
                    callback();
                });
        }
    }

    static saveTripSpecies(trip:TripSpecies, callback: () => void) {
        if (trip.id == Constants.DIRTY_ID || trip.id == Constants.RUNNING_ID) {
            this.getDatabase()
                .onCreationTrip
                .put(trip)
                .then((aaa) => {
                    console.log(aaa);
                    callback();
                });
        } else {
            let tripBean:TripBean = <TripBean>trip;
            this.getDatabase()
                .dirtyTrips
                .put(tripBean)
                .then((aaa) => {
                    console.log(aaa);
                    callback();
                });
        }
    }

    static saveTrip(trip:TripBean, callback: () => void) {
        if (trip.id == Constants.DIRTY_ID || trip.id == Constants.RUNNING_ID) {
            this.getDatabase()
                .onCreationTrip
                .put(trip)
                .then((aaa) => {
                    console.log(aaa);
                    callback();
                });
        } else {
            let tripBean:TripBean = <TripBean>trip;
            this.getDatabase()
                .dirtyTrips.put(tripBean)
                .then((aaa) => {
                    console.log(aaa);
                    callback();
                });
        }
    }

    static deleteDirtyTrip() {
        this.getDatabase().onCreationTrip.delete(Constants.DIRTY_ID);
    }

    static cancelCreations() {
        this.getDatabase().onCreationTrip.delete(Constants.DIRTY_ID);
        this.getDatabase().onCreationTrip.delete(Constants.RUNNING_ID);
    }

    static finishTripCreation(trip:TripSpecies, callback: (id:string) => void) {
        if (trip.id == Constants.DIRTY_ID) {
            if (trip.mode == 'Live') {
                trip.startedAt = new Date();
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

    static sendTrip(trip:any, callback: () => void) {
        if (trip.id == Constants.RUNNING_ID) {
            trip.id = '' + new Date().getTime();
            trip.dirty = true;
            this.getDatabase()
                .dirtyTrips
                .put(trip)
                .then((aaa) => {
                    console.log("Nouvelle sortie dans les dirtyTrips", aaa);
                    this.cancelCreations();
                    callback();
                });
        } else {

            this.getDatabase()
                .dirtyTrips
                .put(trip)
                .then((aaa) => {
                    console.log("Mise à jour de la sortie dans les dirtyTrips", aaa);
                    callback();
                });
        }
    }

    static syncTrips():Promise<boolean> {

        let result:Promise<boolean> = new Promise<boolean>((resolve, reject) => {
            let someTripsSaved:boolean = false;
            let allPromises:Promise<void>[] = [];

            this.getDatabase()
                .dirtyTrips
                .toArray((dirtyTrips) => {
                    dirtyTrips
                        .forEach((dirtyTrip:TripBean) => {
                            let promise = this.syncTrip(dirtyTrip);
                            promise.then(() => {
                                this.getDatabase().dirtyTrips.delete(dirtyTrip.id!);
                                someTripsSaved = true;
                                console.log("Trip saved !");
                            }, () => {
                                console.log("Arf, môrche pô :(");
                            });
                            allPromises.push(promise);
                        });

                    Promise.all(allPromises)
                        .then(() => {
                            resolve(someTripsSaved)
                        }, reject);
                    });
            });

        return result;
    }

    static syncTrip(trip:TripBean):Promise<void> {
        return new Promise((resolve, reject) =>  {
            console.log("On essaye de sauvegarder la sortie", trip);
            if (trip.createdOn) {
                this.backendPut(`/v1/trips/${trip.id}`, trip)
                    .then(
                        (r) => {
                            PicturesService.checkForPicturesToRename(r);
                            resolve();
                        },
                        reject
                        );
            } else {
                this.backendPost('/v1/trips', trip)
                    .then(
                        (r) => {
                            PicturesService.checkForPicturesToRename(r);
                            resolve();
                        },
                        reject
                        );
            }
        });
    }

    static getTripAndCatch(tripId:any, catchId:any, callback:(t:TripBean,c:CatchSummary) => void) {
        TripsService.getTrip(tripId, (trip:TripBean) => {
            let result:CatchSummary = {id:catchId, withSample:false};
            if (trip.catchs) {
                trip.catchs.forEach((someCatch:CatchBean) => {
                    if (catchId == someCatch.id) {
                        result = someCatch;
                    }
                });
            }
            if (trip.mode == 'Live' && result.id == Constants.NEW_CATCH_ID) {
                result.caughtAt = new Date();
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

}
