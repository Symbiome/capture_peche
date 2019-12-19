import Dexie from 'dexie';
import Trip from '@/pojos/Trip';
import Constants from '@/services/Constants';

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

    static newTrip(mode:string, callback:(id:string)=>any) {
        var options = {weekday: "long", month: "long", day: "numeric"};
        let now = new Date();
        let name = "Sortie du " + now.toLocaleDateString('fr-FR', options);

        let newTrip:Trip = new Trip(mode);
        newTrip.id = Constants.DIRTY_ID;
        newTrip.name = name;
        newTrip.date = now;

        this.getInstance().onCreationTrip.put(newTrip)
          .then(id => callback(id));
    }

    static newLiveTrip(callback:(id:string)=>any) {
        TripsStorageService.newTrip('live', callback);
    }

    static newAfterwardsTrip(callback:(id:string)=>any) {
        TripsStorageService.newTrip('afterwards', callback);
    }

    static getTrip(id:any, callback:(trip:any)=>any) {
        if (id == Constants.DIRTY_ID) {
            this.getInstance().onCreationTrip.get(id)
            .then((aaa) => callback(aaa));
        } else {
            // TODO
        }
    }

}
