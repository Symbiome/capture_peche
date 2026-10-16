/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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
import AnglerEffortHelpers from "@/services/AnglerEffortHelpers";

// #210 : mise en forme des statistiques d'effort de pêche du pêcheur.
describe("AnglerEffortHelpers", () => {
  it("aligne sessions et heures sur les mois, à zéro les mois sans session", () => {
    const series = AnglerEffortHelpers.monthlySeries(
      { MARCH: { tripsCount: 2, hours: 3.04 } },
      ["FEBRUARY", "MARCH"]
    );
    expect(series).toEqual({ trips: [0, 2], hours: [0, 3] });
  });

  it("ordonne les techniques par temps passé et ignore les temps nuls", () => {
    const names: { [id: string]: string } = { a: "Mouche", b: "Lancer", c: "Toc" };
    const slices = AnglerEffortHelpers.techniqueSlices({ a: 1.04, b: 2.5, c: 0.01 }, (id) => names[id]);
    expect(slices.map((s) => [s.label, s.hours])).toEqual([["Lancer", 2.5], ["Mouche", 1]]);
  });

  it("n'expose la CPUE qu'au-delà du seuil fixé par le backend", () => {
    const rows = AnglerEffortHelpers.cpueRows(
      [
        { speciesId: "perche", catchesCount: 12, catchesPerHour: 2.0049 },
        { speciesId: "brochet", catchesCount: 3 },
      ],
      (id) => id
    );
    expect(rows[0].catchesPerHour).toBe(2);
    expect(rows[1].catchesPerHour).toBeUndefined();
  });

  it("calcule une part en pourcentage, nulle sans total", () => {
    expect(AnglerEffortHelpers.sharePercent(3, 7)).toBe(43);
    expect(AnglerEffortHelpers.sharePercent(0, 0)).toBe(0);
  });
});
