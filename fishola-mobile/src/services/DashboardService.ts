/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
import Constants from "@/services/Constants";
import {
  Dashboard,
  SpeciesWithAlias,
  GlobalDashboard,
} from "@/pojos/BackendPojos";
import AbstractFisholaService from "@/services/AbstractFisholaService";
import ReferentialService from "./ReferentialService";
import Helpers from "@/services/Helpers";

export class DashboardAndSpecies {
  constructor(
    public dashboard: Dashboard,
    public species: SpeciesWithAlias[]
  ) {}
}

export class GlobalDashboardAndSpecies {
  constructor(
    public dashboard: GlobalDashboard,
    public species: SpeciesWithAlias[]
  ) {}
}

export default class DashboardService extends AbstractFisholaService {
  constructor() {
    super();
  }

  static getExportUrl(): string {
    return Constants.apiUrl("/v1/dashboard/export");
  }

  static parseDate(input: any): Date {
    const result = Helpers.parseLocalDate(input);
    return result;
  }

  static loadDashboard(year: number): Promise<DashboardAndSpecies> {
    const params = {
      year: year,
    };
    return new Promise<DashboardAndSpecies>((resolve, reject) => {
      Promise.all([
        this.backendGetWithArgs("/v1/dashboard", params),
        ReferentialService.getAllSpeciesNoCache(),
      ]).then((data: [Dashboard, SpeciesWithAlias[]]) => {
        if (data[0].latestTripsCatchs) {
          data[0].latestTripsCatchs.forEach((trip) => {
            trip.day = this.parseDate(trip.day);
          });
        }
        const result: DashboardAndSpecies = new DashboardAndSpecies(
          data[0],
          data[1]
        );
        resolve(result);
      }, reject);
    });
  }

  static loadDashboardOrTimeout(year: number): Promise<DashboardAndSpecies> {
    const promise = this.loadDashboard(year);
    return this.timeout(5000, promise);
  }

  static loadGlobalDashboard(year: number): Promise<GlobalDashboardAndSpecies> {
    const params = {
      year: year,
    };
    return new Promise<GlobalDashboardAndSpecies>((resolve, reject) => {
      Promise.all([
        this.backendGetWithArgs("/v1/global-dashboard", params),
        ReferentialService.getAllSpeciesNoCache(),
      ]).then((data: [GlobalDashboard, SpeciesWithAlias[]]) => {
        const result: GlobalDashboardAndSpecies = new GlobalDashboardAndSpecies(
          data[0],
          data[1]
        );
        resolve(result);
      }, reject);
    });
  }

  static asyncExport(): Promise<void> {
    return this.backendPost("/v1/dashboard/async-export");
  }

  static loadGlobalDashboardOrTimeout(
    year: number
  ): Promise<GlobalDashboardAndSpecies> {
    const promise = this.loadGlobalDashboard(year);
    return this.timeout(5000, promise);
  }
}
