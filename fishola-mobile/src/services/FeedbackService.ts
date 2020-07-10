/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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
import AbstractFisholaService from '@/services/AbstractFisholaService';
import { Feedback } from '@/pojos/BackendPojos';

/**
 * Service allowing to create feedback (locally) and send them to server when connected to internet.
 */
export default class FeedbackService extends AbstractFisholaService {

    constructor () {
        super();
    }

    /**
     * Stores the given feedback in local database.
     * It will be send to server later with a synchronisation mechanism.
     * @param feedback the feedbakc to store
     */
    static sendFeedback(feedback:Feedback): Promise<void> {

        this.getDatabase()
        .dirtyPictures
        .put(newPicture)
        .then(id => {
            console.info('Image enregistrée', id);
            callback();
        });


    }

    static syncFeedbacks() {

        this.getDatabase()
            .dirtyPictures
            .toCollection()
            .primaryKeys((pictureIds:string[]) => {

                if (pictureIds && pictureIds.length > 0) {
                    console.info("Liste des IDs de photos dans la base embarquée", pictureIds);
                }

                let allPromises:Promise<void>[] = [];

                // Pour chaque photo on créé une promise qui tente de la sauvegarder
                pictureIds
                    .filter((pictureId) => pictureId.length == 36)
                    .forEach((pictureId) => {
                        let promise = this.syncPicture(pictureId);
                        allPromises.push(promise);
                        promise.then(() => {
                            console.info("Photo synchronisée, on la supprime de la base embarquée", pictureId);
                            PicturesService.deletePicture(pictureId);
                        });
                    });

                // Pour les photos qui ne correspondent pas à une capture sur le serveur : on les supprime au bout de 7j
                pictureIds
                    .filter((pictureId) => pictureId.length != 36)
                    .forEach((pictureId) => {
                        PicturesService.getPictureFull(pictureId).then(result => {
                            if (result.dirtySince) {
                                let dirtySinceInMillis = new Date().getTime() - result.dirtySince;
                                if (dirtySinceInMillis > (1000 * 60 * 60 * 24 * 7)) { // Plus de 7j
                                    console.info(`On supprime la photo ${pictureId} qui n'est pas sauvegardée depuis ${result.dirtySince}`);
                                    PicturesService.deletePicture(pictureId);
                                    return;
                                }
                            }
                        });
                    });

                if (allPromises.length > 0) {
                    Promise
                        .all(allPromises)
                        .then(
                            () => {
                                console.info("Toutes les photos sont sauvegardées");
                            },
                            (eee) => {
                                console.error("Problème de synchro des images", eee);
                            });
                }

            });
    }

    static sendFeedbackToServer(feedback: Feedback):Promise<void> {
        return new Promise((resolve, reject) =>  {
            this.backendPut("/v1/feedback", feedback)
                .then(resolve, reject);
        });
    }

}
