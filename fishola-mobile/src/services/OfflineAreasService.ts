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
import { AttributionResponse, WaterEntity as Lake } from "@/pojos/BackendPojos";
import Helpers from "@/services/Helpers";
import OfflineHydroGeometry from "@/services/OfflineHydroGeometry";

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
  /** Même rayon que l'attribution serveur (HydroSearchDao.ATTRIBUTION_RADIUS_M). */
  static readonly ATTRIBUTION_RADIUS_M = 5000;
  private static readonly ATTRIBUTION_LIMIT = 5;
  private static readonly SEARCH_LIMIT = 50;

  /**
   * Entités des packs gardées en mémoire : relire plusieurs Mo depuis Dexie à
   * chaque frappe de recherche ou tap carte serait trop lent. Invalidé à chaque
   * téléchargement / suppression de pack.
   */
  private static featuresCache: Promise<any[]> | null = null;

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
    OfflineAreasService.featuresCache = null;
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
    OfflineAreasService.featuresCache = null;
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

  /**
   * Attribution hydro locale (#174) : entités des packs téléchargés les plus
   * proches du point, au format de la réponse serveur. `proposal` absent si
   * aucune entité locale n'est dans le rayon (pas de pack sur ce secteur).
   */
  static async attributeLocally(lat: number, lng: number): Promise<AttributionResponse> {
    const features = await OfflineAreasService.cachedFeatures();
    const nearest = OfflineHydroGeometry.nearestEntities(
      features,
      lat,
      lng,
      OfflineAreasService.ATTRIBUTION_RADIUS_M,
      OfflineAreasService.ATTRIBUTION_LIMIT
    );
    return { proposal: nearest[0], alternatives: nearest.slice(1) };
  }

  /**
   * Recherche par nom dans les packs téléchargés (repli hors-ligne de la
   * recherche serveur, #174), au format Lake des résultats serveur.
   */
  static async searchByName(q: string): Promise<Lake[]> {
    const features = await OfflineAreasService.cachedFeatures();
    const uniqueById = new Map<string, any>();
    features.forEach((f) => {
      const props = f && f.properties;
      if (props && props.water_entity_id && props.name && !uniqueById.has(props.water_entity_id)) {
        uniqueById.set(props.water_entity_id, f);
      }
    });
    return Helpers.rankBySearch(Array.from(uniqueById.values()), q, (f) => [f.properties.name])
      .slice(0, OfflineAreasService.SEARCH_LIMIT)
      .map((f) => OfflineAreasService.featureToLake(f));
  }

  private static cachedFeatures(): Promise<any[]> {
    if (!OfflineAreasService.featuresCache) {
      OfflineAreasService.featuresCache = OfflineAreasService.mergedFeatureCollection()
        .then((collection) => collection.features)
        .catch((e) => {
          OfflineAreasService.featuresCache = null;
          throw e;
        });
    }
    return OfflineAreasService.featuresCache;
  }

  private static featureToLake(feature: any): Lake {
    const point = OfflineHydroGeometry.representativePoint(feature.geometry);
    return {
      id: feature.properties.water_entity_id,
      name: feature.properties.name,
      kind: feature.properties.kind,
      latitude: point ? point.lat : undefined,
      longitude: point ? point.lng : undefined,
      exportAs: feature.properties.name,
      waterEntityCode: "",
      nature: "",
      altitudeMoyenne: 0,
      bdtopoCleabs: "",
      geom: "",
    } as unknown as Lake;
  }

  /** Nombre d'octets UTF-8 d'une chaîne (affichage de la taille du pack). */
  private static byteLength(s: string): number {
    if (typeof TextEncoder !== "undefined") {
      return new TextEncoder().encode(s).length;
    }
    return s.length;
  }
}
