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
import FisholaOpenCVService from "@/services/opencv/FisholaOpenCVService";

// Le service est un singleton : on repart d'un état non chargé à chaque test.
const service = FisholaOpenCVService.INSTANCE;

beforeEach(() => {
  service.cv = undefined;
  document.head.innerHTML = "";
  // @ts-ignore — window.cv est posé par le bundle opencv.js au chargement.
  delete window.cv;
});

// Ces tests couvrent le chargement paresseux d'opencv.js, pas la détection de
// marqueur elle-même : celle-ci nécessite un vrai navigateur et des images, et
// reste couverte par le banc Cypress (tests/cypress/bench/fish-detection).
describe("FisholaOpenCVService — chargement paresseux", () => {
  it("n'est pas prêt tant qu'opencv.js n'est pas chargé", () => {
    expect(service.isOpenCVReady()).toBeFalsy();
  });

  it("injecte le script opencv.js et attend son événement de chargement", async () => {
    const loading = service.loadOpenCVIfNeeded();

    const script = document.head.querySelector("script");
    expect(script).not.toBeNull();
    expect(script!.getAttribute("src")).toBe("/js/opencv.js");
    expect(script!.hasAttribute("async")).toBe(true);

    // Le bundle expose window.cv puis déclenche l'événement via son onLoad.
    const fakeCv = { imshow: () => undefined };
    // @ts-ignore
    window.cv = fakeCv;
    document.dispatchEvent(new Event("open-cv-loaded"));

    await loading;
    expect(service.cv).toBe(fakeCv);
    expect(service.isOpenCVReady()).toBeTruthy();
  });

  it("ne recharge pas le script quand OpenCV est déjà là", async () => {
    service.cv = { imshow: () => undefined };

    await service.loadOpenCVIfNeeded();

    expect(document.head.querySelector("script")).toBeNull();
  });
});
