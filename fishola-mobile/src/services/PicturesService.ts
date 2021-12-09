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
    console.info("Image enregistrée", id);
  }

  static deletePictureFromLocalDB(catchId: string) {
    this.getDatabase()
      .dirtyPictures.where("catch")
      .equals(catchId)
      .delete();
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
      PicturesService.internalGetPicturesFull(key).then((pictures) => {
        pictures.forEach(async (result) => {
          if (result.content) {
            const newId = map[key];
            console.debug(`On change l'ID de l'image ${key} -> ${newId}`);
            await PicturesService.savePictureInLocalDB(
              newId,
              result.catch,
              result.content,
              result.isMeasurementPicture,
              result.order,
              result.dirtySince
            );
            PicturesService.deletePictureFromLocalDB(key);
          }
        });
      });
    });
  }

  static syncPictures() {
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

        // TODO Gallery rewrite synchronisation with new routes
        /*
        // Pour chaque photo on créé une promise qui tente de la sauvegarder
        pictureIds
          .filter((pictureId) => pictureId.length == 36)
          .forEach((pictureId) => {
            const promise = this.syncPicture(pictureId);
            allPromises.push(promise);
            promise.then(() => {
              console.info(
                "Photo synchronisée, on la supprime de la base embarquée",
                pictureId
              );
              PicturesService.deletePictureFromLocalDB(pictureId);
            });
          });

        // Pour les photos qui ne correspondent pas à une capture sur le serveur : on les supprime au bout de 7j
        pictureIds
          .filter((pictureId) => pictureId.length != 36)
          .forEach((pictureId) => {
            PicturesService.internalGetPictureFull(pictureId).then((result) => {
              if (result.dirtySince) {
                const dirtySinceInMillis =
                  new Date().getTime() - result.dirtySince;
                if (dirtySinceInMillis > 1000 * 60 * 60 * 24 * 7) {
                  // Plus de 7j
                  console.info(
                    `On supprime la photo ${pictureId} qui n'est pas sauvegardée depuis ${result.dirtySince}`
                  );
                  PicturesService.deletePictureFromLocalDB(pictureId);
                  return;
                }
              }
            });
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
        }*/
      });
  }

  // TODO Galery : add Order
  static syncPicture(pictureId: string, order: number): Promise<void> {
    return new Promise((resolve, reject) => {
      console.info("On essaye de sauvegarder la photo", pictureId);
      PicturesService.internalGetPictureFullFromCatchAndOrder(
        pictureId,
        order
      ).then((matchingLocalPics) => {
        if (matchingLocalPics && matchingLocalPics.length > 0) {
          const result = matchingLocalPics[0];
          if (result.dirtySince) {
            const dirtySinceInMillis = new Date().getTime() - result.dirtySince;
            if (dirtySinceInMillis > 1000 * 60 * 60 * 24 * 7) {
              // Plus de 7j
              console.info(
                `On supprime la photo ${pictureId} qui n'est pas sauvegardée depuis ${result.dirtySince}`
              );
              PicturesService.deletePictureFromLocalDB(pictureId);
              reject("Photo non synchronisée depuis trop longtemps");
              return;
            }
          }
          if (result.content) {
            this.backendPutPlain(
              `/v1/pictures/${pictureId}`,
              result.content
            ).then(resolve, (error: any) => {
              console.error(
                `Erreur lors de la synchro de l'image ${pictureId}`,
                error
              );
              reject(error);
            });
          } else {
            reject(`Unable to find picture content ${pictureId}`);
          }
        } else {
          reject(`Unable to find picture content ${pictureId}`);
        }
      });
    });
  }
}
