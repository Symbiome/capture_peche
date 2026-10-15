/*-
 * #%L
 * Fishola :: Admin
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
import BackendService from "@/services/BackendService";

/** Élément de référentiel proposé dans une liste de saisie. */
export interface ReferentialItem {
  id: string;
  name: string;
}

/**
 * Référentiel d'une liste de saisie (#239) : éléments nationaux non archivés, et ceux que
 * le compte connecté a masqués dans « Mes référentiels ».
 */
export interface EntryReferential {
  all: ReferentialItem[];
  hiddenIds: Set<string>;
}

/** Valeur de l'option « Afficher tout le référentiel » des listes de saisie. */
export const SHOW_ALL = "__SHOW_ALL__";

export const SPECIES_URI = "/v1/referential/species";
export const TECHNIQUES_URI = "/v1/referential/techniques";

export const EMPTY_REFERENTIAL: EntryReferential = { all: [], hiddenIds: new Set() };

/** Référentiel national et sa version filtrée pour le compte connecté, chargés en parallèle. */
export async function loadEntryReferential(uri: string): Promise<EntryReferential> {
  const [all, visible] = await Promise.all([
    BackendService.backendGet(uri),
    BackendService.backendGet(uri + "?forCurrentAdmin=true")
  ]);
  const visibleIds = new Set(visible.map((item: ReferentialItem) => item.id));
  const active: ReferentialItem[] = all.filter((item: any) => !item.archived);
  const hidden = active.filter((item) => !visibleIds.has(item.id));
  return { all: active, hiddenIds: new Set(hidden.map((item) => item.id)) };
}

/**
 * Éléments à proposer : les affichés, plus la valeur courante même masquée, ou tout le
 * référentiel si l'utilisateur l'a demandé.
 */
export function proposedItems(referential: EntryReferential, currentId: string | null,
  showAll: boolean): ReferentialItem[] {
  if (showAll) {
    return referential.all;
  }
  return referential.all.filter((item) => !referential.hiddenIds.has(item.id) || item.id === currentId);
}

/** Nombre d'éléments masqués qui ne sont pas proposés. */
export function hiddenCount(referential: EntryReferential, currentId: string | null): number {
  return referential.all.filter((item) => referential.hiddenIds.has(item.id) && item.id !== currentId).length;
}
