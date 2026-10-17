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
import Hammer from "hammerjs";

const MAX_SCALE = 5;
const DOUBLE_TAP_SCALE = 2;

interface ZoomState {
  scale: number;
  offsetX: number;
  offsetY: number;
  panOffsetX: number;
  panOffsetY: number;
  pinchCentreX: number;
  pinchCentreY: number;
}

/**
 * Zoom au pincement, déplacement et double tap sur un élément (#284) :
 * remplace `vue-pinch-zoom` (Vue 2 uniquement) pour les images, et reprend le
 * code Hammer qui servait déjà au PDF du permis de pêche.
 *
 * @param element élément à zoomer, dans un conteneur qui le contraint
 * @param onZoomChange appelé avec `true` quand l'élément est zoomé, `false` revenu à l'échelle 1
 * @returns fonction qui retire les gestionnaires de gestes
 */
export function enablePinchZoom(element: HTMLElement, onZoomChange: (zoomed: boolean) => void): () => void {
  const hammer = new Hammer(element, {});
  // Le pincement est désactivé par défaut et le déplacement limité à l'axe X.
  hammer.get("pinch").set({ enable: true });
  hammer.get("pan").set({ direction: Hammer.DIRECTION_ALL });

  const state: ZoomState = { scale: 1, offsetX: 0, offsetY: 0, panOffsetX: 0, panOffsetY: 0, pinchCentreX: 0, pinchCentreY: 0 };
  const apply = (offsetX: number, offsetY: number, scale: number) => {
    element.style.transform = `translate(${offsetX}px,${offsetY}px) scale(${scale})`;
  };

  bindPinch(hammer, element, state, apply, onZoomChange);
  bindPan(hammer, element, state, apply);
  hammer.on("doubletap", () => {
    state.scale = state.scale > 1 ? 1 : DOUBLE_TAP_SCALE;
    state.offsetX = 0;
    state.offsetY = 0;
    apply(0, 0, state.scale);
    onZoomChange(state.scale > 1);
  });
  return () => hammer.destroy();
}

/**
 * Débordement autorisé de l'élément zoomé hors de son conteneur, par axe.
 */
function overlap(element: HTMLElement, scale: number): { x: number; y: number } {
  const container = element.parentElement!;
  return {
    x: Math.max(0, Math.round((element.offsetWidth * scale - container.offsetWidth) / 2)),
    y: Math.max(0, Math.round((element.offsetHeight * scale - container.offsetHeight) / 2)),
  };
}

function clamp(value: number, limit: number): number {
  return Math.max(-limit, Math.min(limit, value));
}

/**
 * Pincement : l'échelle est relative au début du geste ; le point pincé reste
 * sous les doigts, et l'élément peut être déplacé pendant le pincement.
 */
function bindPinch(
  hammer: HammerManager,
  element: HTMLElement,
  state: ZoomState,
  apply: (x: number, y: number, scale: number) => void,
  onZoomChange: (zoomed: boolean) => void
) {
  hammer.on("pinchstart", (e) => {
    onZoomChange(true);
    state.panOffsetX = state.offsetX;
    state.panOffsetY = state.offsetY;
    // e.center est relatif à l'écran, d'où les dimensions de l'écran.
    state.pinchCentreX = Math.round((e.center.x - state.panOffsetX - document.body.clientWidth / 2) / state.scale);
    state.pinchCentreY = Math.round((e.center.y - state.panOffsetY - document.body.clientHeight / 2) / state.scale);
  });
  hammer.on("pinch", (e) => {
    const scale = Math.min(MAX_SCALE, Math.max(1, state.scale * e.scale));
    const limits = overlap(element, scale);
    state.offsetX = clamp(state.panOffsetX + Math.round(state.pinchCentreX * (1 - e.scale)) + e.deltaX, limits.x);
    state.offsetY = clamp(state.panOffsetY + Math.round(state.pinchCentreY * (1 - e.scale)) + e.deltaY, limits.y);
    apply(state.offsetX, state.offsetY, scale);
  });
  hammer.on("pinchend", (e) => {
    state.scale = Math.min(MAX_SCALE, Math.max(1, state.scale * e.scale));
    if (state.scale <= 1) {
      onZoomChange(false);
    }
  });
}

/**
 * Déplacement de l'élément zoomé, contraint aux bords du conteneur.
 */
function bindPan(hammer: HammerManager, element: HTMLElement, state: ZoomState, apply: (x: number, y: number, scale: number) => void) {
  hammer.on("panstart", () => {
    state.panOffsetX = state.offsetX;
    state.panOffsetY = state.offsetY;
  });
  hammer.on("panmove", (e) => {
    const limits = overlap(element, state.scale);
    state.panOffsetX = clamp(state.offsetX + e.deltaX, limits.x);
    state.panOffsetY = clamp(state.offsetY + e.deltaY, limits.y);
    apply(state.panOffsetX, state.panOffsetY, state.scale);
  });
  hammer.on("panend", () => {
    // La position finale tient compte des contraintes appliquées pendant panmove.
    state.offsetX = state.panOffsetX;
    state.offsetY = state.panOffsetY;
  });
}
