import Constants from '@/services/Constants';
import FisholaDatabase from './FisholaDatabase';

export default abstract class AbstractFisholaService {

    static getDatabase():FisholaDatabase {
      return FisholaDatabase.getInstance();
    }

    static backendGet(uri:string, callback:(result:any)=>any, errorCallback?:(status:any) => any) {
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

    static backendPut(uri:string, data:any, callback:(result:any)=>any, errorCallback?:(status:any) => any) {
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
          xhr.setRequestHeader('Content-Type', 'application/json');
          xhr.send(JSON.stringify(data));
        } else {
          xhr.send();
        }
    }

    static backendDelete(uri:string, callback:()=>any, errorCallback?:(status:any) => any) {
        let apiUrl = Constants.apiUrl(uri);
        var xhr = new XMLHttpRequest();
        xhr.open('DELETE', apiUrl, true);
        xhr.withCredentials = true;
        xhr.onload = function() {
          if (this.status == 200 || this.status == 204) {
            callback();
          } else if (errorCallback) {
            errorCallback(this.status);
          }
        };
        xhr.send();
    }

    static backendPost(uri:string, data:any, callback:(result:any)=>any, errorCallback?:(status:any) => any) {
        let apiUrl = Constants.apiUrl(uri);
        var xhr = new XMLHttpRequest();
        xhr.open('POST', apiUrl, true);
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
          xhr.setRequestHeader('Content-Type', 'application/json');
          xhr.send(JSON.stringify(data));
        } else {
          xhr.send();
        }
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

