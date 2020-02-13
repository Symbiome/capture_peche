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
                console.log('Image enregistrée');
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

    static checkForPicturesToSync() {

        this.getInstance()
            .dirtyPictures
            .toArray((allPictures) => {
                console.log(allPictures);



                    // // TODO Athimel 11/02/2020 À mettre à un endroit adéquate
                    // console.log(`Tente de l'upload de l'image ${newId}`);
                    // this.getInstance().backendPutPlain(`/v1/pictures/${newId}`, content, () => {
                    //     console.log("it's over");
                    //     PicturesService.deletePicture(newId);
                    // });
            });
    }

}
