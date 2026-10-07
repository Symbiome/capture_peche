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
import { Month, MonthlyEffort, SpeciesCpue } from "@/pojos/BackendPojos";

export interface TechniqueSlice {
  techniqueId: string;
  label: string;
  hours: number;
}

export interface CpueRow {
  speciesId: string;
  label: string;
  catchesCount: number;
  /** Prises par heure arrondies au centième ; absent sous le seuil de prises. */
  catchesPerHour?: number;
}

/**
 * Mise en forme des statistiques d'effort de pêche du pêcheur (#210) pour les
 * graphiques de « Mes données ».
 */
export default class AnglerEffortHelpers {
  /** Sessions et heures (au dixième) par mois, dans l'ordre des mois fournis. */
  static monthlySeries(
    monthlyEffort: { [P in Month]?: MonthlyEffort },
    months: Month[]
  ): { trips: number[]; hours: number[] } {
    return {
      trips: months.map((m) => (monthlyEffort[m] ? monthlyEffort[m]!.tripsCount : 0)),
      hours: months.map((m) => (monthlyEffort[m] ? AnglerEffortHelpers.round(monthlyEffort[m]!.hours, 1) : 0)),
    };
  }

  /** Parts du camembert, de la technique la plus pratiquée à la moins pratiquée. */
  static techniqueSlices(
    hoursPerTechnique: { [index: string]: number },
    techniqueName: (id: string) => string
  ): TechniqueSlice[] {
    return Object.keys(hoursPerTechnique)
      .map((id) => ({
        techniqueId: id,
        label: techniqueName(id),
        hours: AnglerEffortHelpers.round(hoursPerTechnique[id], 1),
      }))
      .filter((slice) => slice.hours > 0)
      .sort((a, b) => b.hours - a.hours);
  }

  static cpueRows(cpuePerSpecies: SpeciesCpue[], speciesName: (id: string) => string): CpueRow[] {
    return cpuePerSpecies.map((cpue) => ({
      speciesId: cpue.speciesId,
      label: speciesName(cpue.speciesId),
      catchesCount: cpue.catchesCount,
      catchesPerHour:
        cpue.catchesPerHour === undefined || cpue.catchesPerHour === null
          ? undefined
          : AnglerEffortHelpers.round(cpue.catchesPerHour, 2),
    }));
  }

  /** Part en pourcentage entier ; 0 si le total est nul. */
  static sharePercent(mine: number, total: number): number {
    return total > 0 ? Math.round((mine * 100) / total) : 0;
  }

  static round(value: number, decimals: number): number {
    const factor = Math.pow(10, decimals);
    return Math.round(value * factor) / factor;
  }
}
