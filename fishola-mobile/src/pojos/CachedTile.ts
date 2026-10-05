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

/**
 * Une tuile de fond de carte IGN mise en cache au fil de la consultation (#53,
 * option a du plan technique : « cache léger de la dernière zone consultée »).
 * Le cache est borné en nombre de tuiles et purgé par ancienneté d'accès : il
 * conserve donc naturellement la dernière zone affichée, sans pack lourd.
 */
export default interface CachedTile {
  /** URL https d'origine de la tuile (clé Dexie). */
  url: string;
  /** Contenu brut de l'image (PNG Plan IGN ou JPEG BD Ortho). */
  data: ArrayBuffer;
  /** Horodatage du dernier affichage (epoch ms), pour la purge LRU. */
  accessedAt: number;
}
