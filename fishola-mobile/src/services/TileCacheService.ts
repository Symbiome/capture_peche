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
import maplibregl, { GetResourceResponse, RequestParameters } from "maplibre-gl";
import AbstractFisholaService from "@/services/AbstractFisholaService";
import NetworkStatusService from "@/services/NetworkStatusService";

/**
 * Cache léger des tuiles de fond de carte (#53, option a du plan technique).
 *
 * Les URL des fonds IGN sont réécrites vers un protocole MapLibre dédié
 * (`fishola-tile://`) dont le gestionnaire charge la tuile sur le réseau,
 * l'enregistre dans Dexie, puis la ressert depuis Dexie quand le réseau manque.
 * Le cache est borné ({@link MAX_TILES}) et purgé par ancienneté d'accès : il
 * garde la dernière zone consultée, sans le poids d'un pack de tuiles régional.
 * Passer par un protocole MapLibre plutôt qu'un service worker fonctionne à
 * l'identique dans le navigateur et dans la WebView Capacitor.
 */
export default class TileCacheService extends AbstractFisholaService {
  static readonly PROTOCOL = "fishola-tile";
  /** ~800 tuiles 256 px ≈ 15 à 25 Mo : quelques niveaux de zoom d'une zone. */
  static readonly MAX_TILES = 800;
  /** La purge (comptage + suppression) n'est lancée que toutes les N écritures. */
  private static readonly PRUNE_EVERY_WRITES = 50;

  private static registered = false;
  private static writesSincePrune = 0;

  /**
   * Réécrit une URL https de tuile vers le protocole de cache, en enregistrant
   * le protocole auprès de MapLibre au premier appel.
   */
  static cachedUrl(httpsUrl: string): string {
    TileCacheService.register();
    return httpsUrl.replace(/^https:\/\//, `${TileCacheService.PROTOCOL}://`);
  }

  /** Retrouve l'URL https d'origine d'une URL réécrite par {@link cachedUrl}. */
  static originalUrl(cachedUrl: string): string {
    return cachedUrl.replace(`${TileCacheService.PROTOCOL}://`, "https://");
  }

  private static register() {
    if (TileCacheService.registered) {
      return;
    }
    maplibregl.addProtocol(TileCacheService.PROTOCOL, (params, abortController) =>
      TileCacheService.loadTile(params, abortController)
    );
    TileCacheService.registered = true;
  }

  /**
   * Gestionnaire du protocole : réseau d'abord (et mise en cache), cache en
   * repli quand le réseau est absent ou la requête en échec. Une tuile jamais
   * consultée reste indisponible hors-ligne : MapLibre laisse alors la zone vide.
   */
  static async loadTile(
    params: RequestParameters,
    abortController: AbortController
  ): Promise<GetResourceResponse<ArrayBuffer>> {
    const url = TileCacheService.originalUrl(params.url);
    if (NetworkStatusService.isOnline()) {
      const fromNetwork = await TileCacheService.fetchTile(url, abortController);
      if (fromNetwork) {
        TileCacheService.store(url, fromNetwork);
        return { data: fromNetwork };
      }
    }
    const fromCache = await TileCacheService.read(url);
    if (fromCache) {
      return { data: fromCache };
    }
    throw new Error(`Tuile indisponible hors-ligne : ${url}`);
  }

  /** Télécharge la tuile ; `null` si le serveur ou le réseau fait défaut. */
  private static async fetchTile(
    url: string,
    abortController: AbortController
  ): Promise<ArrayBuffer | null> {
    try {
      const response = await fetch(url, { signal: abortController.signal });
      return response.ok ? await response.arrayBuffer() : null;
    } catch (error) {
      if (abortController.signal.aborted) {
        throw error;
      }
      return null;
    }
  }

  /** Lit une tuile en cache et rafraîchit sa date d'accès (purge LRU). */
  static async read(url: string): Promise<ArrayBuffer | null> {
    try {
      const tile = await this.getDatabase().tileCache.get(url);
      if (!tile) {
        return null;
      }
      this.getDatabase()
        .tileCache.update(url, { accessedAt: Date.now() })
        .catch(() => undefined);
      return tile.data;
    } catch (error) {
      console.warn("Lecture du cache de tuiles impossible", error);
      return null;
    }
  }

  /**
   * Enregistre une tuile sans bloquer l'affichage : une erreur IndexedDB
   * (quota, navigation privée) ne doit jamais empêcher la carte de s'afficher.
   */
  static store(url: string, data: ArrayBuffer) {
    this.getDatabase()
      .tileCache.put({ url, data, accessedAt: Date.now() })
      .then(() => TileCacheService.pruneIfDue())
      .catch((error) =>
        console.warn("Mise en cache de la tuile impossible", error)
      );
  }

  private static pruneIfDue(): Promise<void> {
    TileCacheService.writesSincePrune++;
    if (TileCacheService.writesSincePrune < TileCacheService.PRUNE_EVERY_WRITES) {
      return Promise.resolve();
    }
    TileCacheService.writesSincePrune = 0;
    return TileCacheService.prune();
  }

  /** Supprime les tuiles les moins récemment affichées au-delà du plafond. */
  static async prune(): Promise<void> {
    const table = this.getDatabase().tileCache;
    const excess = (await table.count()) - TileCacheService.MAX_TILES;
    if (excess <= 0) {
      return;
    }
    const oldestUrls = await table
      .orderBy("accessedAt")
      .limit(excess)
      .primaryKeys();
    await table.bulkDelete(oldestUrls);
  }
}
