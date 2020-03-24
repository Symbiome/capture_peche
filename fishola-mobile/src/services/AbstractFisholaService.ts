import Constants from '@/services/Constants';
import FisholaDatabase from './FisholaDatabase';

class CacheEntry {
  constructor(public since:number, public content:any) {

  }
}

export default abstract class AbstractFisholaService {

    static getDatabase():FisholaDatabase {
      return FisholaDatabase.getInstance();
    }

    static caches:Map<string, CacheEntry> = new Map();

    static clearCache(uri:string) {
      this.caches.delete(uri);
    }

    static backendGet(uri:string):Promise<any> {
      return new Promise<any>((resolve, reject) => {
        let apiUrl = Constants.apiUrl(uri);
        var xhr = new XMLHttpRequest();
        xhr.open('GET', apiUrl, true);
        xhr.withCredentials = true;
        xhr.onload = function() {
          if (this.status == 200) {
            let responseText = this['responseText'];
            let parsed = JSON.parse(responseText);
            resolve(parsed);
          } else if (this.status == 204) {
            resolve();
          } else {
            reject(this.status);
          }
        };
        xhr.send();
      });
    }

    static backendGetWithCache(uri:string):Promise<any> {
      let entry = this.caches.get(uri);
      if (entry && ((new Date().getTime() - entry.since) < (1000 * 60 * 60))) {
        // console.log("On utilise le cache", uri);
        return Promise.resolve(entry.content);
      }

      return new Promise<any>((resolve, reject) => {
        this.backendGet(uri)
          .then(
            (content:any) => {
              let newEntry:CacheEntry = new CacheEntry(new Date().getTime(), content);
              this.caches.set(uri, newEntry);
              resolve(content);
            },
            reject)
        });
    }

    static backendGetWithArgs(uri:string, args:any, callback:(result:any)=>any, errorCallback?:(status:any) => any) {
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

    static wrapResponseReject(xhr:XMLHttpRequest):any {
      let result = {
        status: xhr.status,
        content: undefined
      };
      try {
        let responseText = xhr.responseText;
        if (responseText) {
          let parsed = JSON.parse(responseText);
          result.content = parsed;
        }
      } catch (e) {
        console.error(e);
      }
      return result;
    }

    static backendPut(uri:string, data:any):Promise<any> {
      return new Promise<any>((resolve, reject) => {
        let apiUrl = Constants.apiUrl(uri);
        var xhr = new XMLHttpRequest();
        xhr.open('PUT', apiUrl, true);
        xhr.withCredentials = true;
        xhr.onload = function() {
          if (this.status == 200 || this.status == 201) {
            let responseText = this['responseText'];
            let parsed = JSON.parse(responseText);
            resolve(parsed);
          } else if (this.status == 204) {
            resolve();
          } else {
            let result = AbstractFisholaService.wrapResponseReject(this);
            reject(result);
          }
        };
        if (data != null) {
          xhr.setRequestHeader('Content-Type', 'application/json');
          xhr.send(JSON.stringify(data));
        } else {
          xhr.send();
        }
      });
    }

    static backendDelete(uri:string, data?:any):Promise<void> {
      return new Promise<void>((resolve, reject) => {
        let apiUrl = Constants.apiUrl(uri);
        var xhr = new XMLHttpRequest();
        xhr.open('DELETE', apiUrl, true);
        xhr.withCredentials = true;
        xhr.onload = function() {
          if (this.status == 200 || this.status == 204) {
            resolve();
          } else {
            let result = AbstractFisholaService.wrapResponseReject(this);
            reject(result);
          }
        };
        if (data != null) {
          xhr.setRequestHeader('Content-Type', 'application/json');
          xhr.send(JSON.stringify(data));
        } else {
          xhr.send();
        }
      });
    }

    static backendPost(uri:string, data:any):Promise<any> {
      return new Promise<any>((resolve, reject) => {
        let apiUrl = Constants.apiUrl(uri);
        var xhr = new XMLHttpRequest();
        xhr.open('POST', apiUrl, true);
        xhr.withCredentials = true;
        xhr.onload = function() {
          if (this.status == 200 || this.status == 201) {
            let responseText = this['responseText'];
            let parsed = JSON.parse(responseText);
            resolve(parsed);
          } else if (this.status == 204) {
            resolve();
          } else {
            let result = AbstractFisholaService.wrapResponseReject(this);
            reject(result);
          }
        };
        if (data != null) {
          xhr.setRequestHeader('Content-Type', 'application/json');
          xhr.send(JSON.stringify(data));
        } else {
          xhr.send();
        }
      });
    }

    static backendPutPlain(uri:string, data:string, callback:(result:any)=>any, errorCallback?:(status:any) => any) {
      let apiUrl = Constants.apiUrl(uri);
      var xhr = new XMLHttpRequest();
      xhr.open('PUT', apiUrl, true);
      xhr.withCredentials = true;
      xhr.onload = function() {
        if (this.status == 200 || this.status == 201) {
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
        xhr.setRequestHeader('Content-Type', 'text/plain');
        xhr.send(data);
      } else {
        xhr.send();
      }
  }
}

