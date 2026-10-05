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
import { Chart } from "chart.js";

export type ThemePreference = "light" | "dark" | "system";
export type Theme = "light" | "dark";

const STORAGE_KEY = "fishola-theme";
const DARK_QUERY = "(prefers-color-scheme: dark)";

// Couleurs des textes et quadrillages Chart.js, qui ne lisent pas le CSS.
const CHART_COLORS: Record<Theme, { text: string; grid: string }> = {
  light: { text: "#636E72", grid: "rgba(0, 0, 0, 0.1)" },
  dark: { text: "#A9B4B9", grid: "rgba(255, 255, 255, 0.12)" },
};

/**
 * Mode nuit (#208) : préférence clair / sombre / système, mémorisée sur
 * l'appareil, appliquée via `<html data-theme>` (cf. variables de main.less).
 * En mode « système », suit `prefers-color-scheme` à chaud.
 */
export default class ThemeService {
  private static mediaQuery: MediaQueryList | null = null;

  /** Applique la préférence mémorisée ; à appeler au démarrage de l'app. */
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

  static resolve(preference: ThemePreference): Theme {
    if (preference !== "system") {
      return preference;
    }
    return ThemeService.mediaQuery && ThemeService.mediaQuery.matches ? "dark" : "light";
  }

  private static apply() {
    const theme = ThemeService.resolve(ThemeService.getPreference());
    document.documentElement.setAttribute("data-theme", theme);
    Chart.defaults.color = CHART_COLORS[theme].text;
    Chart.defaults.borderColor = CHART_COLORS[theme].grid;
  }
}
