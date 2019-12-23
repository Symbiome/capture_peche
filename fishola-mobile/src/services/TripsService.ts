import Trip from '@/pojos/Trip';
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
            // TODO
        }
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

}
