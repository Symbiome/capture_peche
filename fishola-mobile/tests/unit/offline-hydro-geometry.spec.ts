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
import OfflineHydroGeometry from "@/services/OfflineHydroGeometry";

const river = {
  type: "Feature",
  geometry: { type: "MultiLineString", coordinates: [[[5.0, 45.0], [5.0, 45.1]]] },
  properties: { water_entity_id: "river", name: "La Rivière", kind: "FLOWING" },
};
const lake = {
  type: "Feature",
  geometry: {
    type: "MultiPolygon",
    coordinates: [[[[5.1, 45.0], [5.12, 45.0], [5.12, 45.02], [5.1, 45.02], [5.1, 45.0]]]],
  },
  properties: { water_entity_id: "lake", name: "Le Lac", kind: "STILL" },
};

// Attribution locale hors-ligne (#174) : un tap à côté d'un cours d'eau doit
// retrouver l'entité par proximité, comme l'attribution serveur.
describe("OfflineHydroGeometry — attribution par proximité", () => {
  it("propose le cours d'eau le plus proche d'un tap à côté du trait", () => {
    const result = OfflineHydroGeometry.nearestEntities([river, lake], 45.05, 5.001, 5000, 5);
    expect(result[0].waterEntityId).toBe("river");
    expect(result[0].distanceM).toBeGreaterThan(70);
    expect(result[0].distanceM).toBeLessThan(90);
    expect(result[0].closestPoint.lng).toBeCloseTo(5.0, 6);
    expect(result[0].closestPoint.lat).toBeCloseTo(45.05, 6);
  });

  it("place un tap à l'intérieur d'un plan d'eau à distance 0", () => {
    const result = OfflineHydroGeometry.nearestEntities([river, lake], 45.01, 5.11, 5000, 5);
    expect(result[0].waterEntityId).toBe("lake");
    expect(result[0].distanceM).toBe(0);
  });

  it("ignore les entités hors du rayon", () => {
    const result = OfflineHydroGeometry.nearestEntities([river, lake], 46.0, 6.0, 5000, 5);
    expect(result).toEqual([]);
  });

  it("dédoublonne une entité présente dans deux packs", () => {
    const result = OfflineHydroGeometry.nearestEntities([river, river], 45.05, 5.001, 5000, 5);
    expect(result.length).toBe(1);
  });

  it("donne un point représentatif situé sur l'entité", () => {
    const point = OfflineHydroGeometry.representativePoint(river.geometry);
    expect(point).toEqual({ lng: 5.0, lat: 45.1 });
  });
});
