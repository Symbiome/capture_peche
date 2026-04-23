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
export default class UtilityServices {
  static parseLocalDateTime(someLocalDateTime: number[]): Date {
    if (someLocalDateTime[5]) {
      return new Date(
        someLocalDateTime[0],
        someLocalDateTime[1] - 1,
        someLocalDateTime[2],
        someLocalDateTime[3],
        someLocalDateTime[4],
        someLocalDateTime[5]
      );
    } else {
      return new Date(
        someLocalDateTime[0],
        someLocalDateTime[1] - 1,
        someLocalDateTime[2],
        someLocalDateTime[3],
        someLocalDateTime[4]
      );
    }
  }

  static formatDate(puet: number[]): string {
    const theDate: Date = UtilityServices.parseLocalDateTime(puet);
    const hourOptions: Intl.DateTimeFormatOptions = {
      month: "numeric",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "numeric"
    };
    const result: string = theDate.toLocaleTimeString("fr-FR", hourOptions);
    return result;
  }

  static camelCaseToUnderscore(camelCase: string) {
    return camelCase
      .replace("xport", "xports")
      .replace(/\.?([A-Z])/g, function(x, y) {
        return "_" + y.toLowerCase();
      })
      .replace(/^_/, "");
  }
}
