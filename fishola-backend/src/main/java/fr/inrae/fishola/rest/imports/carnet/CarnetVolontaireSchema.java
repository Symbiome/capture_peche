package fr.inrae.fishola.rest.imports.carnet;

/*-
 * #%L
 * Fishola :: Backend
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

import java.util.List;
import java.util.Set;

/**
 * Constantes du format d'import CSV opérateur « carnet volontaire » (#143), dédié —
 * distinct du format générique {@link fr.inrae.fishola.rest.imports.ImportSchema}.
 */
public final class CarnetVolontaireSchema {

    private CarnetVolontaireSchema() {}

    /** En-tête attendu, dans l'ordre (séparateur « ; », UTF-8). */
    public static final List<String> EXPECTED_HEADER = List.of(
            "session_ref", "eau_nom", "commune", "date", "mode_peche",
            "heure_debut", "heure_fin", "technique_principale", "technique_secondaire",
            "nombre_lignes", "appat_leurre", "espece_recherchee", "observations_diverses", "bredouille",
            "capture_espece", "capture_origine_trf", "capture_technique", "capture_appat_leurre",
            "capture_heure", "capture_taille", "capture_poids", "capture_nombre", "capture_conservee",
            "capture_taille_min", "capture_taille_max", "capture_marque", "capture_marque_numero");

    /** Modalité acceptant l'absence d'espèce recherchée. */
    public static final String ESPECE_RECHERCHEE_AUCUNE = "aucune";

    /**
     * Référentiel fermé propre à ce format (spec client), distinct de
     * {@code ImportSchema.FISHING_MODES} (vocabulaire plus large côté format générique).
     */
    public static final Set<String> MODES_PECHE = Set.of(
            "bateau", "float tube/canoe", "bord itinerant", "bord statique");

    public static final Set<String> TROUT_ORIGINS = Set.of("naturelle", "deversement", "inconnue");

    // Étages de validation (mêmes valeurs que ImportSchema.STRUCTUREL/REFERENTIEL/METIER).
    public static final String STRUCTUREL = "structurel";
    public static final String REFERENTIEL = "referentiel";
    public static final String METIER = "metier";

    // Codes d'erreur stables (repris dans import_row_error.code).
    public static final String STRUCT_HEADER = "STRUCT_HEADER";
    public static final String STRUCT_DATE = "STRUCT_DATE";
    public static final String STRUCT_TIME = "STRUCT_TIME";
    public static final String STRUCT_TIME_ORDER = "STRUCT_TIME_ORDER";
    public static final String STRUCT_MODE = "STRUCT_MODE";
    public static final String STRUCT_BREDOUILLE = "STRUCT_BREDOUILLE";
    public static final String STRUCT_TROUT_ORIGIN = "STRUCT_TROUT_ORIGIN";
    public static final String REF_SPECIES = "REF_SPECIES";
    public static final String REF_EXPECTED_SPECIES = "REF_EXPECTED_SPECIES";
    public static final String REF_TECHNIQUE = "REF_TECHNIQUE";
    public static final String REF_TECHNIQUE_SECONDARY = "REF_TECHNIQUE_SECONDARY";
    public static final String REF_WATER_ENTITY = "REF_WATER_ENTITY";
    public static final String REF_WATER_ENTITY_SCOPE = "REF_WATER_ENTITY_SCOPE";
    public static final String METIER_QUANTITY = "METIER_QUANTITY";
    public static final String METIER_SIZE_ABERRANT = "METIER_SIZE_ABERRANT";
    public static final String METIER_SIZE_REQUIRED = "METIER_SIZE_REQUIRED";
    public static final String METIER_LOT_BOUNDS = "METIER_LOT_BOUNDS";
    public static final String METIER_TAG_REF = "METIER_TAG_REF";
    public static final String METIER_BREDOUILLE = "METIER_BREDOUILLE";
}
