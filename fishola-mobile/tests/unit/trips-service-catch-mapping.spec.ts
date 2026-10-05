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
import moment from "moment";
import TripsService from "@/services/TripsService";

/**
 * Une prise saisie par classe de taille (#196) doit se rouvrir telle quelle
 * depuis le serveur : nombre de poissons et bornes de la classe conservés,
 * aucune taille exacte inventée à partir de la borne basse.
 */
describe("TripsService.backendCatchToCatchBean", () => {
  it("conserve le lot et sa classe de taille", () => {
    const backendCatch = {
      id: "c1",
      speciesId: "s1",
      size: null,
      quantity: 10,
      lotMinSize: 40,
      lotMaxSize: 50,
    };

    const bean: any = TripsService.backendCatchToCatchBean(moment(), backendCatch);

    expect(bean.quantity).toBe(10);
    expect(bean.lotMinSize).toBe(40);
    expect(bean.lotMaxSize).toBe(50);
    expect(bean.size).toBeNull();
  });
});
