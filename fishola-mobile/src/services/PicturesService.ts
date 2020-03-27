import AbstractFisholaService from '@/services/AbstractFisholaService';

export default class PicturesService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static savePicture(catchId:string, content:string, callback:()=>any, dirtySince?:number) {

        let newPicture = {
            id: catchId,
            content: content,
            dirtySince: dirtySince || new Date().getTime()
        }

        this.getDatabase()
            .dirtyPictures
            .put(newPicture)
            .then(id => {
                console.log('Image enregistrée', id);
                callback();
            });
    }

    static deletePicture(catchId:string) {
        this.getDatabase()
            .dirtyPictures
            .delete(catchId);
    }

    static getPictureFull(catchId:string, callback:(content?:string, dirtySince?:number)=>any) {
        
        this.getDatabase()
            .dirtyPictures
            .get(catchId)
            .then((item:any) => {
                callback(item.content, item.dirtySince);
            })
            .catch(() => {
                console.log("Image non trouvée");
                callback();
            });
    }

    static getPicture(catchId:string):Promise<string> {
        return new Promise<string>((resolve, reject) => {
            this.getPictureFull(catchId, (content?, dirtySince?) => {
                if (content) {
                    resolve(content);
                } else {
                    reject();
                }
            });
        });
    }

    static checkForPicturesToRename(map:any) {
        let keys:string[] = Object.keys(map);
        keys.forEach((key:string) => {
            PicturesService.getPictureFull(key, (content?, dirtySince?) => {
                if (content) {
                    let newId = map[key];
                    console.log(`On change l'ID de l'image ${key} -> ${newId}`);
                    PicturesService.savePicture(newId, content, () => {
                        PicturesService.deletePicture(key);
                    }, dirtySince);
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
                    console.log("Liste des IDs de photos dans la base embarquée", pictureIds);
                }

                let allPromises:Promise<void>[] = [];

                // Pour chaque photo on créé une promise qui tente de la sauvegarder
                pictureIds
                    .filter((pictureId) => pictureId.length == 36)
                    .forEach((pictureId) => {
                        let promise = this.syncPicture(pictureId);
                        allPromises.push(promise);
                        promise.then(() => {
                            console.log("Photo synchronisée, on la supprime de la base embarquée", pictureId);
                            PicturesService.deletePicture(pictureId);
                        });
                    });

                // Pour les photos qui ne correspondent pas à une capture sur le serveur : on les supprime au bout de 7j
                pictureIds
                    .filter((pictureId) => pictureId.length != 36)
                    .forEach((pictureId) => {
                        PicturesService.getPictureFull(pictureId, (content?, dirtySince?) => {
                            if (dirtySince) {
                                let dirtySinceInMillis = new Date().getTime() - dirtySince;
                                if (dirtySinceInMillis > (1000 * 60 * 60 * 24 * 7)) { // Plus de 7j
                                    console.log(`On supprime la photo ${pictureId} qui n'est pas sauvegardée depuis ${dirtySince}`);
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
                                console.log("Toutes les photos sont sauvegardées");
                            },
                            (eee) => {
                                console.log("Problème de synchro des images", eee);
                            });
                }

            });
    }

    static syncPicture(pictureId:string):Promise<void> {
        return new Promise((resolve, reject) =>  {
            console.log("On essaye de sauvegarder la photo", pictureId);
            PicturesService.getPictureFull(pictureId, (content?, dirtySince?) => {
                if (dirtySince) {
                    let dirtySinceInMillis = new Date().getTime() - dirtySince;
                    if (dirtySinceInMillis > (1000 * 60 * 60 * 24 * 7)) { // Plus de 7j
                        console.log(`On supprime la photo ${pictureId} qui n'est pas sauvegardée depuis ${dirtySince}`);
                        PicturesService.deletePicture(pictureId);
                        reject("Photo non synchronisée depuis trop longtemps");
                        return;
                    }
                }
                if (content) {
                    this.backendPutPlain(`/v1/pictures/${pictureId}`, content, resolve, reject);
                } else {
                    reject(`Unable to find picture content ${pictureId}`);
                }
            });
        });
    }

}
