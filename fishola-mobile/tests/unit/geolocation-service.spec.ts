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
import GeolocationService from "@/services/GeolocationService";

/**
 * « Plans d'eau autour de moi » (#198) : une erreur remontée par le watcher
 * (qui s'arrête alors) doit être rejetée immédiatement, et un refus de
 * permission doit être reconnu pour afficher un message dédié.
 */
describe("GeolocationService", () => {
  afterEach(() => {
    delete GeolocationService.latestError;
    delete GeolocationService.latestPosition;
  });

  it("reconnaît un refus de permission web (code 1) et natif", () => {
    expect(GeolocationService.isPermissionDenied({ code: 1 })).toBe(true);
    expect(
      GeolocationService.isPermissionDenied({
        message: "Location permission was denied",
      })
    ).toBe(true);
    expect(GeolocationService.isPermissionDenied({ code: 3 })).toBe(false);
    expect(GeolocationService.isPermissionDenied(undefined)).toBe(false);
  });

  it("rejette sans attendre sur une erreur TIMEOUT du watcher", async () => {
    const timeoutError = { code: 3, message: "Timeout expired" };
    GeolocationService.latestError = timeoutError;
    const startedAt = Date.now();

    await expect(
      GeolocationService.getPositionWithRetryUntilTimeout(5000)
    ).rejects.toBe(timeoutError);
    expect(Date.now() - startedAt).toBeLessThan(1000);
  });

  it("réessaie tant que le watcher n'a encore rien produit", async () => {
    const position = {
      timestamp: Date.now(),
      coords: { latitude: 45.87, longitude: 6.15 },
    } as any;
    setTimeout(() => {
      GeolocationService.latestPosition = position;
    }, 300);

    await expect(
      GeolocationService.getPositionWithRetryUntilTimeout(2000)
    ).resolves.toBe(position);
  });
});
