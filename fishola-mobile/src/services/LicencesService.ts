import {
  LicenceFromClientBean,
  LicenceResponseBean,
} from "@/pojos/BackendPojos";
import AbstractFisholaService from "@/services/AbstractFisholaService";

export default class DocumentationService extends AbstractFisholaService {
  constructor() {
    super();
  }

  static getAllLicences(userId: string): Promise<LicenceResponseBean[]> {
    return this.backendGet("/v1/licences/" + userId);
  }

  static getLicence(userId: string, licenceId: string): Promise<void> {
    return this.backendGet("/v1/licences/" + userId + "/" + licenceId);
  }

  static postLicence(
    userId: string,
    licence: LicenceFromClientBean
  ): Promise<void> {
    return this.backendPost("/v1/licences/" + userId, licence);
  }

  static deleteLicence(userId: string, licenceId: string): Promise<void> {
    return this.backendDelete("/v1/licences/" + userId + "/" + licenceId);
  }
}
