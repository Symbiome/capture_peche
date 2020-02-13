import AbstractFisholaService from '@/services/AbstractFisholaService';

export default class PicturesService extends AbstractFisholaService {

    static instance?:PicturesService;

    constructor () {
        super();
    }

    static getInstance():PicturesService {
        if (!this.instance) {
            console.log("Pas encore d'instance partagée, on la créé");
            this.instance = new PicturesService();
        }
        return this.instance;
    }

    static savePicture(catchId:string, content:string, callback:()=>any) {

        let newPicture = {
            id: catchId,
            content: content,
        }

        this.getInstance()
            .dirtyPictures
            .put(newPicture)
            .then(id => {
                console.log('Image enregistrée', id);
                callback();
            });
    }

    static deletePicture(catchId:string) {
        this.getInstance()
            .dirtyPictures
            .delete(catchId);
    }

    static getPicture(catchId:string, callback:(content?:string)=>any) {
        
        this.getInstance()
            .dirtyPictures
            .get(catchId)
            .then((item:any) => {
                callback(item.content);
            })
            .catch(() => {
                console.log("Image non trouvée");
                callback();
            });
    }

    static checkForPicturesToRename(map:any) {
        let keys:string[] = Object.keys(map);
        keys.forEach((key:string) => {
            PicturesService.getPicture(key, (content?) => {
                if (content) {
                    let newId = map[key];
                    console.log(`On change l'ID de l'image ${key} -> ${newId}`);
                    PicturesService.savePicture(newId, content, () => {
                        PicturesService.deletePicture(key);
                    });
                }
            })
        });
    }

    static syncPictures() {

        this.getInstance()
            .dirtyPictures
            .toCollection()
            .primaryKeys((pictureIds:string[]) => {

                console.log("Liste des IDs de photos dans la base embarquée", pictureIds);
                let allPromises:Promise<void>[] = [];

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
            PicturesService.getPicture(pictureId, (content?) => {
                if (content) {
                    this.getInstance().backendPutPlain(`/v1/pictures/${pictureId}`, content, resolve, reject);
                } else {
                    reject(`Unable to find picture content ${pictureId}`);
                }
            });
        });
    }

}
