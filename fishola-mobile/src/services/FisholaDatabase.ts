import Dexie from 'dexie';
import {TripBean} from '@/pojos/BackendPojos';

export default class FisholaDatabase extends Dexie {

    static instance:FisholaDatabase = new FisholaDatabase();

    // Declare implicit table properties.
    // (just to inform Typescript. Instanciated by Dexie in stores() method)
    onCreationTrip: Dexie.Table<any, string>;
    dirtyTrips: Dexie.Table<TripBean, string>;
    dirtyPictures: Dexie.Table<any, string>;
    //...other tables goes here...

    constructor () {
        super("Fishola");
        console.log("Construction de la base Fishola ...");
        this.version(1).stores({
            onCreationTrip: 'id, mode',
            dirtyTrips: 'id, mode',
            //...other tables goes here...
        });
        this.version(2).stores({
            onCreationTrip: 'id, mode',
            dirtyTrips: 'id, mode',
            dirtyPictures: 'id, content, dirtySince'
            //...other tables goes here...
        });

        // The following line is needed if your typescript
        // is compiled using babel instead of tsc:
        this.onCreationTrip = this.table("onCreationTrip");
        this.dirtyTrips = this.table("dirtyTrips");
        this.dirtyPictures = this.table("dirtyPictures");
    }

    static getInstance():FisholaDatabase {
        return this.instance;
    }

}
