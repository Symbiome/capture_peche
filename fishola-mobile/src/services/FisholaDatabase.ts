import Dexie from 'dexie';
import {TripBean} from '@/pojos/BackendPojos';
import StoredPicture from '@/pojos/StoredPicture';

export default class FisholaDatabase extends Dexie {

    static instance:FisholaDatabase = new FisholaDatabase();

    // Declare implicit table properties.
    // (just to inform Typescript. Instanciated by Dexie in stores() method)
    onCreationTrip: Dexie.Table<any, string>;
    dirtyTrips: Dexie.Table<TripBean, string>;
    dirtyPictures: Dexie.Table<StoredPicture, string>;

    constructor () {
        super("Fishola");
        console.log("Construction de la base Fishola ...");
        this.version(1).stores({
            onCreationTrip: 'id',
            dirtyTrips: 'id',
        });
        this.version(2).stores({
            onCreationTrip: 'id',
            dirtyTrips: 'id',
            dirtyPictures: 'id'
        });

        this.onCreationTrip = this.table("onCreationTrip");
        this.dirtyTrips = this.table("dirtyTrips");
        this.dirtyPictures = this.table("dirtyPictures");

        console.log("Base Fishola prête");
    }

    static getInstance():FisholaDatabase {
        return this.instance;
    }

}
