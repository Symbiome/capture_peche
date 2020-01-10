import Lake from '@/pojos/Lake';
import Trip from '@/pojos/Trip';
import TripMeta from '@/pojos/TripMeta';
import TripSpecies from '@/pojos/TripSpecies';
import TripMain from '@/pojos/TripMain';
import {TripLight, TripMode} from '@/pojos/BackendPojos';
import Constants from '@/services/Constants';
import AbstractFisholaService from '@/services/AbstractFisholaService';
import ReferentialService from '@/services/ReferentialService';
import Helpers from '@/pojos/Helpers';

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
        var options = {weekday: "long", month: "long", day: "numeric"};
        let now = new Date();
        let name = "Sortie du " + now.toLocaleDateString('fr-FR', options);

        let newTrip:TripMeta = {
            id: Constants.DIRTY_ID,
            mode: mode,
            name: name,
            date: now,
            startedAt: now
        }

        this.getInstance().onCreationTrip.put(newTrip)
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
            this.getInstance().onCreationTrip.get(id)
            .then((aaa) => {
                if (!aaa.speciesIds) {
                    aaa.speciesIds = [];
                }
                callback(aaa);
            });
        } else {
            this.getInstance().trips.get(id)
            .then((aaa) => {
                if (aaa) {
                    callback(aaa);
                } else {
                    // Il faut charger le trip depuis le back
                    let url:string = `/v1/trips/${id}`;
                    this.getInstance().backendGet(url, (bbb:any) => {
                        console.log("Sortie récupérée depuis le back :", bbb);
                        callback(TripsService.backendTripToTrip(bbb));
                    });
                }
            });
        }
    }

    static backendTripToTrip(input:any):Trip {
        let realDate = new Date(input.day);

        let result:Trip = {
            id: input.id,
            dirty: false,
            mode: input.mode,
            type: input.type,
            name: input.name,
            lakeId: input.lake,
            // weatherId: input.weather,
            date: realDate,
            speciesIds: [],
            catchs: [],
        };

        if (input.startTime) {
            let startTimeArray = input.startTime.split(':');
            let startedAt = new Date(realDate);
            startedAt.setHours(startTimeArray[0], startTimeArray[1], startTimeArray[2]);
            
            result.startedAt = startedAt;

            if (input.endTime) {
                let endTimeArray = input.endTime.split(':');
                let finishedAt = new Date(realDate);
                finishedAt.setHours(endTimeArray[0], endTimeArray[1], endTimeArray[2]);
                // Cas particulier d'une pêche qui se termine après minuit
                if (finishedAt.getTime() < startedAt.getTime()) {
                    finishedAt.setDate(finishedAt.getDate() + 1);
                }

                result.finishedAt = finishedAt;
            }
        }

        // if (input.modifiableUntil) {
        //     result.modifiableUntil = new Date(input.modifiableUntil);
        // }
        return result;
    }

    static storedTripToLight(input:Trip):TripLight {
        let result:TripLight = {
            id: input.id!,
            name: input.name!,
            date: input.date!,
            catchsCount: input.catchs.length,
            durationInSeconds: 6666, // TODO
            modifiable: true,
            lakeId: input.lakeId!
        };
        return result;
    }

    // static backendTripToLight(input:any, lakesIndex:Map<string, Lake>):TripLight {
    //     let lakeName = lakesIndex.get(input.lakeId!)!.name;
    //     let realDate = new Date(input.date);

    //     let startedAt = new Date(realDate);
    //     startedAt.setHours(input.startedAt[0], input.startedAt[1], input.startedAt[2]);
    //     let finishedAt = new Date(realDate);
    //     finishedAt.setHours(input.finishedAt[0], input.finishedAt[1], input.finishedAt[2]);
    //     // Cas particulier d'une pêche qui se termine après minuit
    //     if (finishedAt.getTime() < startedAt.getTime()) {
    //         finishedAt.setDate(finishedAt.getDate() + 1);
    //     }
    //     let duration = Helpers.computeDuration(startedAt, finishedAt);

    //     let result:TripLight = {
    //         id: input.id,
    //         name: input.name,
    //         lakeName: lakeName,
    //         date: realDate,
    //         catchsCount: input.catchsCount,
    //         // durationInSeconds: duration,
    //         durationInSeconds: 666,
    //         modifiable: input.modifiable
    //     };
    //     if (input.modifiableUntil) {
    //         result.modifiableUntil = new Date(input.modifiableUntil);
    //     }
    //     return result;
    // }

    static listTrips(callback:(trips:TripLight[])=>any) {

        let result:TripLight[] = [];

        this.getInstance().trips.toArray((trips) => {
            trips.forEach(trip => {
                if (trip.dirty) {
                    result.push(this.storedTripToLight(trip));
                }
            });

        });

        this.getInstance().backendGet('/v1/trips', (trips:any) => {
            console.log("Sorties récupérées depuis le back :", trips);
            trips.elements.forEach((trip:TripLight) => {
                console.log(trip);
                result.push(trip);
            })
            callback(result);
        });
    }

    static saveTripMain(trip:TripMain, callback: () => void) {
        if (trip.id == Constants.DIRTY_ID || trip.id == Constants.RUNNING_ID) {
            this.getInstance().onCreationTrip.put(trip)
            .then((aaa) => {
                console.log(aaa);
                callback();
            });
        } else {
            // TODO
        }
    }

    static saveTripMeta(trip:TripMeta, callback: () => void) {
        if (trip.id == Constants.DIRTY_ID || trip.id == Constants.RUNNING_ID) {
            this.getInstance().onCreationTrip.put(trip)
            .then((aaa) => {
                console.log(aaa);
                callback();
            });
            callback();
        } else {
            // TODO
        }
    }

    static saveTripSpecies(trip:TripSpecies, callback: () => void) {
        if (trip.id == Constants.DIRTY_ID || trip.id == Constants.RUNNING_ID) {
            this.getInstance().onCreationTrip.put(trip)
            .then((aaa) => {
                console.log(aaa);
                callback();
            });
            callback();
        } else {
            // TODO
        }
    }

    static deleteDirtyTrip() {
        this.getInstance().onCreationTrip.delete(Constants.DIRTY_ID);
    }

    static cancelCreations() {
        this.getInstance().onCreationTrip.delete(Constants.DIRTY_ID);
        this.getInstance().onCreationTrip.delete(Constants.RUNNING_ID);
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

    static sendTrip(trip:Trip, callback: () => void) {
        if (trip.id == Constants.RUNNING_ID) {
            trip.id = '' + new Date().getTime();
            trip.dirty = true;
            this.getInstance().trips
                .put(trip)
                .then((aaa) => {
                    console.log(aaa);
                    this.cancelCreations();
                    callback();
                });
        } else {
            // TODO
        }
    }

    static syncTrips() {
        this.getInstance().trips
            .filter(t => t.dirty === true)
            .each((dirtyTrip:Trip) => this.syncTrip(dirtyTrip, (result:boolean) => {
                console.log(result);
                if (result) {
                    this.getInstance().trips.delete(dirtyTrip.id!);
                }
            }));
    }

    static syncTrip(trip:Trip, callback: (success:boolean) => void) {
        console.log("On essaye de sauvegarder la sortie", trip);
        this.getInstance().backendPut('/v1/trips', trip, (r) => {
            callback(true);
        }, (eee) => {
            console.log("Pas Okay :'(", eee);
            callback(false);
        });
    }

}
