import Dexie from 'dexie';
import Trip from '@/pojos/Trip';

export default class TripsStorageService extends Dexie {

    static instance?:TripsStorageService;

    // Declare implicit table properties.
    // (just to inform Typescript. Instanciated by Dexie in stores() method)
    onCreationTrip: Dexie.Table<Trip, string>; // number = type of the primkey
    trips: Dexie.Table<Trip, number>; // number = type of the primkey
    //...other tables goes here...

    constructor () {
        super("Fishola");
        console.log("Construction de la base Fishola ...");
        this.version(1).stores({
            onCreationTrip: 'id, mode',
            trips: 'id, mode',
            //...other tables goes here...
        });

        // The following line is needed if your typescript
        // is compiled using babel instead of tsc:
        this.onCreationTrip = this.table("onCreationTrip");
        this.trips = this.table("trips");
    }

    static getInstance():TripsStorageService {
        if (!this.instance) {
            console.log("Pas encore d'instance partagée, on la créé");
            this.instance = new TripsStorageService();
        }
        return this.instance;
    }

    static newLiveTrip(callback:(id:string)=>any) {
        let newTrip:Trip = new Trip('live');
        newTrip.id = 'DIRTY';
        this.getInstance().onCreationTrip.put(newTrip)
          .then(id => callback(id));
    }

    static newAfterwardsTrip(callback:(id:string)=>any) {
        let newTrip:Trip = new Trip('afterwards');
        newTrip.id = 'DIRTY';
        this.getInstance().onCreationTrip.put(newTrip)
          .then(id => callback(id));
    }

    static getTrip(id:any, callback:(trip:any)=>any) {
        if (id == 'DIRTY') {
            this.getInstance().onCreationTrip.get(id)
            .then((aaa) => callback(aaa));
        } else {
            // TODO
        }
    }

}
