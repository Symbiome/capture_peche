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
 * Statut réseau online/offline (#10). S'appuie sur `navigator.onLine` + les
 * évènements `online`/`offline` du navigateur (disponibles aussi dans la WebView
 * Capacitor), sans dépendance native supplémentaire. Le reste de l'app dérivait
 * l'offline de timeouts serveur (5 s) ; ce service donne un signal immédiat pour
 * déclencher la synchronisation dès le retour du réseau plutôt qu'au prochain
 * polling.
 */
export default class NetworkStatusService {
  static isOnline(): boolean {
    // En l'absence d'API (SSR/tests), on suppose online pour ne pas bloquer.
    return typeof navigator === "undefined" || navigator.onLine !== false;
  }

  static isOffline(): boolean {
    return !NetworkStatusService.isOnline();
  }

  /**
   * Abonne un callback aux transitions de connectivité. Retourne une fonction de
   * désabonnement (à appeler dans beforeDestroy).
   */
  static subscribe(onChange: (online: boolean) => void): () => void {
    const onOnline = () => onChange(true);
    const onOffline = () => onChange(false);
    window.addEventListener("online", onOnline);
    window.addEventListener("offline", onOffline);
    return () => {
      window.removeEventListener("online", onOnline);
      window.removeEventListener("offline", onOffline);
    };
  }
}
