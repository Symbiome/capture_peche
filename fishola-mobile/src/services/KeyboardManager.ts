/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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
import { Plugins, KeyboardInfo } from '@capacitor/core';
const { Keyboard} = Plugins;

/**
 * In charge of handling mobile keyboad, for instance : 
 * - Configuring iOS keyboard
 * - When keyboard shows, hide elements tagged with .hiddenWhenKeyboardShows css class
 * - When keyboard shows, add .keyboardShowing css class to .keyboardSensitive elements
 */
export default class KeyboardManager {
    // Css class to hide when keyboard shows (any resolution)
    static CSS_HIDE_CLASS = ".hiddenWhenKeyboardShows";

    // Css class to hide when keyboard shows (small resolution only)
    static CSS_HIDE_CLASS_SMALL_SCREEN_ONLY = ".hiddenWhenKeyboardShows_SmallScreensOnly";

    // Css classes for which the "keyboardShowing" class will be added (but no other change)
    static CSS_KEYBOARD_SENSITIVE_CLASS = ".keyboardSensitive, .page, .pane, h1";


    // Height (in px x screen density) under which elements tagged with the
    // small resolution class should also be hidden
    static SMALL_SCREEN_HEIGHT = 800;

    static alreadyConfigured = false;

    static setupKeyboardConfiguration() {
        // Make sure the keyboard setup is only done once
        if (!KeyboardManager.alreadyConfigured) {
            KeyboardManager.alreadyConfigured = true;

            // Step 1: Keyboard configuration: show accessory bar (on iOS)
            // This bar is displayed on top of keyboard and allows to navigate
            // Through inputs + hide keyboard
            Keyboard.setAccessoryBarVisible({isVisible: true});

            // Step 2: Listen for keyboard showing events
            // 2.1: Keyboard is just about to show
            Keyboard.addListener('keyboardWillShow', (info: KeyboardInfo) => {
                KeyboardManager.keyboardShowing(info);
            });
            // 2.2 Keyboard has done hidding
            Keyboard.addListener('keyboardWillHide', () => {
                KeyboardManager.keyboardHiding();
            });
        }
    }

    private static keyboardShowing(keyboardInfo: KeyboardInfo) {
        // Hide all DOM elements tagged with CSS_HIDE_CLASS
        const toHide = [KeyboardManager.CSS_HIDE_CLASS];
        // On small resolution, also hide CSS_HIDE_CLASS_SMALL_SCREEN_ONLY
        console.debug("KeyboardShowing - Full Screen " + screen.height + "px, keyboard " + keyboardInfo.keyboardHeight + "px, devicePixelRatio " +  window.devicePixelRatio);
        console.debug("KeyboardShowing " + (screen.height -  keyboardInfo.keyboardHeight) + "*"+   window.devicePixelRatio + "=" + ((screen.height -  keyboardInfo.keyboardHeight) * window.devicePixelRatio) + " < " + KeyboardManager.SMALL_SCREEN_HEIGHT+ ": "+ ((screen.height -  keyboardInfo.keyboardHeight) * window.devicePixelRatio < KeyboardManager.SMALL_SCREEN_HEIGHT));
        if ((screen.height -  keyboardInfo.keyboardHeight) * window.devicePixelRatio < KeyboardManager.SMALL_SCREEN_HEIGHT) {
            toHide.push(KeyboardManager.CSS_HIDE_CLASS_SMALL_SCREEN_ONLY);
        }
        KeyboardManager.hideOrRevealElementsWithClass(true, toHide);
    }

    private static keyboardHiding() {
        // Make all hidden DOM elements visible again
        const toHide = [KeyboardManager.CSS_HIDE_CLASS, KeyboardManager.CSS_HIDE_CLASS_SMALL_SCREEN_ONLY];
        KeyboardManager.hideOrRevealElementsWithClass(false, toHide);
    }

    // Hides (or reveal) all elements tagged with the given css classes
    // Also add "keyboardShowing" class for .keyboardSensitive elements
    private static hideOrRevealElementsWithClass(keyboardShowing: boolean, cssClassesList: Array<String>) {
        // Step 1: hide elements
        const selector = cssClassesList.join(",");
        const toReveals = <HTMLElement[]><unknown>document.querySelectorAll(selector);
        toReveals.forEach( toReveal => {
            if (keyboardShowing) {
                toReveal.classList.add("hidden");
            } else {
                toReveal.classList.remove("hidden");
            }
        });

        // Step 2: add 'keyboardShowing' class for .keyboardSensitive elements (and .page)
        const keyboardSensitives = <HTMLElement[]><unknown>document.querySelectorAll(KeyboardManager.CSS_KEYBOARD_SENSITIVE_CLASS);
        keyboardSensitives.forEach( keyboardSensitive => {
            if (keyboardShowing) {
                keyboardSensitive.classList.add("keyboardShowing");
            } else {
                keyboardSensitive.classList.remove("keyboardShowing");
            }
        });
    }
}

