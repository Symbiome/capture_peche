"""
Tests de la saisie manuelle opérateur (#62) — validation métier, sans base.

Même principe que `test_import.py` : référentiel SIMULÉ (aucun accès à la BDD
partagée), on valide la logique métier (cohérence bredouille, lots, taille
aberrante Q8) — partagée avec l'import via `size_out_of_bounds`. La persistance
`Trip`/`Catch` se teste contre la base de recette (hors tests unitaires).
"""
from django.test import SimpleTestCase

from backoffice.imports.service import size_out_of_bounds
from backoffice.manual_entry import CaptureData, ManualTripForm, validate_manual


class FakeReferential:
    """Bornes de taille pour déclencher l'aberration (Perche 5–80 cm)."""
    BOUNDS = {"perche": (5, 80)}

    def size_bounds(self, species_id):
        return self.BOUNDS.get(species_id)


class SizeOutOfBoundsTest(SimpleTestCase):
    def test_pas_de_bornes_ou_pas_de_taille(self):
        self.assertFalse(size_out_of_bounds(None, 42))
        self.assertFalse(size_out_of_bounds((5, 80), None))

    def test_dans_les_bornes(self):
        self.assertFalse(size_out_of_bounds((5, 80), 5))
        self.assertFalse(size_out_of_bounds((5, 80), 80))
        self.assertFalse(size_out_of_bounds((5, 80), 40))

    def test_hors_bornes(self):
        self.assertTrue(size_out_of_bounds((5, 80), 4))
        self.assertTrue(size_out_of_bounds((5, 80), 250))

    def test_borne_ouverte(self):
        # Une borne None ne contraint pas ce côté.
        self.assertFalse(size_out_of_bounds((None, 80), 1))
        self.assertFalse(size_out_of_bounds((5, None), 10_000))
        self.assertTrue(size_out_of_bounds((5, None), 2))


class ValidateManualTest(SimpleTestCase):
    def setUp(self):
        self.ref = FakeReferential()

    def test_capture_valide_sans_erreur(self):
        captures = [CaptureData(species_id="perche", quantity=3, size=25, kept=True)]
        self.assertEqual(validate_manual(self.ref, bredouille=False, captures=captures), [])

    def test_quantite_lot_inferieure_a_un(self):
        captures = [CaptureData(species_id="perche", quantity=0, size=25)]
        errs = validate_manual(self.ref, bredouille=False, captures=captures)
        self.assertEqual([(e.index, e.field) for e in errs], [(0, "quantity")])

    def test_taille_aberrante(self):
        captures = [CaptureData(species_id="perche", quantity=1, size=250)]
        errs = validate_manual(self.ref, bredouille=False, captures=captures)
        self.assertEqual(len(errs), 1)
        self.assertEqual((errs[0].index, errs[0].field), (0, "size"))

    def test_taille_sans_bornes_connues_acceptee(self):
        # Espèce sans bornes : aucune aberration possible.
        captures = [CaptureData(species_id="silure", quantity=1, size=250)]
        self.assertEqual(validate_manual(self.ref, bredouille=False, captures=captures), [])

    def test_bredouille_avec_captures_incoherent(self):
        captures = [CaptureData(species_id="perche", quantity=1, size=25)]
        errs = validate_manual(self.ref, bredouille=True, captures=captures)
        self.assertEqual([(e.index, e.field) for e in errs], [(None, "bredouille")])

    def test_bredouille_sans_capture_ok(self):
        self.assertEqual(validate_manual(self.ref, bredouille=True, captures=[]), [])

    def test_plusieurs_captures_erreurs_indexees(self):
        captures = [
            CaptureData(species_id="perche", quantity=1, size=25),   # ok
            CaptureData(species_id="perche", quantity=0, size=25),   # quantité KO
            CaptureData(species_id="perche", quantity=1, size=250),  # taille KO
        ]
        errs = validate_manual(self.ref, bredouille=False, captures=captures)
        self.assertEqual({(e.index, e.field) for e in errs}, {(1, "quantity"), (2, "size")})


class ManualTripFormLockTest(SimpleTestCase):
    """Verrouillage de la méthode de recueil par le profil opérateur (M-F)."""

    def test_methode_verrouillee_restreint_et_desactive(self):
        field = ManualTripForm(locked_method="enquete").fields["collection_method"]
        self.assertTrue(field.disabled)
        self.assertEqual([v for v, _ in field.choices], ["enquete"])
        self.assertEqual(field.initial, "enquete")

    def test_sans_verrou_liste_restreinte_complete(self):
        field = ManualTripForm().fields["collection_method"]
        self.assertFalse(field.disabled)
        self.assertEqual({v for v, _ in field.choices},
                         {"enquete", "carnet_volontaire", "carnet_obligatoire"})

    def test_champ_desactive_ignore_la_valeur_soumise(self):
        # `disabled=True` → Django retient l'initiale, quelle que soit la soumission.
        form = ManualTripForm(
            data={"collection_method": "carnet_obligatoire", "day": "2026-01-01",
                  "start_time": "08:00", "end_time": "10:00", "eau_nom": "Lac",
                  "technique": "", "bredouille": "on"},
            locked_method="enquete")
        form.is_valid()  # déclenche le nettoyage
        self.assertEqual(form.cleaned_data.get("collection_method"), "enquete")
