import TripMeta from '@/pojos/TripMeta';
import TripSpecies from '@/pojos/TripSpecies';
import TripMain from '@/pojos/TripMain';
import TripSummary from '@/pojos/TripSummary';
import {TripLight, TripMode, TripBean} from '@/pojos/BackendPojos';
import Constants from '@/services/Constants';
import AbstractFisholaService from '@/services/AbstractFisholaService';

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

    static backendTripToTrip(input:any):TripBean {
        let realDate = new Date(input.date);

console.log("From the back", input);

        let result:any = {
            id: input.id,
            mode: input.mode,
            type: input.type,
            name: input.name,
            lakeId: input.lakeId,
            weatherId: input.weatherId,
            date: realDate,
            speciesIds: input.speciesIds || [],
            catchs: [],
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

        // if (input.modifiableUntil) {
        //     result.modifiableUntil = new Date(input.modifiableUntil);
        // }
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
        let realDate = new Date(input.date);
        input.date = realDate;
        return input;
    }

    static listTrips(sortDown:boolean, callback:(trips:TripLight[], count:number)=>any) {

        let result:TripLight[] = [];

        this.getInstance().trips.toArray((trips) => {
            trips.forEach(trip => {
                result.push(this.storedTripToLight(trip));
            });

        });

        let page = {
            pageNumber: 0,
            pageSize: -1,
            orderClauses: [{clause:"whatever", desc:sortDown}]
        };

        this.getInstance().backendPost('/v1/trips', page, (trips:any) => {
            console.log("Sorties récupérées depuis le back :", trips);
            trips.elements.forEach((trip:TripLight) => {
                console.log(trip);
                let tl:TripLight = TripsService.backendTripToLight(trip);
                result.push(tl);
            })
            callback(result, trips.count);
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

    static sendTrip(trip:any, callback: () => void) {
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
            // .filter(t => t.dirty === true)
            .each((dirtyTrip:TripBean) => this.syncTrip(dirtyTrip, (result:boolean) => {
                console.log(result);
                if (result) {
                    this.getInstance().trips.delete(dirtyTrip.id!);
                }
            }));
    }

    static syncTrip(trip:TripBean, callback: (success:boolean) => void) {
        console.log("On essaye de sauvegarder la sortie", trip);
        this.getInstance().backendPut('/v1/trips', trip, (r) => {
            callback(true);
        }, (eee) => {
            console.log("Pas Okay :'(", eee);
            callback(false);
        });
    }

}
