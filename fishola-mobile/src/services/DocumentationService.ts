import {Editorial, DocumentationLight} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';

export default class DocumentationService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static getDocumentations():Promise<DocumentationLight[]> {
        return this.backendGetWithCache('/v1/documentations');
    }

    static getCredits():Promise<Editorial> {
        return this.backendGetWithCache('/v1/editorial/credits');
    }

}
