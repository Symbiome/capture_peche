/*-
 * #%L
 * Carnet de pêche :: Mobile
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
 * Prélèvement d'écailles hérité de l'application d'origine (programme INRAE des lacs alpins),
 * masqué pour le projet RM&C (#240, #241) mais conservé pour une éventuelle
 * réactivation via `VITE__SCALE_SAMPLING_ENABLED=true`.
 */
export default class ScaleSampling {

  /** Le prélèvement d'écailles est-il proposé au pêcheur ? */
  static isEnabled(): boolean {
    return import.meta.env.VITE__SCALE_SAMPLING_ENABLED === "true";
  }

  /**
   * Afficher le toggle « Prélèvement (optionnel) » à la saisie d'une capture :
   * jamais si la fonctionnalité est désactivée, même pour un pêcheur ayant
   * `promptSamples` en base ; une capture qui a déjà un numéro d'échantillon
   * l'affiche alors en lecture seule, sans toggle.
   */
  static showSampleToggle(
    enabled: boolean,
    withSample: boolean,
    promptSamples: boolean,
    speciesAuthorized: boolean
  ): boolean {
    return enabled && (withSample || (promptSamples && speciesAuthorized));
  }
}
