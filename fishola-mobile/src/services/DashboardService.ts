import {Dashboard, SpeciesWithAlias} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';
import ReferentialService from './ReferentialService';

export class DashboardAndSpecies {
    constructor (
        public dashboard:Dashboard,
        public species:SpeciesWithAlias[]) {
    }
}

export default class DashboardService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static loadDashboard():Promise<DashboardAndSpecies> {
        return new Promise<DashboardAndSpecies>((resolve, reject) => {
            Promise
            .all(
                [
                    this.backendGetWithCache('/v1/dashboard'), 
                    ReferentialService.getAllSpecies()
                ])
            .then((data:[Dashboard, SpeciesWithAlias[]]) => {
                let result:DashboardAndSpecies = new DashboardAndSpecies(data[0], data[1]);
                resolve(result);
            },
            reject);
        });
    }

}
