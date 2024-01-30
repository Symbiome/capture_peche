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

  static async getLicence(licenceId: string): Promise<Blob> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      const apiUrl = Constants.apiUrl(`/v1/licences/${licenceId}`);
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

  static postLicence(licence: LicenceFromClientBean): Promise<void> {
    return this.backendPost("/v1/licences/", licence);
  }

  static deleteLicence(licenceId: string): Promise<void> {
    return this.backendDelete("/v1/licences/" + licenceId);
  }
}
