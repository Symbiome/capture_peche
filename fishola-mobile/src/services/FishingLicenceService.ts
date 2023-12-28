import {
  LicenceFromClientBean,
  LicenceResponseBean,
} from "@/pojos/BackendPojos";
import AbstractFisholaService from "@/services/AbstractFisholaService";

export default class FishingLicenceService extends AbstractFisholaService {
  constructor() {
    super();
  }

  static getAllLicences(): Promise<LicenceResponseBean[]> {
    return this.backendGet("/v1/licences/");
  }

  static getLicence(licenceId: string): Promise<void> {
    return this.backendGet("/v1/licences/" + licenceId);
  }

  static postLicence(
    userId: string,
    licence: LicenceFromClientBean
  ): Promise<void> {
    return this.backendPost("/v1/licences/", licence);
  }

  static deleteLicence(licenceId: string): Promise<void> {
    return this.backendDelete("/v1/licences/" + licenceId);
  }
}
