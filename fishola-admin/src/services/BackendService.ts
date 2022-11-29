/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import Constants from "@/services/Constants";

export default abstract class BackendService {
  static backendGet(uri: string): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      const apiUrl = Constants.apiUrl(uri);
      const xhr = new XMLHttpRequest();
      xhr.open("GET", apiUrl, true);
      xhr.withCredentials = true;
      xhr.onload = function() {
        if (this.status == 200) {
          const responseText = this["responseText"];
          const parsed = JSON.parse(responseText);
          resolve(parsed);
        } else if (this.status == 204) {
          resolve(undefined);
        } else {
          const result = BackendService.wrapResponseReject(this);
          reject(result);
        }
      };
      xhr.send();
    });
  }

  static backendGetWithArgs(uri: string, args: any): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      let apiUrl = Constants.apiUrl(uri);
      const xhr = new XMLHttpRequest();

      const queryString = Object.keys(args)
        .map(k => encodeURIComponent(k) + "=" + encodeURIComponent(args[k]))
        .join("&");
      apiUrl += "?" + queryString;

      xhr.open("GET", apiUrl, true);
      xhr.withCredentials = true;
      xhr.onload = function() {
        if (this.status == 200 || this.status == 201) {
          const responseText = this["responseText"];
          if (responseText.length < 1) {
            resolve(true);
          } else {
            const parsed = JSON.parse(responseText);
            resolve(parsed);
          }
        } else if (this.status == 204) {
          resolve(undefined);
        } else {
          const result = BackendService.wrapResponseReject(this);
          reject(result);
        }
      };
      xhr.send();
    });
  }

  static wrapResponseReject(xhr: XMLHttpRequest): any {
    const result = {
      status: xhr.status,
      content: undefined
    };
    try {
      const responseText = xhr.responseText;
      if (responseText) {
        const parsed = JSON.parse(responseText);
        result.content = parsed;
      }
    } catch (e) {
      console.error("Error while wrapping response", e);
    }
    return result;
  }

  static backendPut(uri: string, data: any): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      const apiUrl = Constants.apiUrl(uri);
      const xhr = new XMLHttpRequest();
      xhr.open("PUT", apiUrl, true);
      xhr.withCredentials = true;
      xhr.onload = function() {
        if (this.status == 200 || this.status == 201) {
          const responseText = this["responseText"];
          try {
            const parsed = JSON.parse(responseText);
            resolve(parsed);
          } catch (syntaxError) {
            console.error(
              "Could not parse server response as JSON ",
              responseText
            );
            resolve(responseText);
          }
        } else if (this.status == 204) {
          resolve(undefined);
        } else {
          const result = BackendService.wrapResponseReject(this);
          reject(result);
        }
      };
      if (data != null) {
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.send(JSON.stringify(data));
      } else {
        xhr.send();
      }
    });
  }

  static backendDelete(uri: string, data?: any): Promise<void> {
    return new Promise<void>((resolve, reject) => {
      const apiUrl = Constants.apiUrl(uri);
      const xhr = new XMLHttpRequest();
      xhr.open("DELETE", apiUrl, true);
      xhr.withCredentials = true;
      xhr.onload = function() {
        if (this.status == 200 || this.status == 204) {
          resolve(undefined);
        } else {
          const result = BackendService.wrapResponseReject(this);
          reject(result);
        }
      };
      if (data != null) {
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.send(JSON.stringify(data));
      } else {
        xhr.send();
      }
    });
  }

  static backendPost(uri: string, data?: any): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      const apiUrl = Constants.apiUrl(uri);
      const xhr = new XMLHttpRequest();
      xhr.open("POST", apiUrl, true);
      xhr.withCredentials = true;
      xhr.onload = function() {
        if (this.status == 200 || this.status == 201) {
          const responseText = this["responseText"];
          try {
            const parsed = JSON.parse(responseText);
            resolve(parsed);
          } catch (syntaxError) {
            console.error(
              "Could not parse server response as JSON ",
              responseText
            );
            resolve(responseText);
          }
        } else if (this.status == 204) {
          resolve(undefined);
        } else {
          const result = BackendService.wrapResponseReject(this);
          reject(result);
        }
      };
      if (data != null) {
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.send(JSON.stringify(data));
      } else {
        xhr.send();
      }
    });
  }

  static backendPutPlain(uri: string, data: string): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      const apiUrl = Constants.apiUrl(uri);
      const xhr = new XMLHttpRequest();
      xhr.open("PUT", apiUrl, true);
      xhr.withCredentials = true;
      xhr.onload = function() {
        if (this.status == 200 || this.status == 201) {
          const responseText = this["responseText"];
          const parsed = JSON.parse(responseText);
          resolve(parsed);
        } else if (this.status == 204) {
          resolve(undefined);
        } else {
          const result = BackendService.wrapResponseReject(this);
          reject(result);
        }
      };
      if (data != null) {
        xhr.setRequestHeader("Content-Type", "text/plain");
        xhr.send(data);
      } else {
        xhr.send();
      }
    });
  }

  static timeout(ms: number, promise: Promise<any>): Promise<any> {
    // Create a promise that rejects in <ms> milliseconds
    const timeout = new Promise((resolve, reject) => {
      const id = setTimeout(() => {
        clearTimeout(id);
        const error = {
          timeoutReached: true,
          message: "Timed out in " + ms + "ms."
        };
        reject(error);
      }, ms);
    });

    // Returns a race between our timeout and the passed in promise
    return Promise.race([promise, timeout]);
  }
}
