import {Editorial, DocumentationLight} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';

export default class DocumentationService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static getSampleBuyPonitsDocumentation():Promise<DocumentationLight> {
        return new Promise<DocumentationLight>((resolve, reject) => {
            this.getDocumentations()
                .then((allDocs:DocumentationLight[]) => {
                    let found = false;
                    allDocs.forEach((doc) => {
                        if (doc.name == 'Documentation sur les prélèvements') {
                            resolve(doc);
                            found = true;
                        }
                    });
                    if (!found) {
                        reject("Not Found");
                    }
                }, reject);
        });
    }

    static getDocumentations():Promise<DocumentationLight[]> {
        return this.backendGetWithCache('/v1/documentations');
    }

    static getCredits():Promise<Editorial> {
        return this.backendGetWithCache('/v1/editorial/credits');
    }

}
