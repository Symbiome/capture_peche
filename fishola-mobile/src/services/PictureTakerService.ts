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

import { Camera, CameraSource, CameraResultType } from "@capacitor/core";

export default class PictureTakerService {
  public static INSTANCE = new PictureTakerService();

  /**
   * Takes a picture weither from the Camera or Gallery according to the given boolean (always fileInput if on desktop).
   * @returns the taken picture src
   */
  async takePicture(fromCameraIfPossible: boolean): Promise<string> {
    let source = CameraSource.Photos;
    if (fromCameraIfPossible) {
      source = CameraSource.Camera;
    }
    // In web mode, we ignore CameraSource and always use a file input (webUseInput set to true)
    try {
      const image = await Camera.getPhoto({
        quality: 95,
        allowEditing: false,
        resultType: CameraResultType.DataUrl,
        source: source,
        webUseInput: true,
      });
      const imageSrc = image.dataUrl;
      if (imageSrc) {
        return imageSrc;
      }
    } catch (failure) {
      console.error("Unable to use camera", failure);
    }
    throw new Error("Unable to use camera");
  }
}
