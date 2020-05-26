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

    // Css class for which the "keyboardShowing" class will be added (but no other change)
    static CSS_KEYBOARD_SENSITIVE_CLASS = ".keyboardSensitive";


    // Height (in px) under which elements tagged with the
    // small resolution class should also be hidden
    static SMALL_SCREEN_HEIGHT = 250;

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
        let toHide = [KeyboardManager.CSS_HIDE_CLASS];
        // On small resolution, also hide CSS_HIDE_CLASS_SMALL_SCREEN_ONLY
        if (keyboardInfo.keyboardHeight < KeyboardManager.SMALL_SCREEN_HEIGHT) {
            toHide.push(KeyboardManager.CSS_HIDE_CLASS_SMALL_SCREEN_ONLY);
        }
        KeyboardManager.hideOrRevealElementsWithClass(true, toHide);
    }

    private static keyboardHiding() {
        // Make all hidden DOM elements visible again
        let toHide = [KeyboardManager.CSS_HIDE_CLASS, KeyboardManager.CSS_HIDE_CLASS_SMALL_SCREEN_ONLY];
        KeyboardManager.hideOrRevealElementsWithClass(false, toHide);
    }

    // Hides (or reveal) all elements tagged with the given css classes
    // Also add "keyboardShowing" class for .keyboardSensitive elements
    private static hideOrRevealElementsWithClass(keyboardShowing: boolean, cssClassesList: Array<String>) {
        // Step 1: hide elements
        let selector = cssClassesList.join(",");
        let toReveals = <HTMLElement[]><unknown>document.querySelectorAll(selector);
        toReveals.forEach( toReveal => {
            if (keyboardShowing) {
                toReveal.classList.add("hidden");
            } else {
                toReveal.classList.remove("hidden");
            }
        });

        // Step 2: add 'keyboardShowing' class for .keyboardSensitive elements
        let keyboardSensitives = <HTMLElement[]><unknown>document.querySelectorAll(".keyboardSensitive");
        keyboardSensitives.forEach( keyboardSensitive => {
            if (keyboardShowing) {
                console.log("Add keyboardShowing to ", keyboardSensitive.classList);
                keyboardSensitive.classList.add("keyboardShowing");
                keyboardSensitive.classList.remove("keyboardSensitive");
                console.log("=> ", keyboardSensitive.classList);
            } else {
                console.log("Removing keyboardShowing from ", keyboardSensitive.classList);
                keyboardSensitive.classList.remove("keyboardShowing");
                keyboardSensitive.classList.add("keyboardSensitive");
            }
        });
    }
}

