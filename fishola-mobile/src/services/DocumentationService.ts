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
import {Editorial, DocumentationLight} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';
import Constants from '@/services/Constants';

export default class DocumentationService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static getSamplesDocumentationUrl():string {
        const result = Constants.apiUrl('/v1/documentation/fixed/samples');
        return result;
    }

    static getCGUUrl():string {
        const result = Constants.apiUrl('/v1/documentation/fixed/cgu');
        return result;
    }

    static getDocumentations():Promise<DocumentationLight[]> {
        return this.backendGetWithCache('/v1/documentations');
    }

    static getNews():Promise<DocumentationLight[]> {
        return this.backendGetWithCache('/v1/news');
    }

    static getCredits():Promise<Editorial> {
        return this.backendGetWithCache('/v1/editorial/credits');
    }

    static getFaq():Promise<Editorial> {
        return this.backendGetWithCache('/v1/editorial/faq');
    }

    static prepareCaches():Promise<void> {
        const allPromises:Promise<void>[] = [
            this.prepareCache('/v1/documentations'),
            this.prepareCache('/v1/editorial/credits'),
            this.prepareCache('/v1/editorial/faq')
        ];
        return new Promise<void>((resolve, reject) => {
            Promise.all(allPromises).then(
                () => resolve()
                ,reject
            );
        });
    }


}
