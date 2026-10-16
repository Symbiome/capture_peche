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

export type EventHandler = (...args: any[]) => void;

const handlersByEvent = new Map<string, EventHandler[]>();

/**
 * Bus d'événements global de l'application (#284), qui remplace
 * `this.$root.$emit/$on/$off` de Vue 2, supprimés en Vue 3.
 *
 * Même sémantique que Vue 2 : un événement transporte plusieurs arguments, et
 * `off(event)` sans handler retire tous les abonnés de cet événement.
 */
export default class EventBus {
  /**
   * Abonne un handler à un événement.
   *
   * @param event nom de l'événement
   * @param handler fonction appelée avec les arguments de `emit`
   */
  static on(event: string, handler: EventHandler): void {
    const handlers = handlersByEvent.get(event) || [];
    handlers.push(handler);
    handlersByEvent.set(event, handlers);
  }

  /**
   * Désabonne un handler, ou tous les handlers de l'événement si aucun n'est
   * précisé.
   *
   * @param event nom de l'événement
   * @param handler handler à retirer (facultatif)
   */
  static off(event: string, handler?: EventHandler): void {
    if (!handler) {
      handlersByEvent.delete(event);
      return;
    }
    const remaining = (handlersByEvent.get(event) || []).filter((h) => h !== handler);
    handlersByEvent.set(event, remaining);
  }

  /**
   * Publie un événement. Une erreur dans un handler est journalisée sans
   * empêcher l'appel des suivants, comme le faisait Vue 2.
   *
   * @param event nom de l'événement
   * @param args arguments transmis aux handlers
   */
  static emit(event: string, ...args: any[]): void {
    const handlers = [...(handlersByEvent.get(event) || [])];
    handlers.forEach((handler) => {
      try {
        handler(...args);
      } catch (e) {
        console.error(`Erreur dans un abonné à l'événement « ${event} »`, e);
      }
    });
  }
}
