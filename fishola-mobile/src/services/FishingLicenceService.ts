/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2024 INRAE - UMR CARRTEL
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
import {
  LicenceFromClientBean,
  LicenceResponseBean,
} from "@/pojos/BackendPojos";
import AbstractFisholaService from "@/services/AbstractFisholaService";
import Constants from "./Constants";

export default class FishingLicenceService extends AbstractFisholaService {
  constructor() {
    super();
  }

  static getAllLicences(): Promise<LicenceResponseBean[]> {
    return this.backendGet("/v1/licences/");
  }

  static async getLicenceFile(licenceId: string): Promise<Blob> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      const apiUrl = Constants.apiUrl(`/v1/licences/${licenceId}/file`);
      xhr.open("GET", apiUrl, true);
      xhr.withCredentials = true;
      xhr.responseType = "blob";

      xhr.onload = function () {
        if (xhr.status == 200) {
          resolve(xhr.response);
        } else {
          reject(xhr.status);
        }
      };

      xhr.onerror = function () {
        reject("Impossible de contacter le serveur");
      };

      xhr.send();
    });
  }

  static async getLicence(licenceId: string): Promise<LicenceResponseBean> {
    return this.backendGet(`/v1/licences/${licenceId}`);
  }


  static postLicence(licence: LicenceFromClientBean): Promise<void> {
    return this.backendPost("/v1/licences/", licence);
  }

  static putLicence(licence: LicenceFromClientBean, id: String): Promise<void> {
    return this.backendPut("/v1/licences/" + id, licence);
  }

  static deleteLicence(licenceId: string): Promise<void> {
    return this.backendDelete("/v1/licences/" + licenceId);
  }
}
