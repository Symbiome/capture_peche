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
import { Lake } from "@/pojos/BackendPojos";
import AbstractFisholaService from "@/services/AbstractFisholaService";
import ReferentialService from "@/services/ReferentialService";

import { Device, DeviceInfo } from "@capacitor/device";
import { Geolocation, CallbackID, Position } from "@capacitor/geolocation";

export class CoordsAndLake {
  constructor(
    public latitude: number,
    public longitude: number,
    public lake: Lake
  ) {}
}

export default class GeolocationService extends AbstractFisholaService {
  static watchId?: CallbackID;
  static latestPosition?: Position;
  static latestError?: any;
  static notifiedPositionDisabled: boolean = false;

  constructor() {
    super();
  }

  static canUsePosition(): Promise<void> {
    return new Promise<void>((resolve, reject) => {
      Device.getInfo().then((device: DeviceInfo) => {
        const isNotSecured: boolean =
          device.platform == "web" && window.location.protocol == "http:";
        const isDesktopBrowser: boolean =
          device.platform == "web" &&
          device.operatingSystem != "android" &&
          device.operatingSystem != "ios";

        console.info("isNotSecured", isNotSecured);
        console.info("isDesktopBrowser", isDesktopBrowser);
        console.info("device", device);

        if (isNotSecured) {
          reject("Pas de Geolocation hors contexte sécurisé");
        } else if (isDesktopBrowser) {
          reject("Pas de Geolocation sans smartphone");
        } else {
          resolve();
        }
      }, reject);
    });
  }

  static startWatchingPosition(): Promise<boolean> {
    return new Promise<boolean>((resolve, reject) => {
      this.canUsePosition().then(async () => {
        if (GeolocationService.watchId) {
          console.error("Il y a déjà un watcher en cours");
          resolve(false);
        } else {
          const options = {
            enableHighAccuracy: false,
            maximumAge: 20,
            timeout: 3000,
          };
          const watchId: CallbackID = await Geolocation.watchPosition(
            options,
            this.receivePosition
          );
          GeolocationService.watchId = watchId;
          resolve(true);
        }
      }, reject);
    });
  }

  static stopWatchingPosition() {
    console.debug(
      "About to stop watching position",
      GeolocationService.watchId
    );
    if (GeolocationService.watchId) {
      Geolocation.clearWatch({ id: GeolocationService.watchId });
      delete GeolocationService.watchId;
    }
  }

  static receivePosition(position?: Position | null, error?: any) {
    if (position && position != null) {
      console.debug("Received position", JSON.stringify(position));
      GeolocationService.latestPosition = position;
      delete GeolocationService.latestError;
    } else {
      console.debug("Error receiving position", error);
      // delete GeolocationService.latestPosition;
      GeolocationService.latestError = error;
      GeolocationService.stopWatchingPosition();
    }
  }

  static getInstantPosition(): Promise<Position> {
    return new Promise<Position>((resolve, reject) => {
      if (this.latestError) {
        reject(this.latestError);
      } else if (this.latestPosition) {
        const age = new Date().getTime() - this.latestPosition.timestamp;
        console.debug("Âge de la position", age);
        resolve(this.latestPosition);
      } else {
        reject("Did you start the watcher before?");
      }
    });
  }

  static getPositionWithRetryUntilTimeout(ms: number): Promise<Position> {
    console.debug("getPositionWithRetryUntilTimeout", ms);
    if (ms < 0) {
      return Promise.reject("Timeout");
    }
    return new Promise<Position>((resolve, reject) => {
      const startedAt = new Date().getTime();
      GeolocationService.getInstantPosition().then(
        (result) => {
          resolve(result);
        },
        (error) => {
          if (
            error &&
            error.message &&
            error.message.indexOf("User denied") != -1
          ) {
            reject(error);
          } else if (
            error &&
            error.message &&
            error.message.indexOf("location unavailable") != -1
          ) {
            reject(error);
          } else {
            setTimeout(() => {
              const remaing = ms - (new Date().getTime() - startedAt);
              GeolocationService.getPositionWithRetryUntilTimeout(remaing).then(
                resolve,
                reject
              );
            }, 200);
          }
        }
      );
    });
  }

  static checkWatchAndGetPositionUntilTimeout(ms?: number): Promise<Position> {
    console.debug("GeolocationService.watchId", GeolocationService.watchId);
    if (!GeolocationService.watchId) {
      this.startWatchingPosition();
      delete GeolocationService.latestError;
    }
    const result = GeolocationService.getPositionWithRetryUntilTimeout(
      ms || 2000
    );
    return result;
  }

  static getClosestLake(): Promise<Lake> {
    return new Promise<Lake>((resolve, reject) => {
      Promise.all([
        ReferentialService.getLakes(),
        GeolocationService.checkWatchAndGetPositionUntilTimeout(3000),
      ]).then((data: [Lake[], Position]) => {
        const lakes: Lake[] = data[0];

        if (!lakes || lakes.length == 0) {
          reject();
        } else {
          const position: Position = data[1];

          console.info("lakes", lakes);
          console.info("position", position);

          const latitude = position.coords.latitude;
          const longitude = position.coords.longitude;

          const closestLake = GeolocationService.chooseClosestLake(
            lakes,
            latitude,
            longitude
          );
          resolve(closestLake);
        }
      }, reject);
    });
  }

  static computeDistance(
    latitudeA: number,
    longitudeA: number,
    latitudeB: number,
    longitudeB: number
  ): number {
    const result = Math.sqrt(
      Math.pow(latitudeB - latitudeA, 2) + Math.pow(longitudeB - longitudeA, 2)
    );
    return result;
  }

  static chooseClosestLake(
    lakes: Lake[],
    latitude: number,
    longitude: number
  ): Lake {
    let result: Lake = lakes[0];
    let minDistance = 999999;
    lakes.forEach((lake) => {
      const distance = this.computeDistance(
        lake.latitude,
        lake.longitude,
        latitude,
        longitude
      );
      if (distance < minDistance) {
        minDistance = distance;
        result = lake;
      }
      console.debug(`Distance pour le ${lake.name}: ${distance}`);
    });
    console.debug(`Winner is: ${result.name}`);
    return result;
  }

  static getClosestLakeMock(): Promise<Lake> {
    return new Promise<Lake>((resolve, reject) => {
      const promises = [ReferentialService.getLakes()];
      Promise.all(promises).then((data: any[]) => {
        const lakes: Lake[] = data[0];

        if (!lakes || lakes.length == 0) {
          reject();
        } else {
          console.debug("lakes", lakes);

          // Les Sorinières
          const latitude = 47.1464206;
          const longitude = -1.5245706;

          // // Seynod
          // const latitude = 45.8345;
          // const longitude = 6.0974;

          // // Thonnon-les-bains
          // const latitude = 46.3185;
          // const longitude = 6.4764;

          // // Alby-sur-Chéran
          // const latitude = 45.7618;
          // const longitude = 6.0205;

          const result = GeolocationService.chooseClosestLake(
            lakes,
            latitude,
            longitude
          );
          resolve(result);
        }
      }, reject);
    });
  }
}
