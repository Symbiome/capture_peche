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
import { vi } from "vitest";
import TileCacheService from "@/services/TileCacheService";
import NetworkStatusService from "@/services/NetworkStatusService";

// MapLibre ne se charge pas sous jsdom (worker via createObjectURL) ; seul
// l'enregistrement du protocole est utilisé par le service.
vi.mock("maplibre-gl", () => ({ default: { addProtocol: vi.fn() } }));

/**
 * Cache léger des tuiles de fond de carte (#53) : réseau d'abord avec mise en
 * cache, repli sur le cache hors-ligne, purge des tuiles les plus anciennes.
 * La table Dexie est remplacée par un bouchon en mémoire (pas d'IndexedDB dans
 * jsdom).
 */
class FakeTileTable {
  rows = new Map<string, any>();

  get(url: string) {
    return Promise.resolve(this.rows.get(url));
  }
  put(row: any) {
    this.rows.set(row.url, row);
    return Promise.resolve(row.url);
  }
  update(url: string, changes: any) {
    Object.assign(this.rows.get(url), changes);
    return Promise.resolve(1);
  }
  count() {
    return Promise.resolve(this.rows.size);
  }
  orderBy(_index: string) {
    const sorted = [...this.rows.values()].sort(
      (a, b) => a.accessedAt - b.accessedAt
    );
    return {
      limit: (n: number) => ({
        primaryKeys: () => Promise.resolve(sorted.slice(0, n).map((r) => r.url)),
      }),
    };
  }
  bulkDelete(urls: string[]) {
    urls.forEach((url) => this.rows.delete(url));
    return Promise.resolve();
  }
}

const TILE_URL = "https://data.geopf.fr/wmts?LAYER=PLAN&TILEMATRIX=12";
const CACHED_URL = "fishola-tile://data.geopf.fr/wmts?LAYER=PLAN&TILEMATRIX=12";

function tileBytes(value: number): ArrayBuffer {
  return new Uint8Array([value]).buffer;
}

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

describe("TileCacheService", () => {
  let table: FakeTileTable;

  beforeEach(() => {
    table = new FakeTileTable();
    vi.spyOn(TileCacheService, "getDatabase").mockReturnValue({
      tileCache: table,
    } as any);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("réécrit l'URL vers le protocole de cache et la retrouve", () => {
    expect(TileCacheService.originalUrl(CACHED_URL)).toBe(TILE_URL);
  });

  it("sert la tuile du réseau et la met en cache", async () => {
    vi.spyOn(NetworkStatusService, "isOnline").mockReturnValue(true);
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        arrayBuffer: () => Promise.resolve(tileBytes(7)),
      })
    );

    const response = await TileCacheService.loadTile(
      { url: CACHED_URL } as any,
      new AbortController()
    );
    await flushPromises();

    expect(new Uint8Array(response.data)[0]).toBe(7);
    expect(table.rows.has(TILE_URL)).toBe(true);
  });

  it("ressert la tuile en cache hors-ligne sans appeler le réseau", async () => {
    vi.spyOn(NetworkStatusService, "isOnline").mockReturnValue(false);
    const fetchSpy = vi.fn();
    vi.stubGlobal("fetch", fetchSpy);
    await table.put({ url: TILE_URL, data: tileBytes(3), accessedAt: 1 });

    const response = await TileCacheService.loadTile(
      { url: CACHED_URL } as any,
      new AbortController()
    );

    expect(new Uint8Array(response.data)[0]).toBe(3);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("retombe sur le cache quand le réseau échoue", async () => {
    vi.spyOn(NetworkStatusService, "isOnline").mockReturnValue(true);
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("down")));
    await table.put({ url: TILE_URL, data: tileBytes(5), accessedAt: 1 });

    const response = await TileCacheService.loadTile(
      { url: CACHED_URL } as any,
      new AbortController()
    );

    expect(new Uint8Array(response.data)[0]).toBe(5);
  });

  it("échoue pour une tuile jamais consultée hors-ligne", async () => {
    vi.spyOn(NetworkStatusService, "isOnline").mockReturnValue(false);

    await expect(
      TileCacheService.loadTile({ url: CACHED_URL } as any, new AbortController())
    ).rejects.toThrow("Tuile indisponible hors-ligne");
  });

  it("purge les tuiles les moins récemment affichées au-delà du plafond", async () => {
    const total = TileCacheService.MAX_TILES + 2;
    for (let i = 0; i < total; i++) {
      await table.put({ url: `tile-${i}`, data: tileBytes(i), accessedAt: i });
    }

    await TileCacheService.prune();

    expect(table.rows.size).toBe(TileCacheService.MAX_TILES);
    expect(table.rows.has("tile-0")).toBe(false);
    expect(table.rows.has("tile-1")).toBe(false);
    expect(table.rows.has(`tile-${total - 1}`)).toBe(true);
  });
});
