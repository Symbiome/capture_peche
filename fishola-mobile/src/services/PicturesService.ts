import AbstractFisholaService from '@/services/AbstractFisholaService';
import StoredPicture from '@/pojos/StoredPicture';

export default class PicturesService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static savePicture(catchId:string, content:string, callback:()=>any, dirtySince?:number) {

        let newPicture:StoredPicture = {
            id: catchId,
            dirtySince: dirtySince || new Date().getTime(),
            content: content
        }

        this.getDatabase()
            .dirtyPictures
            .put(newPicture)
            .then(id => {
                console.info('Image enregistrée', id);
                callback();
            });
    }

    static deletePicture(catchId:string) {
        this.getDatabase()
            .dirtyPictures
            .delete(catchId);
    }

    static getPictureFull(catchId:string):Promise<StoredPicture> {
        
        return new Promise<StoredPicture>((resolve, reject) => {
            this.getDatabase()
            .dirtyPictures
            .get(catchId).then(
                (item?:StoredPicture) => {
                    if (item) {
                        resolve(item);
                    } else {
                        reject();
                    }
                },
                reject);
        });
    }

    static getPicture(catchId:string):Promise<string> {
        return new Promise<string>((resolve, reject) => {
            this.getPictureFull(catchId).then(
                (result) => {
                    if (result.content) {
                        resolve(result.content);
                    } else {
                        reject();
                    }
                },
                reject);
        });
    }

    static checkForPicturesToRename(map:any) {
        let keys:string[] = Object.keys(map);
        keys.forEach((key:string) => {
            PicturesService.getPictureFull(key).then(result => {
                if (result.content) {
                    let newId = map[key];
                    console.debug(`On change l'ID de l'image ${key} -> ${newId}`);
                    PicturesService.savePicture(
                        newId,
                        result.content,
                        () => {
                            PicturesService.deletePicture(key);
                        },
                        result.dirtySince);
                }
            })
        });
    }

    static syncPictures() {

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

    static syncPicture(pictureId:string):Promise<void> {
        return new Promise((resolve, reject) =>  {
            console.info("On essaye de sauvegarder la photo", pictureId);
            PicturesService.getPictureFull(pictureId).then(result => {
                if (result.dirtySince) {
                    let dirtySinceInMillis = new Date().getTime() - result.dirtySince;
                    if (dirtySinceInMillis > (1000 * 60 * 60 * 24 * 7)) { // Plus de 7j
                        console.info(`On supprime la photo ${pictureId} qui n'est pas sauvegardée depuis ${result.dirtySince}`);
                        PicturesService.deletePicture(pictureId);
                        reject("Photo non synchronisée depuis trop longtemps");
                        return;
                    }
                }
                if (result.content) {
                    this.backendPutPlain(`/v1/pictures/${pictureId}`, result.content)
                        .then(
                            resolve,
                            (error:any) => {
                                console.error(`Erreur lors de la synchro de l'image ${pictureId}`, error);
                                reject(error);
                            });
                } else {
                    reject(`Unable to find picture content ${pictureId}`);
                }
            });
        });
    }

}
