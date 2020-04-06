import {Dashboard, SpeciesWithAlias} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';
import ReferentialService from './ReferentialService';
import Helpers from '@/pojos/Helpers';

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

    static parseDate(input:any):Date {
        let result = Helpers.parseLocalDate(input);
        return result;
    }

    static loadDashboard():Promise<DashboardAndSpecies> {
        return new Promise<DashboardAndSpecies>((resolve, reject) => {
            Promise
            .all(
                [
                    this.backendGet('/v1/dashboard'),
                    ReferentialService.getAllSpeciesNoCache()
                ])
            .then((data:[Dashboard, SpeciesWithAlias[]]) => {
                if (data[0].latestTripsCatchs) {
                    data[0].latestTripsCatchs.forEach((trip) => {
                        trip.day = this.parseDate(trip.day);
                    });
                }
                let result:DashboardAndSpecies = new DashboardAndSpecies(data[0], data[1]);
                resolve(result);
            },
            reject);
        });
    }

}
