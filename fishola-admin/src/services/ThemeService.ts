/*-
 * #%L
 * Fishola :: Admin
 * %%
 * Copyright (C) 2019 - 2024 INRAE - UMR CARRTEL
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
export type ThemePreference = "light" | "dark" | "system";

const STORAGE_KEY = "fishola-admin-theme";
const DARK_QUERY = "(prefers-color-scheme: dark)";

/**
 * Mode nuit (#208) : préférence clair / sombre / système, mémorisée sur le
 * poste. Bulma 1 (via Buefy) gère nativement `<html data-theme="light|dark">` ;
 * l'attribut est toujours posé pour que le choix l'emporte sur le système.
 */
export default class ThemeService {
  private static mediaQuery: MediaQueryList | null = null;

  /** Applique la préférence mémorisée ; à appeler avant le montage de l'app. */
  static init() {
    if (typeof window.matchMedia === "function") {
      ThemeService.mediaQuery = window.matchMedia(DARK_QUERY);
      ThemeService.mediaQuery.addEventListener("change", () => {
        if (ThemeService.getPreference() === "system") {
          ThemeService.apply();
        }
      });
    }
    ThemeService.apply();
  }

  static getPreference(): ThemePreference {
    try {
      const stored = window.localStorage.getItem(STORAGE_KEY);
      if (stored === "light" || stored === "dark" || stored === "system") {
        return stored;
      }
    } catch (e) {
      // Stockage indisponible (navigation privée) : on suit le système.
    }
    return "system";
  }

  static setPreference(preference: ThemePreference) {
    try {
      window.localStorage.setItem(STORAGE_KEY, preference);
    } catch (e) {
      console.warn("Préférence de thème non mémorisée", e);
    }
    ThemeService.apply();
  }

  private static apply() {
    const preference = ThemeService.getPreference();
    const systemDark = !!ThemeService.mediaQuery && ThemeService.mediaQuery.matches;
    const theme = preference === "system" ? (systemDark ? "dark" : "light") : preference;
    document.documentElement.setAttribute("data-theme", theme);
  }
}
