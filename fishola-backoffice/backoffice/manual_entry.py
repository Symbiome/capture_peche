"""
Saisie manuelle opérateur (#62, CdC 3.1.5.3).

Chemin de création **dédié et contrôlé** d'une sortie + ses captures, à la main,
depuis le back-office — distinct de l'admin brut de `Trip` (verrouillé en lecture
seule) et de l'import CSV de masse (#32).

Principe : **un seul jeu de règles** avec l'import. La résolution de l'entité
hydro et la règle de taille aberrante (Q8) sont réutilisées telles quelles
(`imports.service.Referential`, `imports.service.size_out_of_bounds`). Ce module
n'ajoute que :
  - la validation *structurelle* via les formulaires Django (types, date ≤ jour,
    fin > début) et *référentielle* via des `ModelChoiceField` (espèce/technique) ;
  - la validation *métier* restante (quantité de lot, taille aberrante, cohérence
    bredouille) sous forme DB-free et testable ;
  - la persistance transactionnelle `Trip` + `Catch` (même schéma que l'import).

Limites V1 (assumées, alignées sur l'import) :
  - méthode de recueil **verrouillée** sur `{enquete, carnet_volontaire,
    carnet_obligatoire}` — jamais `saisie_pecheur` (cf. cadrage §3, option a) ;
  - sortie opérateur sans profil pêcheur → `owner_id` NULL ; le rattachement
    enquêté (`surveyed_angler`) attend l'arbitrage RGPD (I-F) ;
  - `mode_peche` / `espece_ciblee` saisis et validés mais **pas encore persistés**
    (mêmes colonnes manquantes que l'import : enum `fishing_mode`, espèce ciblée) ;
  - localisation par **recherche de nom** (comme l'import) ; le point sur la carte
    (clic-pour-placer, M-D) est un incrément séparé.
"""
from __future__ import annotations

import uuid
from dataclasses import dataclass, field

from django import forms
from django.db import transaction
from django.utils import timezone
from unfold.widgets import (
    UnfoldAdminIntegerFieldWidget,
    UnfoldAdminSelectWidget,
    UnfoldAdminSingleDateWidget,
    UnfoldAdminSingleTimeWidget,
    UnfoldAdminTextareaWidget,
    UnfoldAdminTextInputWidget,
    UnfoldBooleanSwitchWidget,
)

from .imports.schema import FISHING_MODES
from .imports.service import Referential, size_out_of_bounds
from .models import Catch, Species, Technique, Trip, WaterEntity

# Méthodes de recueil ouvertes à l'opérateur (cf. cadrage §3, option a) — la
# valeur `saisie_pecheur` est réservée à l'application pêcheur et exclue ici.
OPERATOR_COLLECTION_METHODS = [
    ("enquete", "Enquête"),
    ("carnet_volontaire", "Carnet volontaire"),
    ("carnet_obligatoire", "Carnet obligatoire"),
]


# --- Données parsées (indépendantes des formulaires, pour la validation/persist) --

@dataclass
class CaptureData:
    species_id: object
    quantity: int = 1
    size: int | None = None
    weight: int | None = None
    size_class: str | None = None
    kept: bool = False
    description: str | None = None
    technique_id: object | None = None  # repli sur la technique de la sortie si absent


@dataclass
class ManualError:
    """Erreur métier. `index` = position de la capture (0-based) ou None si la
    faute porte sur la sortie elle-même (cohérence bredouille)."""
    index: int | None
    field: str
    message: str


@dataclass
class ManualResult:
    trip_id: object
    captures: int
    errors: list = field(default_factory=list)


def validate_manual(referential, *, bredouille, captures):
    """Validation MÉTIER (DB-free hormis `referential`) d'une saisie manuelle.

    Reprend les règles de l'étage métier de l'import : cohérence bredouille,
    quantité de lot ≥ 1, taille aberrante (Q8) via le même helper partagé.
    Renvoie la liste des `ManualError` (vide si tout est valide)."""
    errors = []

    if bredouille:
        if captures:
            errors.append(ManualError(None, "bredouille",
                                      "sortie déclarée bredouille mais des captures sont renseignées"))
        return errors  # une sortie bredouille n'a, par définition, pas de capture à valider

    for idx, c in enumerate(captures):
        if c.quantity is None or c.quantity < 1:
            errors.append(ManualError(idx, "quantity",
                                      "le nombre d'individus d'un lot doit être ≥ 1"))
        if c.size is not None:
            bounds = referential.size_bounds(c.species_id)
            if size_out_of_bounds(bounds, c.size):
                lo, hi = bounds
                errors.append(ManualError(idx, "size",
                                          f"taille {c.size} cm hors bornes [{lo}-{hi}] pour l'espèce"))
    return errors


class ManualEntryService:
    """Valide (métier) puis persiste une saisie manuelle. Le `referential` est
    injectable pour les tests (même contrat que `ImportService`)."""

    def __init__(self, referential=None):
        self.ref = referential or Referential()

    def resolve_water_entity(self, *, water_entity_id="", eau_nom="", commune=""):
        """Résout l'entité hydro. Priorité à l'**id** posé par le clic-sur-carte
        (exact, sans ambiguïté d'homonyme) ; repli sur la résolution par **nom**
        du référentiel d'import si l'id est absent/invalide/inconnu. Renvoie l'id
        ou None."""
        if water_entity_id:
            try:
                uuid.UUID(str(water_entity_id))
            except (ValueError, TypeError):
                pass
            else:
                if WaterEntity.objects.filter(pk=water_entity_id).exists():
                    return str(water_entity_id)
        return self.ref.water_entity_id(eau_nom, commune)

    def validate(self, *, bredouille, captures):
        return validate_manual(self.ref, bredouille=bredouille, captures=captures)

    def create(self, *, collection_method, day, start, end, water_entity_id,
               technique_id, bredouille, captures, name=None, created_by=None):
        """Insère la sortie et ses captures dans une transaction unique. Suppose
        la validation métier déjà passée (le seul appelant, la vue admin, valide
        d'abord). Mêmes valeurs par défaut que l'import (`type`, `mode`, `source`,
        `owner_id` NULL)."""
        rows = [] if bredouille else list(captures)
        with transaction.atomic():
            now = timezone.now()
            trip = Trip.objects.create(
                collection_method=collection_method,
                day=day, start_time=start, end_time=end,
                water_entity_id=water_entity_id,
                name=name or f"Saisie {collection_method} {day:%d/%m/%Y}",
                type="Border", mode="Afterwards", source="web",
                hidden=False, owner_id=None, created_on=now,
            )
            for c in rows:
                Catch.objects.create(
                    trip=trip, species_id=c.species_id,
                    technique_id=c.technique_id or technique_id,
                    size=c.size, weight=c.weight, kept=c.kept,
                    quantity=c.quantity or 1, size_class=c.size_class or None,
                    description=c.description or None, created_on=now,
                )
        return ManualResult(trip.id, len(rows))


# --- Formulaires ------------------------------------------------------------

def _fishing_mode_choices():
    # Liste (ouverte) figée dans le schéma d'import ; présentée triée + option vide.
    return [("", "—")] + [(m, m.capitalize()) for m in sorted(FISHING_MODES)]


class ManualTripForm(forms.Form):
    """Sortie (session). La localisation est saisie par **nom** d'entité hydro et
    résolue côté serveur via le référentiel de l'import (accent-insensible) — le
    point-sur-carte est un incrément séparé (M-D)."""

    collection_method = forms.ChoiceField(
        label="Méthode de recueil", choices=OPERATOR_COLLECTION_METHODS,
        widget=UnfoldAdminSelectWidget,
        help_text="Réservé à l'opérateur — la saisie pêcheur passe par l'application mobile.",
    )
    day = forms.DateField(label="Date", widget=UnfoldAdminSingleDateWidget)
    start_time = forms.TimeField(label="Heure de début", widget=UnfoldAdminSingleTimeWidget)
    end_time = forms.TimeField(label="Heure de fin", widget=UnfoldAdminSingleTimeWidget)
    eau_nom = forms.CharField(
        label="Entité hydrographique", widget=UnfoldAdminTextInputWidget,
        help_text="Cliquez sur la carte pour sélectionner, ou saisissez le nom (résolu dans le référentiel).",
    )
    commune = forms.CharField(label="Commune (désambiguïsation)", required=False,
                              widget=UnfoldAdminTextInputWidget)
    # Rempli par le clic-sur-carte (résolution exacte par id) ; vide = repli sur le nom.
    water_entity_id = forms.CharField(required=False, widget=forms.HiddenInput)
    technique = forms.ModelChoiceField(
        label="Technique", queryset=Technique.objects.all().order_by("name"),
        widget=UnfoldAdminSelectWidget,
    )
    mode_peche = forms.ChoiceField(label="Mode de pêche", required=False,
                                   choices=_fishing_mode_choices, widget=UnfoldAdminSelectWidget)
    espece_ciblee = forms.ModelChoiceField(
        label="Espèce ciblée", required=False,
        queryset=Species.objects.all().order_by("name"), widget=UnfoldAdminSelectWidget,
    )
    bredouille = forms.BooleanField(label="Bredouille (aucune capture)", required=False,
                                    widget=UnfoldBooleanSwitchWidget)

    def __init__(self, *args, locked_method="", **kwargs):
        """`locked_method` (issu du profil opérateur, M-F) verrouille la méthode
        de recueil : la liste est réduite à cette seule valeur et le champ est
        désactivé (Django ignore alors toute valeur soumise et retient l'initiale)."""
        super().__init__(*args, **kwargs)
        # Empêche l'autofill du navigateur de remplir les champs (ex. « Jérôme »
        # injecté dans « Nombre »).
        for field in self.fields.values():
            field.widget.attrs.setdefault("autocomplete", "off")
        if locked_method:
            label = dict(OPERATOR_COLLECTION_METHODS).get(locked_method, locked_method)
            field = self.fields["collection_method"]
            field.choices = [(locked_method, label)]
            field.initial = locked_method
            field.disabled = True
            field.help_text = "Verrouillée par votre profil opérateur."

    def clean_day(self):
        day = self.cleaned_data["day"]
        if day > timezone.localdate():
            raise forms.ValidationError("la date ne peut pas être dans le futur.")
        return day

    def clean(self):
        cleaned = super().clean()
        start, end = cleaned.get("start_time"), cleaned.get("end_time")
        if start and end and end <= start:
            self.add_error("end_time", "l'heure de fin doit être postérieure à l'heure de début.")
        return cleaned


class ManualCatchForm(forms.Form):
    """Une capture (ou un lot). Masquée si la sortie est bredouille."""

    species = forms.ModelChoiceField(
        label="Espèce", queryset=Species.objects.all().order_by("name"),
        widget=UnfoldAdminSelectWidget,
    )
    quantity = forms.IntegerField(label="Nombre (lot)", min_value=1, initial=1,
                                  widget=UnfoldAdminIntegerFieldWidget)
    size = forms.IntegerField(label="Taille (cm)", required=False, min_value=0,
                              widget=UnfoldAdminIntegerFieldWidget)
    weight = forms.IntegerField(label="Poids (g)", required=False, min_value=0,
                                widget=UnfoldAdminIntegerFieldWidget)
    size_class = forms.CharField(label="Classe de taille", required=False,
                                 widget=UnfoldAdminTextInputWidget)
    kept = forms.BooleanField(label="Conservée", required=False, widget=UnfoldBooleanSwitchWidget)
    description = forms.CharField(label="Pathologies / description", required=False,
                                  widget=UnfoldAdminTextareaWidget)

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        # Coupe l'autofill du navigateur (cf. ManualTripForm).
        for field in self.fields.values():
            field.widget.attrs.setdefault("autocomplete", "off")

    def to_data(self):
        """Convertit le formulaire nettoyé en `CaptureData` (pour validation/persist)."""
        c = self.cleaned_data
        return CaptureData(
            species_id=c["species"].id,
            quantity=c.get("quantity") or 1,
            size=c.get("size"),
            weight=c.get("weight"),
            size_class=c.get("size_class") or None,
            kept=c.get("kept", False),
            description=c.get("description") or None,
        )


class BaseManualCatchFormSet(forms.BaseFormSet):
    """Ignore les lignes entièrement vides ; expose les captures saisies."""

    def clean(self):
        super().clean()
        self.captures = [
            form.to_data()
            for form in self.forms
            if form.cleaned_data and not form.cleaned_data.get("DELETE") and form.cleaned_data.get("species")
        ]


ManualCatchFormSet = forms.formset_factory(
    ManualCatchForm, formset=BaseManualCatchFormSet, extra=1, can_delete=True,
)
