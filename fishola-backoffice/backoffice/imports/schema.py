"""Constantes du format d'import CSV opérateur (#32, 28 colonnes).

Template **proposé**, dérivé du CdC (Annexe I), du modèle backend (trip/catch/…)
et de la note complémentaire Symbiome (Q3 lots, Q7 import, Q8 bornes de tailles).
Ce n'est **pas** un format officiel fourni par le client : plusieurs choix restent
à valider par Symbiome/UFBRMC (format, enum mode de pêche, bins de classes de
taille, valeurs de bornes…). Cf. `.claude/docs/04-cadrage-3153-import-csv.md` §7.
"""

# En-tête attendu, dans l'ordre (séparateur « ; », UTF-8).
EXPECTED_HEADER = [
    "session_ref", "collection_method", "date", "heure_debut", "heure_fin",
    "eau_nom", "commune", "mode_peche", "technique", "nb_lignes",
    "espece_ciblee", "bredouille",
    "enquete_age", "enquete_sexe", "enquete_commune", "enquete_experience_annees",
    "enquete_importance", "enquete_membre_club", "enquete_sorties_par_an",
    "capture_espece", "capture_longueur_cm", "capture_poids_g", "capture_conservation",
    "capture_nombre", "capture_classe_taille", "capture_prelevement",
    "capture_marque", "capture_pathologies",
]

COLLECTION_METHODS = {"saisie_pecheur", "enquete", "carnet_volontaire", "carnet_obligatoire"}

# Liste (ouverte) figée ici pour la validation ; enum fishing_mode dédié à venir.
FISHING_MODES = {"bateau", "float tube", "kayak", "a pied", "belly boat", "du bord", "rive"}

# Étages de validation.
STRUCTUREL = "structurel"
REFERENTIEL = "referentiel"
METIER = "metier"

# Codes d'erreur stables (repris dans import_row_error.code).
STRUCT_HEADER = "STRUCT_HEADER"
STRUCT_COLLECTION_METHOD = "STRUCT_COLLECTION_METHOD"
STRUCT_DATE = "STRUCT_DATE"
STRUCT_TIME = "STRUCT_TIME"
STRUCT_TIME_ORDER = "STRUCT_TIME_ORDER"
STRUCT_MODE = "STRUCT_MODE"
STRUCT_BREDOUILLE = "STRUCT_BREDOUILLE"
REF_SPECIES = "REF_SPECIES"
REF_TECHNIQUE = "REF_TECHNIQUE"
REF_WATER_ENTITY = "REF_WATER_ENTITY"
METIER_QUANTITY = "METIER_QUANTITY"
METIER_SIZE_ABERRANT = "METIER_SIZE_ABERRANT"
METIER_BREDOUILLE = "METIER_BREDOUILLE"
