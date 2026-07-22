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
import OfflineArea from "@/pojos/OfflineArea";

/** Un département proposé au téléchargement (miroir du DTO backend #54). */
export interface DepartmentSummary {
  code: string;
  name: string;
  communeCount: number;
}

/**
 * Gestion des packs hydrographiques départementaux hors-ligne (#54, « télécharger
 * un département façon Komoot »). Symbiome a cadré : on télécharge **uniquement
 * les entités hydro** du département (pas de fond de carte / tuiles). Chaque pack
 * est un FeatureCollection GeoJSON stocké dans Dexie (`offlineAreas`), rendu par
 * la carte comme source locale quand les tuiles vectorielles sont injoignables.
 */
export default class OfflineAreasService extends AbstractFisholaService {
  /** Départements disponibles au téléchargement (code, nom, nb communes). */
  static listDepartments(): Promise<DepartmentSummary[]> {
    return this.backendGet("/v1/departments").then(
      (results: DepartmentSummary[]) => results || []
    );
  }

  /**
   * Télécharge le pack hydro d'un département et le persiste hors-ligne.
   * Écrase un pack déjà présent (= mise à jour). Renvoie l'entrée stockée.
   */
  static async downloadPack(
    code: string,
    name: string
  ): Promise<OfflineArea> {
    const geojson = await this.backendGet(
      `/v1/departments/${encodeURIComponent(code)}/hydro`
    );
    const features = (geojson && geojson.features) || [];
    const serialized = JSON.stringify(geojson);
    const area: OfflineArea = {
      code,
      name,
      geojson,
      entityCount: features.length,
      bytes: OfflineAreasService.byteLength(serialized),
      downloadedAt: new Date().getTime(),
    };
    await this.getDatabase().offlineAreas.put(area);
    return area;
  }

  /** Packs installés, du plus récent au plus ancien. */
  static listPacks(): Promise<OfflineArea[]> {
    return this.getDatabase()
      .offlineAreas.orderBy("downloadedAt")
      .reverse()
      .toArray();
  }

  /** Un pack par code département (undefined si absent). */
  static getPack(code: string): Promise<OfflineArea | undefined> {
    return this.getDatabase().offlineAreas.get(code);
  }

  /** Codes des départements téléchargés (pour cocher la liste). */
  static async downloadedCodes(): Promise<Set<string>> {
    const codes = await this.getDatabase().offlineAreas.orderBy("code").keys();
    return new Set(codes as string[]);
  }

  /** Supprime un pack téléchargé. */
  static deletePack(code: string): Promise<void> {
    return this.getDatabase().offlineAreas.delete(code);
  }

  /**
   * Toutes les entités hydro hors-ligne, fusionnées en un seul FeatureCollection
   * (source unique pour la carte quand les tuiles ne répondent pas). Vide si
   * aucun pack n'est installé.
   */
  static async mergedFeatureCollection(): Promise<any> {
    const packs = await OfflineAreasService.listPacks();
    const features: any[] = [];
    packs.forEach((p) => {
      const list = (p.geojson && p.geojson.features) || [];
      list.forEach((f: any) => features.push(f));
    });
    return { type: "FeatureCollection", features };
  }

  /** Nombre d'octets UTF-8 d'une chaîne (affichage de la taille du pack). */
  private static byteLength(s: string): number {
    if (typeof TextEncoder !== "undefined") {
      return new TextEncoder().encode(s).length;
    }
    return s.length;
  }
}
