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
import AbstractFisholaService from "@/services/AbstractFisholaService";

/**
 * Invariant vérifié ici : **toute** requête doit régler sa promesse, y compris
 * quand elle échoue au niveau transport.
 *
 * Une requête XHR qui n'aboutit pas (réseau absent, hôte injoignable, requête
 * interrompue) ne déclenche pas `onload` mais `onerror` / `onabort` /
 * `ontimeout`. Sans ces gestionnaires, la promesse restait éternellement en
 * attente. Le push d'une sortie hors réseau laissait alors le verrou
 * `syncInProgress` de `TripsService.syncTrips` armé pour toute la durée de vie
 * de l'application : plus aucune sortie ne repartait, même le réseau revenu,
 * jusqu'au redémarrage.
 *
 * Un test qui se contente de vérifier « la synchro repart » via un bouchon
 * réseau ne suffit pas : la fidélité du bouchon décide alors du résultat. On
 * pilote donc directement l'évènement XHR.
 */

type XhrEvent = "onerror" | "onabort" | "ontimeout";

class FakeXhr {
  static last: FakeXhr;

  status = 0;
  responseText = "";
  withCredentials = false;
  timeout = 0;
  onload: (() => void) | null = null;
  onerror: (() => void) | null = null;
  onabort: (() => void) | null = null;
  ontimeout: (() => void) | null = null;

  constructor() {
    FakeXhr.last = this;
  }

  open() {
    /* rien : on ne veut pas de vraie requête */
  }
  setRequestHeader() {
    /* idem */
  }
  send() {
    /* la requête reste en vol : c'est le test qui décide de son sort */
  }

  fire(event: XhrEvent) {
    const handler = this[event];
    if (!handler) {
      throw new Error(`Aucun gestionnaire ${event} branché sur la requête`);
    }
    handler.call(this);
  }
}

describe("AbstractFisholaService — échec de transport", () => {
  let originalXhr: any;

  beforeEach(() => {
    originalXhr = (globalThis as any).XMLHttpRequest;
    (globalThis as any).XMLHttpRequest = FakeXhr as any;
  });

  afterEach(() => {
    (globalThis as any).XMLHttpRequest = originalXhr;
  });

  // Les méthodes qui écrivent (`POST`/`PUT`/`DELETE`) sont celles qu'emprunte
  // la synchronisation ; `backendGetWithArgs` alimente la liste des sorties.
  const cases: Array<[string, () => Promise<unknown>]> = [
    ["backendPost", () => AbstractFisholaService.backendPost("/v1/trips", {})],
    ["backendPut", () => AbstractFisholaService.backendPut("/v1/trips/1", {})],
    ["backendDelete", () => AbstractFisholaService.backendDelete("/v1/trips/1")],
    [
      "backendPutPlain",
      () => AbstractFisholaService.backendPutPlain("/v1/pictures/1", "data"),
    ],
    [
      "backendGetWithArgs",
      () => AbstractFisholaService.backendGetWithArgs("/v1/trips", {}),
    ],
    ["backendGet", () => AbstractFisholaService.backendGet("/v1/trips")],
  ];

  const events: XhrEvent[] = ["onerror", "onabort", "ontimeout"];

  cases.forEach(([name, call]) => {
    events.forEach((event) => {
      it(`${name} rompt sa promesse sur ${event}`, async () => {
        const promise = call();
        FakeXhr.last.fire(event);

        await expect(promise).rejects.toBeTruthy();
      });
    });
  });

  it("le rejet reste distinguable d'une réponse HTTP en erreur", async () => {
    // `backendGetOrOfflineStorage` se sert de cette distinction : un statut
    // numérique (401, 5xx) est propagé — le serveur a répondu, le cache local
    // ne doit pas masquer une session expirée — alors qu'un échec de transport
    // déclenche le repli hors ligne.
    const promise = AbstractFisholaService.backendPost("/v1/trips", {});
    FakeXhr.last.fire("onerror");

    await promise.then(
      () => {
        throw new Error("La promesse aurait dû être rompue");
      },
      (error) => {
        expect(typeof error).not.toBe("number");
        expect(error.networkError).toBe(true);
      }
    );
  });
});
