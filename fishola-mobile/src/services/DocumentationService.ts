import {Editorial, DocumentationLight} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';
import Constants from '@/services/Constants';

export default class DocumentationService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static getSamplesDocumentationUrl():string {
        let result = Constants.apiUrl('/v1/documentation/fixed/samples');
        return result;
    }

    static getCGUUrl():string {
        let result = Constants.apiUrl('/v1/documentation/fixed/cgu');
        return result;
    }

    static getDocumentations():Promise<DocumentationLight[]> {
        return this.backendGetWithCache('/v1/documentations');
    }

    static getCredits():Promise<Editorial> {
        return this.backendGetWithCache('/v1/editorial/credits');
    }


    static prepareCaches():Promise<void> {
        let allPromises:Promise<void>[] = [
            this.prepareCache('/v1/documentations'),
            this.prepareCache('/v1/editorial/credits')
        ];
        return new Promise<void>((resolve, reject) => {
            Promise.all(allPromises).then(
                () => resolve()
                ,reject
            );
        });
    }


}
