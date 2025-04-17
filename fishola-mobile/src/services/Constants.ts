/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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

export default class Constants {

    static NEW_TRIP_ID:string = 'NEW';
    static RUNNING_ID:string = 'RUNNING';
    static NEW_CATCH_ID:string = 'NEW';

    static MONTHS = ["Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc"];

    static baseApiUrl():string {
        let result = import.meta.env.VITE__API_URL;
        if (!result) {
            result = location.protocol + "//" + location.hostname;
            if (import.meta.env.VITE__API_DEFAULT_PORT) {
              result += ":" + import.meta.env.VITE__API_DEFAULT_PORT;
            }
            result += "/api";
        }
        return result;
    }

    static baseDeeplinkSafeApiUrl():string {
        let result = import.meta.env.VITE__DEEPLINK_SAFE_API_URL;
        if (!result) {
            result = Constants.baseApiUrl();
        }
        return result;
    }

    static apiUrl(path:string):string {
        const result = Constants.baseApiUrl() + path;
        return result;
    }

    static deeplinkSafeApiUrl(path:string):string {
        const result = Constants.baseDeeplinkSafeApiUrl() + path;
        return result;
    }

}
