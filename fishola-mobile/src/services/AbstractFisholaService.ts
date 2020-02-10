import Dexie from 'dexie';
import {TripBean} from '@/pojos/BackendPojos';

import Constants from '@/services/Constants';

export default abstract class AbstractFisholaService extends Dexie {

    // Declare implicit table properties.
    // (just to inform Typescript. Instanciated by Dexie in stores() method)
    onCreationTrip: Dexie.Table<any, string>;
    dirtyTrips: Dexie.Table<TripBean, string>;
    pictures: Dexie.Table<any, string>;
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
            pictures: 'id, content'
            //...other tables goes here...
        });

        // The following line is needed if your typescript
        // is compiled using babel instead of tsc:
        this.onCreationTrip = this.table("onCreationTrip");
        this.dirtyTrips = this.table("dirtyTrips");
        this.pictures = this.table("pictures");
    }

    backendGet(uri:string, callback:(result:any)=>any, errorCallback?:(status:any) => any) {
        let apiUrl = Constants.apiUrl(uri);
        var xhr = new XMLHttpRequest();
        xhr.open('GET', apiUrl, true);
        xhr.withCredentials = true;
        xhr.onload = function() {
          if (this.status == 200) {
            let responseText = this['responseText'];
            let parsed = JSON.parse(responseText);
            callback(parsed);
          } else if (errorCallback) {
            errorCallback(this.status);
          }
        };
        xhr.send();
    }

    backendGetWithArgs(uri:string, args:any, callback:(result:any)=>any, errorCallback?:(status:any) => any) {
        let apiUrl = Constants.apiUrl(uri);
        var xhr = new XMLHttpRequest();

        let queryString = Object.keys(args)
          .map(k => encodeURIComponent(k) + '=' + encodeURIComponent(args[k]))
          .join('&');
        apiUrl += "?" + queryString;

        xhr.open('GET', apiUrl, true);
        xhr.withCredentials = true;
        xhr.onload = function() {
          if (this.status == 200) {
            let responseText = this['responseText'];
            let parsed = JSON.parse(responseText);
            callback(parsed);
          } else if (errorCallback) {
            errorCallback(this.status);
          }
        };
        xhr.send();
    }

    backendPut(uri:string, data:any, callback:(result:any)=>any, errorCallback?:(status:any) => any) {
        let apiUrl = Constants.apiUrl(uri);
        var xhr = new XMLHttpRequest();
        xhr.open('PUT', apiUrl, true);
        xhr.withCredentials = true;
        xhr.onload = function() {
          if (this.status == 200) {
            let responseText = this['responseText'];
            let parsed = JSON.parse(responseText);
            callback(parsed);
          } else if (this.status == 204) {
            callback(null);
          } else if (errorCallback) {
            errorCallback(this.status);
          }
        };
        if (data != null) {
          xhr.setRequestHeader('Content-Type', 'application/json');
          xhr.withCredentials = true;
          xhr.send(JSON.stringify(data));
        } else {
          xhr.send();
        }
    }

    backendPost(uri:string, data:any, callback:(result:any)=>any, errorCallback?:(status:any) => any) {
        let apiUrl = Constants.apiUrl(uri);
        var xhr = new XMLHttpRequest();
        xhr.open('POST', apiUrl, true);
        xhr.withCredentials = true;
        xhr.onload = function() {
          if (this.status == 200) {
            let responseText = this['responseText'];
            let parsed = JSON.parse(responseText);
            callback(parsed);
          } else if (this.status == 204) {
            callback(null);
          } else if (errorCallback) {
            errorCallback(this.status);
          }
        };
        if (data != null) {
          xhr.setRequestHeader('Content-Type', 'application/json');
          xhr.withCredentials = true;
          xhr.send(JSON.stringify(data));
        } else {
          xhr.send();
        }
    }

}

