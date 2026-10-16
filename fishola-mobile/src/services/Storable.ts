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
import { isProxy, toRaw } from "vue";

/**
 * Copie d'une valeur sans les Proxy réactifs de Vue 3, avant écriture dans
 * IndexedDB (#284) : le clonage structuré d'IndexedDB refuse les Proxy
 * (DataCloneError), que Vue 2 n'utilisait pas.
 *
 * Seuls les tableaux et les objets simples sont parcourus ; Date, Blob, File,
 * ArrayBuffer, etc. sont repris tels quels.
 *
 * @param value valeur, éventuellement réactive
 * @returns une copie de la valeur, sans aucun Proxy
 */
export function toStorable<T>(value: T): T {
  const raw = isProxy(value) ? toRaw(value) : value;
  if (Array.isArray(raw)) {
    return raw.map(toStorable) as T;
  }
  if (Object.prototype.toString.call(raw) === "[object Object]") {
    return Object.fromEntries(
      Object.entries(raw as object).map(([key, child]) => [key, toStorable(child)])
    ) as T;
  }
  return raw;
}
