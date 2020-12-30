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
import {Dashboard, SpeciesWithAlias, GlobalDashboard} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';
import ReferentialService from './ReferentialService';
import Helpers from '@/services/Helpers';

export class DashboardAndSpecies {
    constructor (
        public dashboard:Dashboard,
        public species:SpeciesWithAlias[]) {
    }
}

export class GlobalDashboardAndSpecies {
    constructor (
        public dashboard:GlobalDashboard,
        public species:SpeciesWithAlias[]) {
    }
}

export default class DashboardService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static parseDate(input:any):Date {
        const result = Helpers.parseLocalDate(input);
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
                const result:DashboardAndSpecies = new DashboardAndSpecies(data[0], data[1]);
                resolve(result);
            },
            reject);
        });
    }

    static loadDashboardOrTimeout():Promise<DashboardAndSpecies> {
        const promise = this.loadDashboard();
        return this.timeout(5000, promise);
    }

    static loadGlobalDashboard():Promise<GlobalDashboardAndSpecies> {
        return new Promise<GlobalDashboardAndSpecies>((resolve, reject) => {
            Promise
            .all(
                [
                    this.backendGet('/v1/global-dashboard'),
                    ReferentialService.getAllSpeciesNoCache()
                ])
            .then((data:[GlobalDashboard, SpeciesWithAlias[]]) => {
                const result:GlobalDashboardAndSpecies = new GlobalDashboardAndSpecies(data[0], data[1]);
                resolve(result);
            },
            reject);
        });
    }

    static loadGlobalDashboardOrTimeout():Promise<GlobalDashboardAndSpecies> {
        const promise = this.loadGlobalDashboard();
        return this.timeout(5000, promise);
    }

}
