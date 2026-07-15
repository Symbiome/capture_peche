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
import Dexie from "dexie";
import { TripBean, Feedback } from "@/pojos/BackendPojos";
import StoredPicture from "@/pojos/StoredPicture";
import OfflineEntry from "@/pojos/OfflineEntry";

export default class FisholaDatabase extends Dexie {
  static instance: FisholaDatabase = new FisholaDatabase();

  // Declare implicit table properties.
  // (just to inform Typescript. Instanciated by Dexie in stores() method)
  onCreationTrip: Dexie.Table<any, string>;
  dirtyTrips: Dexie.Table<TripBean, string>;
  dirtyPictures: Dexie.Table<StoredPicture, string>;
  toDeletePictures: Dexie.Table<StoredPicture, string>;
  offlineStorage: Dexie.Table<OfflineEntry, string>;
  offlineFeedbacks: Dexie.Table<Feedback, string>;
  lastMeasurementPic: Dexie.Table<StoredPicture, string>;

  constructor() {
    super("Fishola");
    console.info("Construction de la base Fishola ...");
    this.version(1).stores({
      onCreationTrip: "id",
      dirtyTrips: "id",
    });
    this.version(2).stores({
      onCreationTrip: "id",
      dirtyTrips: "id",
      dirtyPictures: "id",
    });
    this.version(3).stores({
      onCreationTrip: "id",
      dirtyTrips: "id",
      dirtyPictures: "id",
      offlineStorage: "key",
    });
    this.version(4).stores({
      onCreationTrip: "id",
      dirtyTrips: "id",
      dirtyPictures: "id",
      offlineStorage: "key",
      offlineFeedbacks: "id",
    });

    // As StoredPicture model has changed (new fields), new version is required even though collections did not change
    this.version(5).stores({
      onCreationTrip: "id",
      dirtyTrips: "id",
      dirtyPictures: "id, order, catch",
      offlineStorage: "key",
      offlineFeedbacks: "id",
    });

    this.version(6).stores({
      onCreationTrip: "id",
      dirtyTrips: "id",
      dirtyPictures: "id, order, catch",
      toDeletePictures: "id, order, catch",
      offlineStorage: "key",
      offlineFeedbacks: "id",
    });

    this.version(7).stores({
      onCreationTrip: "id",
      dirtyTrips: "id",
      dirtyPictures: "id, order, catch",
      toDeletePictures: "id, order, catch",
      offlineStorage: "key",
      offlineFeedbacks: "id",
      lastMeasurementPic: "id",
    });

    // v8 (#10, mode offline) : les stores ne changent pas, mais les sorties déjà
    // en attente de synchronisation (créées avant #10) n'ont pas de statut hydro.
    // On les marque PENDING pour qu'elles soient re-validées côté serveur au
    // prochain push (le createTrip backend recalcule l'attribution). Un bump de
    // version est requis même sans changement de schéma pour exécuter l'upgrade.
    this.version(8)
      .stores({
        onCreationTrip: "id",
        dirtyTrips: "id",
        dirtyPictures: "id, order, catch",
        toDeletePictures: "id, order, catch",
        offlineStorage: "key",
        offlineFeedbacks: "id",
        lastMeasurementPic: "id",
      })
      .upgrade((tx) =>
        tx
          .table("dirtyTrips")
          .toCollection()
          .modify((trip: any) => {
            if (!trip.hydroValidation) {
              trip.hydroValidation = "PENDING";
            }
          })
      );

    this.onCreationTrip = this.table("onCreationTrip");
    this.dirtyTrips = this.table("dirtyTrips");
    this.dirtyPictures = this.table("dirtyPictures");
    this.toDeletePictures = this.table("toDeletePictures");
    this.offlineStorage = this.table("offlineStorage");
    this.offlineFeedbacks = this.table("offlineFeedbacks");
    this.lastMeasurementPic = this.table("lastMeasurementPic");

    console.info("Base Fishola prête");
  }

  static getInstance(): FisholaDatabase {
    return this.instance;
  }
}
