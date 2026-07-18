"""
Tests du pipeline d'import (#32) — classification des 3 étages, sans base.

On utilise un référentiel SIMULÉ (pas d'accès à la BDD Fishola partagée) : ces
tests valident la logique structurel/référentiel/métier sur les templates
officiels. L'insertion réelle (trip/catch) se teste via `manage.py import_trips`
contre la base de recette.
"""
from pathlib import Path

from django.test import SimpleTestCase

from backoffice.imports import schema
from backoffice.imports.parsing import read_rows
from backoffice.imports.service import ImportService

FIXTURES = Path(__file__).resolve().parent / "fixtures"


class FakeReferential:
    """Référentiel en mémoire couvrant les libellés des templates officiels."""
    SPECIES = {"brochet", "perche", "sandre", "truite fario", "gardon"}
    TECHNIQUES = {"leurre", "vif", "mouche", "coup"}
    WATER = {"lac du bourget", "la saone", "le rhone", "lac d'annecy"}
    BOUNDS = {"perche": (5, 80)}  # borne pour déclencher INV3 (Perche 250 cm)

    def species_id(self, name):
        n = (name or "").lower()
        return n if n in self.SPECIES else None

    def technique_id(self, name):
        n = (name or "").lower()
        return n if n in self.TECHNIQUES else None

    def water_entity_id(self, eau, commune):
        n = (eau or "").lower()
        return n if n in self.WATER else None

    def size_bounds(self, species_id):
        return self.BOUNDS.get(species_id)


def _rows(name):
    return read_rows((FIXTURES / name).read_text(encoding="utf-8-sig"))


class ImportValidationTest(SimpleTestCase):
    def setUp(self):
        self.service = ImportService(referential=FakeReferential())

    def test_entete_conforme(self):
        header, _ = _rows("sessions_valid.csv")
        self.assertEqual(header, schema.EXPECTED_HEADER)

    def test_template_valide_sans_erreur_et_regroupe(self):
        _, records = _rows("sessions_valid.csv")
        errors, sessions = self.service.validate_all(records)
        self.assertEqual(errors, [], f"erreurs inattendues : {[(e.line, e.code) for e in errors]}")
        self.assertEqual(set(sessions), {"S001", "S002", "S003", "S004", "S005"})
        captures = {k: sum(1 for (_, _, p) in v["records"] if p["has_capture"])
                    for k, v in sessions.items()}
        self.assertEqual(captures, {"S001": 2, "S002": 1, "S003": 1, "S004": 0, "S005": 1})

    def test_template_invalide_chaque_ligne_au_bon_etage(self):
        _, records = _rows("sessions_invalid.csv")
        expected = {
            "INV1": (schema.REFERENTIEL, schema.REF_SPECIES),
            "INV2": (schema.REFERENTIEL, schema.REF_WATER_ENTITY),
            "INV3": (schema.METIER, schema.METIER_SIZE_ABERRANT),
            "INV4": (schema.STRUCTUREL, schema.STRUCT_TIME_ORDER),
            "INV5": (schema.STRUCTUREL, schema.STRUCT_COLLECTION_METHOD),
            "INV6": (schema.METIER, schema.METIER_QUANTITY),
        }
        ref_by_line, line = {}, 1
        for rec in records:
            line += 1
            ref_by_line[line] = rec["session_ref"]
        errors, _ = self.service.validate_all(records)
        first_by_ref = {}
        for e in errors:
            first_by_ref.setdefault(ref_by_line[e.line], (e.stage, e.code))
        for ref, exp in expected.items():
            self.assertIn(ref, first_by_ref, f"{ref} aurait dû être rejetée")
            self.assertEqual(first_by_ref[ref], exp, f"{ref} : étage/code inattendu")
