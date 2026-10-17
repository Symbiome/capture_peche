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
 * Palette « Carnet de pêche » pour le code qui ne lit pas le CSS (Chart.js,
 * MapLibre). Couleurs reprises des maquettes Figma et de leurs pictogrammes ;
 * mêmes valeurs que les variables `@carnet-*` de less/_colors.less.
 */
export const CARNET_COLORS = {
  blue: "#0000E0",
  navy: "#142634",
  sand: "#F9F4F2",
  sky: "#0FAFFF",
  deepBlue: "#0067BF",
  orange: "#FE8401",
  magenta: "#D7257D",
  green: "#62BE55",
  darkGreen: "#1E794A",
  purple: "#7A41DC",
  yellow: "#FFCD0F",
  red: "#CA2134",
  pink: "#F97DBD",
  lavender: "#A3A3FF",
  grey: "#5B6B78",
};

/** Couleurs catégorielles des graphiques, dans l'ordre d'attribution. */
export const CARNET_CHART_PALETTE: string[] = [
  CARNET_COLORS.blue,
  CARNET_COLORS.orange,
  CARNET_COLORS.sky,
  CARNET_COLORS.magenta,
  CARNET_COLORS.green,
  CARNET_COLORS.purple,
  CARNET_COLORS.yellow,
  CARNET_COLORS.deepBlue,
  CARNET_COLORS.pink,
  CARNET_COLORS.darkGreen,
  CARNET_COLORS.lavender,
  CARNET_COLORS.red,
];

/** Couleurs des milieux aquatiques sur les cartes. */
export const CARNET_MAP_COLORS = {
  water: CARNET_COLORS.sky,
  waterOutline: CARNET_COLORS.deepBlue,
  selected: CARNET_COLORS.orange,
  catchPin: CARNET_COLORS.blue,
  emptyPin: CARNET_COLORS.grey,
  tripStart: CARNET_COLORS.green,
  tripEnd: CARNET_COLORS.red,
};
