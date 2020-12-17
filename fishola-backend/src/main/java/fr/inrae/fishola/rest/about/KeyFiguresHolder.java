package fr.inrae.fishola.rest.about;

import java.util.Optional;

public class KeyFiguresHolder {

    /**
     * Dernière instance de KeyFigures calculée. Celle-ci va rester en mémoire jusqu'à ce qu'elle expire (cf délai dans
     * la configuration).
     */
    protected static KeyFigures latestKeyFigures;

    public static Optional<KeyFigures> get() {
        return Optional.ofNullable(latestKeyFigures);
    }

    public static void set(KeyFigures newInstance) {
        latestKeyFigures = newInstance;
    }

    public static void unset() {
        latestKeyFigures = null;
    }

}
