import { CatchBean, PicturePerTripBean, TripBean } from "@/pojos/BackendPojos";
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
import AbstractFisholaService from "@/services/AbstractFisholaService";
import StoredPicture from "@/pojos/StoredPicture";
import PictureContentWithOrder from "@/pojos/PictureContentWithOrder";
import TripsService from "./TripsService";
import { pick } from "lodash";

export default class PicturesService extends AbstractFisholaService {
  constructor() {
    super();
  }

  static async savePictureInLocalDB(
    id: string,
    catchId: string,
    content: string,
    isMeasurementPicture: boolean,
    order: number,
    dirtySince?: number
  ): Promise<void> {
    const newPicture: StoredPicture = {
      id: id,
      catch: catchId,
      dirtySince: dirtySince || new Date().getTime(),
      content: content,
      isMeasurementPicture: isMeasurementPicture,
      order: order,
    };
    const savedId = await this.getDatabase().dirtyPictures.put(newPicture);
    if (isMeasurementPicture) {
      await this.getDatabase().lastMeasurementPic.clear();
      await this.getDatabase().lastMeasurementPic.put(newPicture);
    }
    console.info("Image enregistrée", id);
  }

  static async hasAtLeastOnePicWithMarker(): Promise<Boolean> {
    const mostRecentPicWithMarker =
      await this.getDatabase().lastMeasurementPic.toArray();
    return mostRecentPicWithMarker.length > 0;
  }

  static deleteAllPicturesForCatch(catchId: string) {
    this.getDatabase().dirtyPictures.where("catch").equals(catchId).delete();
  }

  static deleteSinglePictureFromLocalDB(picId: string) {
    return this.getDatabase().dirtyPictures.where("id").equals(picId).delete();
  }

  static async deletePicture(
    picId: string,
    catchId: string,
    order: number
  ): Promise<void> {
    // Check if picture exists in local db
    const matchingLocalPic = await this.internalGetPictureFullFromCatchAndOrder(
      catchId,
      order
    );
    if (matchingLocalPic.length) {
      // Delete from local storage
      await this.deleteSinglePictureFromLocalDB(picId);
      console.info("Deleted local storage picture " + picId);
      return;
    } else {
      // Store it as toDelete on server-side during next syncPictures()
      const pictureToDelete: StoredPicture = {
        id: picId,
        catch: catchId,
        dirtySince: new Date().getTime(),
        content: "",
        isMeasurementPicture: false,
        order: order,
      };
      await this.getDatabase().toDeletePictures.put(pictureToDelete);
      console.info(
        "Remote picture stored as to delete at next trip sync " + picId
      );

      return;
    }
  }

  static getAboutToBeDeletedPictures(
    catchId: string
  ): Promise<StoredPicture[]> {
    const toDeletePicturesWithCatchId = this.getDatabase()
      .toDeletePictures.where("catch")
      .equals(catchId);
    return toDeletePicturesWithCatchId.toArray();
  }

  static internalGetPicturesFull(catchId: string): Promise<StoredPicture[]> {
    const picturesWithCatchId = this.getDatabase()
      .dirtyPictures.where("catch")
      .equals(catchId);
    return picturesWithCatchId.toArray();
  }

  static async internalGetPictureFullFromCatchAndOrder(
    catchId: string,
    order: number
  ): Promise<StoredPicture[]> {
    const picWithCatchIdAndOrder = this.getDatabase()
      .dirtyPictures.where("catch")
      .equals(catchId)
      .filter((storedPic) => storedPic.order == order);
    return picWithCatchIdAndOrder.toArray();
  }

  static async internalGetPictureFullFromPictureId(
    pictureId: string
  ): Promise<StoredPicture[]> {
    const picWithCatchIdAndOrder = this.getDatabase()
      .dirtyPictures.where("id")
      .equals(pictureId);
    return picWithCatchIdAndOrder.toArray();
  }

  static async getPicturesFromLocalDB(
    catchId: string
  ): Promise<PictureContentWithOrder[]> {
    const storedPictures = await PicturesService.internalGetPicturesFull(
      catchId
    );
    storedPictures.sort((pic1, pic2) => {
      return pic2.order - pic1.order;
    });
    return storedPictures as PictureContentWithOrder[];
  }

  static checkForPicturesToRename(map: any) {
    const keys: string[] = Object.keys(map);
    keys.forEach((key: string) => {
      console.error("Trip saved, checking pictures for catch " + key);
      PicturesService.internalGetPicturesFull(key).then(async (pictures) => {
        console.error("Found " + pictures.length + " pictures");
        for (let i = 0; i < pictures.length; i++) {
          const pic = pictures[i];
          if (pic.content) {
            const newCatchId = map[key];
            console.debug(`On change l'ID de l'image ${key} -> ${newCatchId}`);
            await PicturesService.savePictureInLocalDB(
              newCatchId + pic.order,
              newCatchId,
              pic.content,
              pic.isMeasurementPicture,
              pic.order,
              pic.dirtySince
            );
          }
        }
        PicturesService.deleteAllPicturesForCatch(key);
      });
    });
  }

  static async syncPictures() {
    // Step 1: get trips for which save is delayed (because corresponding pictures should not be sent yet)
    const allDirtyTrips = await this.getDatabase().dirtyTrips.toArray();
    const tripsWithDelayedSaved: TripBean[] = allDirtyTrips.filter((trip) => {
      return TripsService.shouldDelaySave(trip);
    });
    // Get corresponding catches
    const catchesWithDelayedSaved: CatchBean[] = tripsWithDelayedSaved
      .map((trip) => trip.catchs)
      .flat();
    const catchesWithDelayedsavedIds = catchesWithDelayedSaved.map(
      (aCatch) => aCatch.id
    );

    // Step 2: get pictures to save
    this.getDatabase()
      .dirtyPictures.toCollection()
      .primaryKeys((pictureIds: string[]) => {
        if (pictureIds && pictureIds.length > 0) {
          console.info(
            "Liste des IDs de photos dans la base embarquée",
            pictureIds
          );
        }

        const allPromises: Promise<void>[] = [];
        // Pour chaque photo on créé une promise qui tente de la sauvegarder
        pictureIds
          .filter((pictureId) => pictureId.length >= 36)
          .forEach((pictureId) => {
            const promise = this.syncPicture(
              pictureId,
              catchesWithDelayedsavedIds
            );
            allPromises.push(promise);
          });

        // Pour les photos qui ne correspondent pas à une capture sur le serveur : on les supprime au bout de 7j
        pictureIds
          .filter((pictureId) => pictureId.length < 36)
          .forEach((pictureId) => {
            PicturesService.internalGetPictureFullFromPictureId(pictureId).then(
              (matchingPics) => {
                if (matchingPics.length) {
                  const result = matchingPics[0];
                  if (result.dirtySince) {
                    const dirtySinceInMillis =
                      new Date().getTime() - result.dirtySince;
                    if (dirtySinceInMillis > 1000 * 60 * 60 * 24 * 7) {
                      // Plus de 7j
                      console.info(
                        `On supprime la photo ${pictureId} qui n'est pas sauvegardée depuis ${result.dirtySince}`
                      );
                      PicturesService.deleteSinglePictureFromLocalDB(pictureId);
                    }
                  }
                }
              }
            );
          });

        if (allPromises.length > 0) {
          Promise.all(allPromises).then(
            () => {
              console.info("Toutes les photos sont sauvegardées");
            },
            (eee) => {
              console.error("Problème de synchro des images", eee);
            }
          );
        }
      });

    // Step 3: get pictures to delete and delete them on server
    const toDeletePictures =
      await this.getDatabase().toDeletePictures.toArray();
    for (let i = 0; i < toDeletePictures.length; i++) {
      const toDeletePic = toDeletePictures[i];
      // First check if photo deletion should be delayed (as part of a delayed trip)
      if (!catchesWithDelayedsavedIds.includes(toDeletePic.catch)) {
        const deleteUrl = `/v1/pictures/${toDeletePic.catch}/${toDeletePic.order}`;
        try {
          await this.backendDelete(deleteUrl);
          await this.getDatabase()
            .toDeletePictures.where("id")
            .equals(toDeletePic.id)
            .delete();
          console.info("Deleted remote picture " + deleteUrl);
        } catch (error) {
          console.error(error);
          // Silent catch, delete will be tried again later
        }
      }
    }
  }

  static async syncPicture(
    pictureId: string,
    catchesWithDelayedsavedIds: string[]
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      console.info("On essaye de sauvegarder la photo " + pictureId);
      PicturesService.internalGetPictureFullFromPictureId(pictureId).then(
        (matchingLocalPics) => {
          if (matchingLocalPics && matchingLocalPics.length > 0) {
            const result = matchingLocalPics[0];

            // First check if photo sending should be delayed (as part of a delayed trip)
            if (!catchesWithDelayedsavedIds.includes(result.catch)) {
              if (result.dirtySince) {
                const dirtySinceInMillis =
                  new Date().getTime() - result.dirtySince;
                if (dirtySinceInMillis > 1000 * 60 * 60 * 24 * 7) {
                  // Plus de 7j
                  console.info(
                    `On supprime la photo ${pictureId} qui n'est pas sauvegardée depuis ${result.dirtySince}`
                  );
                  PicturesService.deleteSinglePictureFromLocalDB(pictureId);
                  reject("Photo non synchronisée depuis trop longtemps");
                  return;
                }
              }
              if (result.content) {
                let putUrl = `/v1/pictures/${result.catch}/${result.order}`;
                if (result.isMeasurementPicture) {
                  putUrl = `/v1/pictures/measure/${result.catch}`;
                }
                this.backendPutPlain(putUrl, result.content).then(
                  () => {
                    console.info(
                      "Photo synchronisée, on la supprime de la base embarquée",
                      pictureId
                    );
                    PicturesService.deleteSinglePictureFromLocalDB(pictureId);
                  },
                  (error: any) => {
                    console.error(
                      `Erreur lors de la synchro de l'image ${pictureId}`,
                      error
                    );
                    reject(error);
                  }
                );
              } else {
                reject(`Unable to find picture content ${pictureId}`);
              }
            } else {
              console.info(
                `${pictureId} save is delayed as part of a delayed trip`
              );
              resolve();
            }
          } else {
            reject(`Unable to find picture content ${pictureId}`);
          }
        }
      );
    });
  }

  static async getAllPicsPerYear(): Promise<Map<number, PicturePerTripBean>> {
    const onlinePics: Map<number, PicturePerTripBean> = await this.backendGet(
      "/v1/pictures/all-pictures"
    );
    return onlinePics;
  }
}
