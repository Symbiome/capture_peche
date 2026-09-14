package fr.inrae.fishola.rest.imports;

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
 * Constantes du format d'import CSV opérateur (#71, template officiel 28 colonnes).
 */
public final class ImportSchema {

    private ImportSchema() {}

    /** En-tête attendu, dans l'ordre (séparateur « ; », UTF-8). */
    public static final List<String> EXPECTED_HEADER = List.of(
            "session_ref", "collection_method", "date", "heure_debut", "heure_fin",
            "eau_nom", "commune", "mode_peche", "technique", "nb_lignes",
            "espece_ciblee", "bredouille",
            "enquete_age", "enquete_sexe", "enquete_commune", "enquete_experience_annees",
            "enquete_importance", "enquete_membre_club", "enquete_sorties_par_an",
            "capture_espece", "capture_longueur_cm", "capture_poids_g", "capture_conservation",
            "capture_nombre", "capture_classe_taille", "capture_prelevement",
            "capture_marque", "capture_pathologies");

    public static final Set<String> COLLECTION_METHODS = Set.of(
            "saisie_pecheur", "enquete", "carnet_volontaire", "carnet_obligatoire");

    /** Liste (ouverte) figée ici pour la validation ; enum fishing_mode dédié à venir. */
    public static final Set<String> FISHING_MODES = Set.of(
            "bateau", "float tube", "kayak", "a pied", "belly boat", "du bord", "rive");

    // Étages de validation.
    public static final String STRUCTUREL = "structurel";
    public static final String REFERENTIEL = "referentiel";
    public static final String METIER = "metier";

    // Codes d'erreur stables (repris dans import_row_error.code).
    public static final String STRUCT_HEADER = "STRUCT_HEADER";
    public static final String STRUCT_COLLECTION_METHOD = "STRUCT_COLLECTION_METHOD";
    public static final String STRUCT_DATE = "STRUCT_DATE";
    public static final String STRUCT_TIME = "STRUCT_TIME";
    public static final String STRUCT_TIME_ORDER = "STRUCT_TIME_ORDER";
    public static final String STRUCT_MODE = "STRUCT_MODE";
    public static final String STRUCT_BREDOUILLE = "STRUCT_BREDOUILLE";
    public static final String REF_SPECIES = "REF_SPECIES";
    public static final String REF_TECHNIQUE = "REF_TECHNIQUE";
    public static final String REF_WATER_ENTITY = "REF_WATER_ENTITY";
    /** Entité hydro résolue mais hors du périmètre géographique de l'opérateur (#63 / #71). */
    public static final String REF_WATER_ENTITY_SCOPE = "REF_WATER_ENTITY_SCOPE";
    public static final String METIER_QUANTITY = "METIER_QUANTITY";
    public static final String METIER_SIZE_ABERRANT = "METIER_SIZE_ABERRANT";
    public static final String METIER_BREDOUILLE = "METIER_BREDOUILLE";
}
