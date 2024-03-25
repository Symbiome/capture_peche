/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2022 INRAE - UMR CARRTEL
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
import AbstractFisholaService from "@/services/AbstractFisholaService";
import { FileSharer } from "@byteowls/capacitor-filesharer";

export default class ShareService extends AbstractFisholaService {
  constructor() {
    super();
  }

  static async sharePicture(pictureURL: string, fileName: string) {
    const base64 = await ShareService.getBase64FromUrl(pictureURL);
    FileSharer.share({
      filename: fileName,
      contentType: "application/png",
      // If you want to save base64:
      base64Data: base64,
    })
      .then(() => {})
      .catch((error) => {
        console.error("Impossible de partager l'image ", error.message);
      });
  }

  static async getBase64FromUrl(url: string): Promise<string> {
    const getBase64StringFromDataURL = (dataURL: string) =>
      dataURL.replace("data:", "").replace(/^.+,/, "");
    const data = await fetch(url);
    const blob = await data.blob();
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.readAsDataURL(blob);
      reader.onloadend = () => {
        const base64data = getBase64StringFromDataURL(reader.result as string);
        resolve(base64data as string);
      };
    });
  }
}
