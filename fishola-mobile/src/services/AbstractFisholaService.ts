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
import Constants from '@/services/Constants';
import FisholaDatabase from './FisholaDatabase';
import OfflineEntry from '@/pojos/OfflineEntry';

class CacheEntry {
  constructor(public since:number, public content:any) {

  }
}

export default abstract class AbstractFisholaService {
  static getDatabase(): FisholaDatabase {
    return FisholaDatabase.getInstance();
  }

  static caches: Map<string, CacheEntry> = new Map();

  static clearCache(uri: string) {
    this.caches.delete(uri);
  }

  static pushToCache(uri: string, content: any) {
    const newEntry: CacheEntry = new CacheEntry(new Date().getTime(), content);
    this.caches.set(uri, newEntry);
  }

  /**
   * Une requête XHR qui échoue au niveau transport (réseau absent, hôte
   * injoignable, requête interrompue) ne déclenche PAS `onload`. Sans
   * gestionnaire d'erreur, la promesse correspondante ne se règle donc
   * JAMAIS — ni tenue, ni rompue.
   *
   * C'est ce qui bloquait la synchronisation : le push d'une sortie sans
   * réseau laissait le `Promise.all` de `doSyncDirtyTrips` en attente,
   * `syncTrips` ne se terminait pas, et son verrou `syncInProgress`
   * restait armé pour toute la durée de vie de l'application. Plus aucune
   * sortie ne repartait ensuite, même le réseau revenu, jusqu'au
   * redémarrage — la sortie restait affichée « Non synchronisée ».
   *
   * Le rejet est volontairement non numérique : `backendGetOrOfflineStorage`
   * distingue l'échec de transport (repli sur le cache local) d'une réponse
   * HTTP en erreur (statut numérique, propagé pour ne pas masquer une
   * session expirée).
   */
  static rejectOnTransportFailure(
    xhr: XMLHttpRequest,
    uri: string,
    reject: (cause: any) => void
  ) {
    const fail = (cause: string) => () => {
      console.error(`Échec de transport (${cause}) pour '${uri}'`);
      reject({
        networkError: true,
        message: "Impossible de contacter le serveur",
      });
    };
    xhr.onerror = fail("erreur réseau");
    xhr.onabort = fail("requête interrompue");
    xhr.ontimeout = fail("délai dépassé");
  }

  static backendGet(uri: string): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      const apiUrl = Constants.apiUrl(uri);
      const xhr = new XMLHttpRequest();
      xhr.open("GET", apiUrl, true);
      xhr.withCredentials = true;
      xhr.onload = function () {
        if (this.status == 200) {
          const responseText = this["responseText"];
          const parsed = JSON.parse(responseText);
          resolve(parsed);
        } else if (this.status == 204) {
          resolve(undefined);
        } else {
          reject(this.status);
        }
      };
      AbstractFisholaService.rejectOnTransportFailure(xhr, uri, reject);
      xhr.send();
    });
  }

  static backendGetWithCache(uri: string): Promise<any> {
    const entry = this.caches.get(uri);
    if (entry && new Date().getTime() - entry.since < 1000 * 60 * 60) {
      return Promise.resolve(entry.content);
    }

    return new Promise<any>((resolve, reject) => {
      // Repli hors ligne : le cache mémoire est vide au (re)démarrage de
      // l'app. Sans repli sur le stockage local, tous les référentiels servis
      // par cette méthode (météos, techniques, espèces…) rejetaient sans
      // réseau, ce qui faisait échouer les `Promise.all` des formulaires de
      // saisie — plus aucun plan d'eau ni espèce proposé hors ligne.
      this.backendGetOrOfflineStorage(uri).then((content: any) => {
        const newEntry: CacheEntry = new CacheEntry(
          new Date().getTime(),
          content
        );
        this.caches.set(uri, newEntry);
        resolve(content);
      }, reject);
    });
  }

  static backendGetWithArgs(uri: string, args: any): Promise<any> {
    return new Promise<any>((resolve, reject) => {
      let apiUrl = Constants.apiUrl(uri);
      const xhr = new XMLHttpRequest();

      const queryString = Object.keys(args)
        .map((k) => encodeURIComponent(k) + "=" + encodeURIComponent(args[k]))
        .join("&");
      apiUrl += "?" + queryString;

      xhr.open("GET", apiUrl, true);
      xhr.withCredentials = true;
      AbstractFisholaService.rejectOnTransportFailure(xhr, uri, reject);
      xhr.onload = function () {
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
          const result = AbstractFisholaService.wrapResponseReject(this);
          reject(result);
        }
      };
      xhr.send();
    });
  }

  static wrapResponseReject(xhr: XMLHttpRequest): any {
    const result = {
      status: xhr.status,
      content: undefined,
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
      AbstractFisholaService.rejectOnTransportFailure(xhr, uri, reject);
      xhr.onload = function () {
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
          const result = AbstractFisholaService.wrapResponseReject(this);
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
      AbstractFisholaService.rejectOnTransportFailure(xhr, uri, reject);
      xhr.onload = function () {
        if (this.status == 200 || this.status == 204) {
          resolve(undefined);
        } else {
          const result = AbstractFisholaService.wrapResponseReject(this);
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
      AbstractFisholaService.rejectOnTransportFailure(xhr, uri, reject);
      xhr.onload = function () {
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
          const result = AbstractFisholaService.wrapResponseReject(this);
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
      AbstractFisholaService.rejectOnTransportFailure(xhr, uri, reject);
      xhr.onload = function () {
        if (this.status == 200) {
          const responseText = this["responseText"];
          const parsed = JSON.parse(responseText);
          resolve(parsed);
        } else if (this.status == 204 || this.status == 201) {
          resolve(undefined);
        } else {
          const result = AbstractFisholaService.wrapResponseReject(this);
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
          message: "Timed out in " + ms + "ms.",
        };
        reject(error);
      }, ms);
    });

    // Returns a race between our timeout and the passed in promise
    return Promise.race([promise, timeout]);
  }

  static backendGetAndStoreToOfflineStorage(uri: string): Promise<any> {
    return new Promise<string>((resolve, reject) => {
      const promise = this.backendGet(uri);
      promise.then(
        (result) => {
          console.info(
            `New content available, save it to offline storage for '${uri}'`,
            result
          );
          const entry: OfflineEntry = {
            key: uri,
            content: result,
          };
          this.getDatabase().offlineStorage.put(entry);
          resolve(result);
        },
        (error) => {
          console.error(`Error loading from the backend for '${uri}'`, error);
          reject(error);
        }
      );
    });
  }

  static unmarkOffline(input: any) {
    if (input && typeof input === "object") {
      delete input.offlineMarker;
    }
  }

  static markOffline(input: any) {
    if (input && typeof input === "object") {
      input.offlineMarker = true;
    }
  }

  static deleteFromOfflineStorage(uri: string) {
    this.getDatabase().offlineStorage.delete(uri);
    this.caches.clear();
  }

  static backendGetOrOfflineStorage(uri: string): Promise<any> {
    return new Promise<string>((resolve, reject) => {
      const promise = this.backendGetAndStoreToOfflineStorage(uri);
      this.timeout(5000, promise).then(
        (result) => {
          console.info(`Got fresh answer for '${uri}'`, result);
          this.unmarkOffline(result);
          resolve(result);
        },
        (error) => {
          // Repli sur le cache local dès qu'on n'a PAS pu joindre le serveur :
          // timeout (réseau lent) mais aussi échec de transport (réseau absent,
          // hôte injoignable) — `backendGet` rejette alors avec un message, pas
          // avec un statut. Sans ce second cas, une coupure franche ne
          // déclenchait jamais le repli et déconnectait l'utilisateur.
          // Une réponse HTTP en erreur (rejet = statut numérique : 401, 403,
          // 5xx) reste propagée : le serveur a répondu, le cache ne doit pas
          // masquer une session expirée.
          const serverAnswered = typeof error === "number";
          if (!serverAnswered) {
            console.error(
              `Unable to load from the backend for '${uri}'`,
              JSON.stringify(error)
            );
            this.getDatabase()
              .offlineStorage.get(uri)
              .then((entry?: OfflineEntry) => {
                if (entry) {
                  const content = entry.content;
                  this.markOffline(content);
                  resolve(content);
                } else {
                  reject(`No offline entry for ${uri}`);
                }
              }, reject);
          } else {
            reject(error);
          }
        }
      );
    });
  }

  static prepareCache(uri: string): Promise<void> {
    return new Promise<void>((resolve, reject) => {
      const lakes = this.backendGetOrOfflineStorage(uri);
      lakes.then((data) => {
        this.pushToCache(uri, data);
        resolve();
      }, reject);
    });
  }
}

