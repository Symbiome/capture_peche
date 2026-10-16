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
import { reactive } from "vue";

export interface DialogButton {
  title: string;
  handler: () => void;
}

export interface DialogParams {
  title?: string;
  text: string;
  buttons: DialogButton[];
}

interface DialogState {
  visible: boolean;
  params: DialogParams | null;
}

const state = reactive<DialogState>({ visible: false, params: null });

/**
 * Boîte de dialogue unique de l'application (#284), affichée par
 * `AppDialog.vue` : remplace `vue-js-modal`, réservé à Vue 2. Les fonctions
 * `Helpers.confirm`, `Helpers.alert`… s'appuient dessus.
 */
export default class DialogService {
  static readonly state: Readonly<DialogState> = state;

  /**
   * Affiche la boîte de dialogue, en remplaçant celle éventuellement ouverte.
   *
   * @param params titre, texte (HTML autorisé) et boutons
   */
  static show(params: DialogParams): void {
    state.params = params;
    state.visible = true;
  }

  /** Ferme la boîte de dialogue sans appeler aucun bouton. */
  static hide(): void {
    state.visible = false;
  }
}
