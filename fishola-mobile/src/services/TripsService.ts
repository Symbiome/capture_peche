import Lake from '@/pojos/Lake';
import Trip from '@/pojos/Trip';
import TripLight from '@/pojos/TripLight';
import Constants from '@/services/Constants';
import AbstractFisholaService from '@/services/AbstractFisholaService';
import ReferentialService from '@/services/ReferentialService';

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

    static newTrip(mode:string, callback:(id:string)=>any) {
        var options = {weekday: "long", month: "long", day: "numeric"};
        let now = new Date();
        let name = "Sortie du " + now.toLocaleDateString('fr-FR', options);

        let newTrip:Trip = new Trip();
        newTrip.id = Constants.DIRTY_ID;
        newTrip.mode = mode;
        newTrip.name = name;
        newTrip.date = now;

        this.getInstance().onCreationTrip.put(newTrip)
          .then(id => callback(id));
    }

    static newLiveTrip(callback:(id:string)=>any) {
        TripsService.newTrip('live', callback);
    }

    static newAfterwardsTrip(callback:(id:string)=>any) {
        TripsService.newTrip('afterwards', callback);
    }

    static getTrip(id:any, callback:(trip:any)=>any) {
        if (id == Constants.DIRTY_ID || id == Constants.RUNNING_ID) {
            this.getInstance().onCreationTrip.get(id)
            .then((aaa) => callback(aaa));
        } else {
            this.getInstance().trips.get(id)
            .then((aaa) => callback(aaa));
        }
    }

    static toTripLight(input:Trip, lakesIndex:Map<string, Lake>):TripLight {
        var dayOptions = {weekday: "long", month: "long", day: "numeric", year: "numeric"};
        let date = input.date!.toLocaleDateString('fr-FR', dayOptions);
        let lakeName:string = lakesIndex.get(input.lakeId!)!.name;
        let result = new TripLight(input.id!, input.name!, lakeName, date);
        result.catchsCount = input.catchs.length;
        result.duration = Trip.computeDuration(input);
        return result;
    }

    // FIXME Fait doublon avec la méthode computeDuration sur Trip
    static computeDuration(startedAt:Date, finishedAt?:Date):string {
        if (startedAt) {
            let end = finishedAt || new Date();
            let seconds = Math.floor((end.getTime()-startedAt.getTime())/1000);
            let minutes = Math.floor(seconds/60);
            let hours = Math.floor(minutes/60);
            let result = '';
            if (hours > 0) {
                result += hours + 'h ';
                minutes -= hours * 60;
            }
            if (minutes > 0) {
                result += minutes + 'min ';
                seconds -= hours * 60*60 + minutes * 60;
            }
            result += seconds + 's';
            return result;
        }
        return '';
    }

    static listTrips(callback:(trips:TripLight[])=>any) {
        var dayOptions = {weekday: "long", month: "long", day: "numeric", year: "numeric"};

        ReferentialService.getLakesIndex((lakesIndex:Map<string, Lake>) => {

            let result:TripLight[] = [];

            this.getInstance().trips.toArray((trips) => {
                trips.forEach(trip => {
                    if (trip.dirty) {
                        result.push(this.toTripLight(trip, lakesIndex));
                    }
                });

            });

            this.getInstance().backendGet('/v1/trips', (trips:TripLight[]) => {
                console.log("Sorties récupérées depuis le back :", trips);
                trips.forEach((trip) => {
                    trip.lakeName = lakesIndex.get(trip.lakeId!)!.name;
                    let realDate = new Date(trip.date);
                    trip.date = realDate.toLocaleDateString('fr-FR', dayOptions);
                    if (trip.startedAt && trip.finishedAt) {
                        trip.duration = TripsService.computeDuration(new Date(trip.startedAt), new Date(trip.finishedAt!));
                    }
                    result.push(trip);
                })
                callback(result);
            });
        });
    }

    static saveTrip(trip:Trip, callback: () => void) {
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

    static deleteDirtyTrip() {
        this.getInstance().onCreationTrip.delete(Constants.DIRTY_ID);
    }

    static cancelCreations() {
        this.getInstance().onCreationTrip.delete(Constants.DIRTY_ID);
        this.getInstance().onCreationTrip.delete(Constants.RUNNING_ID);
    }

    static finishTripCreation(trip:Trip, callback: (id:string) => void) {
        if (trip.id == Constants.DIRTY_ID) {
            if (trip.mode == 'live') {
                trip.startedAt = new Date();
            }

            trip.id = Constants.RUNNING_ID;
            TripsService.saveTrip(trip, () => {
                this.deleteDirtyTrip();
                callback(trip.id!);
            });
        } else {
            TripsService.saveTrip(trip, () => {callback(trip.id!);});
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
