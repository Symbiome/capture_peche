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
import { GeoPoint, WaterEntityAttribution } from "@/pojos/BackendPojos";

type Position = number[];

interface ProjectedHit {
  distanceM: number;
  closestPoint: GeoPoint;
}

const EARTH_RADIUS_M = 6371008.8;
const METERS_PER_DEG_LAT = (Math.PI / 180) * EARTH_RADIUS_M;

/** Emprise [minLng, minLat, maxLng, maxLat] calculée une fois par feature. */
const bboxCache = new WeakMap<object, number[]>();

/**
 * Géométrie hors-ligne des packs hydro (#54/#174) : équivalent local, approché,
 * de l'attribution serveur (PostGIS ST_Distance sur un rayon fixe). Les
 * distances sont calculées dans une projection équirectangulaire centrée sur
 * le point tapé — précise à quelques mètres sur les quelques kilomètres du
 * rayon de recherche, sans dépendance géospatiale supplémentaire.
 */
export default class OfflineHydroGeometry {
  /**
   * Entités hydro les plus proches d'un point, triées par distance croissante,
   * dans la limite de `radiusM` et de `limit` entités distinctes. Un point situé
   * à l'intérieur d'un plan d'eau est à distance 0.
   */
  static nearestEntities(
    features: any[],
    lat: number,
    lng: number,
    radiusM: number,
    limit: number
  ): WaterEntityAttribution[] {
    const bestById = new Map<string, WaterEntityAttribution>();
    const marginLat = radiusM / METERS_PER_DEG_LAT;
    const marginLng = marginLat / Math.max(Math.cos((lat * Math.PI) / 180), 0.01);
    features.forEach((feature) => {
      const props = feature && feature.properties;
      if (!props || !props.water_entity_id || !feature.geometry) {
        return;
      }
      const bbox = OfflineHydroGeometry.bboxOf(feature);
      if (lng < bbox[0] - marginLng || lng > bbox[2] + marginLng
        || lat < bbox[1] - marginLat || lat > bbox[3] + marginLat) {
        return;
      }
      const hit = OfflineHydroGeometry.distanceToGeometry(feature.geometry, lat, lng);
      if (!hit || hit.distanceM > radiusM) {
        return;
      }
      const known = bestById.get(props.water_entity_id);
      if (!known || hit.distanceM < known.distanceM) {
        bestById.set(props.water_entity_id, {
          waterEntityId: props.water_entity_id,
          name: props.name,
          kind: props.kind,
          distanceM: Math.round(hit.distanceM),
          closestPoint: hit.closestPoint,
        });
      }
    });
    return Array.from(bestById.values())
      .sort((a, b) => a.distanceM - b.distanceM)
      .slice(0, limit);
  }

  /**
   * Point représentatif d'une géométrie (centrage carte d'un résultat de
   * recherche) : le sommet médian, toujours situé SUR l'entité — contrairement
   * au centre d'emprise d'un cours d'eau sinueux.
   */
  static representativePoint(geometry: any): GeoPoint | undefined {
    const positions = OfflineHydroGeometry.flattenPositions(geometry);
    if (!positions.length) {
      return undefined;
    }
    const middle = positions[Math.floor(positions.length / 2)];
    return { lng: middle[0], lat: middle[1] };
  }

  /** Distance (m) et point le plus proche entre un point et une géométrie GeoJSON. */
  static distanceToGeometry(geometry: any, lat: number, lng: number): ProjectedHit | null {
    const polygons = OfflineHydroGeometry.polygonsOf(geometry);
    const inside = polygons.some((rings) => OfflineHydroGeometry.isInsidePolygon(rings, lng, lat));
    if (inside) {
      return { distanceM: 0, closestPoint: { lat, lng } };
    }
    const lines = OfflineHydroGeometry.linesOf(geometry);
    let best: ProjectedHit | null = null;
    lines.forEach((line) => {
      const hit = OfflineHydroGeometry.distanceToLine(line, lat, lng);
      if (hit && (!best || hit.distanceM < best.distanceM)) {
        best = hit;
      }
    });
    return best;
  }

  private static bboxOf(feature: any): number[] {
    let bbox = bboxCache.get(feature);
    if (!bbox) {
      bbox = [Infinity, Infinity, -Infinity, -Infinity];
      OfflineHydroGeometry.flattenPositions(feature.geometry).forEach((p) => {
        bbox![0] = Math.min(bbox![0], p[0]);
        bbox![1] = Math.min(bbox![1], p[1]);
        bbox![2] = Math.max(bbox![2], p[0]);
        bbox![3] = Math.max(bbox![3], p[1]);
      });
      bboxCache.set(feature, bbox);
    }
    return bbox;
  }

  private static distanceToLine(line: Position[], lat: number, lng: number): ProjectedHit | null {
    const metersPerDegLat = METERS_PER_DEG_LAT;
    const metersPerDegLng = metersPerDegLat * Math.cos((lat * Math.PI) / 180);
    const toXY = (p: Position) => [(p[0] - lng) * metersPerDegLng, (p[1] - lat) * metersPerDegLat];
    let best: ProjectedHit | null = null;
    for (let i = 0; i < line.length; i++) {
      const a = toXY(line[i]);
      const b = i + 1 < line.length ? toXY(line[i + 1]) : a;
      const dx = b[0] - a[0];
      const dy = b[1] - a[1];
      const lengthSq = dx * dx + dy * dy;
      const t = lengthSq === 0 ? 0 : Math.max(0, Math.min(1, -(a[0] * dx + a[1] * dy) / lengthSq));
      const x = a[0] + t * dx;
      const y = a[1] + t * dy;
      const distanceM = Math.sqrt(x * x + y * y);
      if (!best || distanceM < best.distanceM) {
        best = {
          distanceM,
          closestPoint: { lng: lng + x / metersPerDegLng, lat: lat + y / metersPerDegLat },
        };
      }
    }
    return best;
  }

  /** Test pair-impair (ray casting) sur l'anneau extérieur et les trous. */
  private static isInsidePolygon(rings: Position[][], x: number, y: number): boolean {
    let inside = false;
    rings.forEach((ring) => {
      for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
        const xi = ring[i][0];
        const yi = ring[i][1];
        const xj = ring[j][0];
        const yj = ring[j][1];
        if ((yi > y) !== (yj > y) && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi) {
          inside = !inside;
        }
      }
    });
    return inside;
  }

  private static polygonsOf(geometry: any): Position[][][] {
    if (!geometry) {
      return [];
    }
    switch (geometry.type) {
      case "Polygon":
        return [geometry.coordinates];
      case "MultiPolygon":
        return geometry.coordinates;
      case "GeometryCollection":
        return (geometry.geometries || []).flatMap((g: any) => OfflineHydroGeometry.polygonsOf(g));
      default:
        return [];
    }
  }

  /** Lignes à mesurer : cours d'eau, contours de plans d'eau, points isolés. */
  private static linesOf(geometry: any): Position[][] {
    if (!geometry) {
      return [];
    }
    switch (geometry.type) {
      case "Point":
        return [[geometry.coordinates]];
      case "MultiPoint":
        return geometry.coordinates.map((p: Position) => [p]);
      case "LineString":
        return [geometry.coordinates];
      case "MultiLineString":
        return geometry.coordinates;
      case "Polygon":
        return geometry.coordinates;
      case "MultiPolygon":
        return geometry.coordinates.flat();
      case "GeometryCollection":
        return (geometry.geometries || []).flatMap((g: any) => OfflineHydroGeometry.linesOf(g));
      default:
        return [];
    }
  }

  private static flattenPositions(geometry: any): Position[] {
    return OfflineHydroGeometry.linesOf(geometry).flat();
  }
}
