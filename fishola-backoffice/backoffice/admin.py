"""
Admin « AquaAdmin » (module administrateur), thémé avec django-unfold.

Organisation : Utilisateurs (badge de profil coloré, statut, dernière connexion) /
Profils & Droits (Groups) / Historique (journal `audit_log`, lecture seule) + accès
aux données de pêche et aux imports. Branding dans settings (`UNFOLD`) et config/urls.py.
"""
from pathlib import Path

from django.contrib import admin
from django.contrib.auth.admin import GroupAdmin as BaseGroupAdmin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin
from django.contrib.auth.models import Group, User
from django import forms
from django.contrib import messages
from django.db import connection
from django.http import HttpResponse
from django.shortcuts import redirect
from django.template.response import TemplateResponse
from django.urls import reverse
from django.utils.html import format_html
from unfold.admin import ModelAdmin, TabularInline
from unfold.decorators import action, display
from unfold.forms import AdminPasswordChangeForm, UserChangeForm, UserCreationForm
from unfold.widgets import UnfoldAdminFileFieldWidget, UnfoldAdminSelectWidget

from . import maps
from .activity import log_activity
from .imports.schema import EXPECTED_HEADER
from .imports.service import ImportService
from .manual_entry import ManualCatchFormSet, ManualEntryService, ManualTripForm
from .permissions import GroupAdminForm
from .models import (
    AuditLog,
    Catch,
    Commune,
    ImportJob,
    ImportRowError,
    Species,
    SpeciesSizeBounds,
    Technique,
    Trip,
    WaterEntity,
)

# Libellés & couleurs de profil (Administrateur rouge / Opérateur vert).
_ROLE_LABEL = {
    "operator": "Opérateur",
    "admin_regional": "Admin régional",
    "admin_national": "Admin national",
}
_PROFIL_COLORS = {
    "Opérateur": "success",
    "Admin régional": "warning",
    "Admin national": "danger",
    "Super-administrateur": "danger",
    "—": "info",
}


# --- Socle : clef primaire verrouillée + modèles en lecture seule -----------

class AquaModelAdmin(ModelAdmin):
    """Base des admins sur données partagées Fishola.

    La clef primaire (UUID) n'est **jamais** modifiable : on la fige en lecture
    seule à l'édition d'un objet existant (et à l'ajout quand elle est auto-générée).
    Éviter de pouvoir réécrire un identifiant est critique (intégrité, FK, journal).
    """

    def get_readonly_fields(self, request, obj=None):
        ro = list(super().get_readonly_fields(request, obj))
        pk = self.model._meta.pk
        if (obj is not None or pk.has_default()) and pk.name not in ro:
            ro.append(pk.name)
        return ro


class ReadOnlyModelAdmin(AquaModelAdmin):
    """Consultation seule : ni ajout, ni modification, ni suppression.

    Pour les données opérationnelles (sorties/captures des pêcheurs) et les
    référentiels géographiques provisionnés (entités hydro, communes), qui ne
    doivent pas être édités à la main depuis le back-office.
    """

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False


# --- Utilisateurs & Profils (django-auth, restylés unfold) ------------------

admin.site.unregister(User)
admin.site.unregister(Group)


@admin.register(User)
class UserAdmin(BaseUserAdmin, ModelAdmin):
    form = UserChangeForm
    add_form = UserCreationForm
    change_password_form = AdminPasswordChangeForm
    list_display = ("username", "email", "profil", "statut", "last_login")

    @display(description="Profil", label=_PROFIL_COLORS)
    def profil(self, obj):
        if obj.is_superuser and not obj.groups.exists():
            return "Super-administrateur"
        group = obj.groups.first()
        return _ROLE_LABEL.get(group.name, group.name) if group else "—"

    @display(description="Statut", label={"Actif": "success", "Inactif": "danger"})
    def statut(self, obj):
        return "Actif" if obj.is_active else "Inactif"


@admin.register(Group)
class GroupAdmin(BaseGroupAdmin, ModelAdmin):
    form = GroupAdminForm
    filter_horizontal = ()  # remplacé par la matrice de droits (PermissionMatrixWidget)


# --- Référentiels -----------------------------------------------------------

# Référentiels gérés par l'admin (espèces, techniques, seuils) : ajout/édition
# possibles, mais la clef primaire reste verrouillée (AquaModelAdmin).

@admin.register(Species)
class SpeciesAdmin(AquaModelAdmin):
    list_display = ("name", "export_as", "mandatory_size", "built_in")
    search_fields = ("name", "export_as")
    ordering = ("name",)


@admin.register(Technique)
class TechniqueAdmin(AquaModelAdmin):
    list_display = ("name", "export_as", "built_in")
    search_fields = ("name", "export_as")
    ordering = ("name",)


@admin.register(SpeciesSizeBounds)
class SpeciesSizeBoundsAdmin(AquaModelAdmin):
    """Bornes de taille aberrante par espèce (paramétrage admin, note Q8)."""
    list_display = ("species", "min_size_cm", "max_size_cm")
    search_fields = ("species__name",)


# Référentiels géographiques provisionnés : consultation seule.

@admin.register(WaterEntity)
class WaterEntityAdmin(ReadOnlyModelAdmin):
    list_display = ("name", "kind", "water_entity_code")
    list_filter = ("kind",)
    search_fields = ("name", "export_as", "water_entity_code")
    ordering = ("name",)


@admin.register(Commune)
class CommuneAdmin(ReadOnlyModelAdmin):
    list_display = ("name", "insee_com")
    search_fields = ("name", "insee_com")
    ordering = ("name",)


# --- Données de pêche (sorties / captures) : consultation seule -------------
# Écrites par l'application pêcheur et par l'import CSV — pas d'édition manuelle
# (surtout pas des identifiants / propriétaire / horodatage) depuis le back-office.

class CatchInline(TabularInline):
    model = Catch
    extra = 0
    fields = ("species", "technique", "size", "weight", "quantity", "size_class", "kept", "photo")
    readonly_fields = fields
    can_delete = False

    def has_add_permission(self, request, obj=None):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    @display(description="Photo")
    def photo(self, obj):
        """Vignette de la première photo si la capture en a une (sinon « — »)."""
        if not obj or not obj.pk:
            return "—"
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT min(picture_index) FROM catch_picture WHERE catch_id = %s", [str(obj.pk)])
            row = cursor.fetchone()
        if not row or row[0] is None:
            return "—"
        url = reverse("catch_photo", args=[obj.pk, row[0]])
        return format_html(
            '<a href="{0}" target="_blank" rel="noopener">'
            '<img src="{0}" alt="photo" style="height:40px;border-radius:4px"></a>', url)


@admin.register(Trip)
class TripAdmin(ReadOnlyModelAdmin):
    """Consultation des sorties + **saisie manuelle opérateur** (#62) via un
    chemin de création dédié (bouton « Nouvelle saisie »). L'édition directe des
    sorties reste interdite (données écrites par l'appli pêcheur / l'import)."""

    list_display = ("name", "day", "water_entity", "collection_method", "source")
    list_filter = ("collection_method", "type", "mode")
    search_fields = ("name",)
    date_hierarchy = "day"
    inlines = (CatchInline,)
    actions_list = ("saisie_manuelle",)

    @action(description="Nouvelle saisie", url_path="saisie-manuelle", icon="add_circle",
            permissions=["backoffice.add_trip"])
    def saisie_manuelle(self, request):
        """Écran « Nouvelle saisie » : une sortie + ses captures, avec les mêmes
        contrôles que l'import (référentiel + métier). N'écrit jamais via l'admin
        brut de Trip (lecture seule) : passe par ManualEntryService."""
        service = ManualEntryService()
        if request.method == "POST":
            form = ManualTripForm(request.POST)
            formset = ManualCatchFormSet(request.POST)
            if form.is_valid() and formset.is_valid():
                data = form.cleaned_data
                bredouille = data["bredouille"]
                # `formset.captures` = uniquement les lignes réellement renseignées.
                captures = formset.captures

                # Référentiel : id posé par le clic-sur-carte (exact) sinon repli
                # sur la résolution par nom (comme l'import).
                water_entity_id = service.resolve_water_entity(
                    water_entity_id=data.get("water_entity_id", ""),
                    eau_nom=data["eau_nom"], commune=data.get("commune", ""))
                if water_entity_id is None:
                    form.add_error("eau_nom", "entité hydrographique non résolue dans le référentiel.")

                # Métier : bredouille cohérente, lots ≥ 1, tailles aberrantes (Q8).
                for err in service.validate(bredouille=bredouille, captures=captures):
                    if err.index is None:
                        form.add_error(err.field, err.message)
                    else:
                        formset.forms[err.index].add_error(err.field, err.message)

                if form.is_valid() and formset.is_valid():
                    result = service.create(
                        collection_method=data["collection_method"],
                        day=data["day"], start=data["start_time"], end=data["end_time"],
                        water_entity_id=water_entity_id, technique_id=data["technique"].id,
                        bredouille=bredouille, captures=captures,
                        created_by=getattr(request.user, "pk", None),
                    )
                    log_activity(request.user, "trip.create", entity_type="trip",
                                 entity_id=result.trip_id, captures=result.captures,
                                 collection_method=data["collection_method"], source="saisie_manuelle")
                    messages.success(
                        request,
                        f"Sortie enregistrée — {result.captures} capture(s).")
                    return redirect(reverse("admin:backoffice_trip_change", args=[result.trip_id]))
        else:
            form = ManualTripForm()
            formset = ManualCatchFormSet()

        context = {
            **self.admin_site.each_context(request),
            "title": "Nouvelle saisie",
            "form": form,
            "formset": formset,
            "opts": self.model._meta,
            # Carte de sélection de l'entité hydro (mêmes tuiles MVT que la carte
            # back-office) — clic-pour-sélectionner (M-D).
            "hydro_tiles_url": maps._hydro_tiles_url(),
        }
        return TemplateResponse(request, "admin/backoffice/manual_entry.html", context)


# --- Imports (saisie opérateur, rapport) ------------------------------------

class ImportRowErrorInline(TabularInline):
    model = ImportRowError
    extra = 0
    can_delete = False
    fields = ("line", "stage", "column_name", "code", "message")
    readonly_fields = fields

    def has_add_permission(self, request, obj=None):
        return False


class CsvImportForm(forms.Form):
    fichier = forms.FileField(
        label="Fichier CSV",
        widget=UnfoldAdminFileFieldWidget,
    )
    mode = forms.ChoiceField(
        label="Mode d'import",
        initial="partial",
        widget=UnfoldAdminSelectWidget,
        choices=[
            ("partial", "Partiel — insère les sessions valides, liste les rejets"),
            ("all_or_nothing", "Tout ou rien — n'insère rien si une erreur"),
        ],
    )


# Modèle CSV officiel embarqué (téléchargeable depuis l'écran d'import).
_TEMPLATE_PATH = Path(__file__).resolve().parent / "imports" / "template-import-sessions.csv"

# Colonnes du modèle regroupées pour l'aide à l'écran (les colonnes obligatoires
# sont marquées `req`). Les lignes d'une même sortie partagent le même `session_ref`.
IMPORT_COLUMN_GROUPS = [
    ("Sortie", "Une ligne par capture ; les captures d'une même sortie partagent le même « session_ref ».", [
        ("session_ref", "Identifiant de la sortie (regroupe ses captures)", True),
        ("collection_method", "Origine : saisie_pecheur, enquete, carnet_volontaire, carnet_obligatoire", True),
        ("date", "JJ/MM/AAAA", True),
        ("heure_debut", "HH:MM", True),
        ("heure_fin", "HH:MM (> heure_debut)", True),
        ("eau_nom", "Nom du plan / cours d'eau (référentiel)", True),
        ("commune", "Commune (lève l'ambiguïté sur l'eau)", True),
        ("mode_peche", "bateau, float tube, kayak, à pied…", False),
        ("technique", "Technique (référentiel)", False),
        ("nb_lignes", "Nombre de lignes en action", False),
        ("espece_ciblee", "Espèce visée (référentiel)", False),
        ("bredouille", "oui/non — si oui, aucune capture attendue", False),
    ]),
    ("Enquêté", "Profil anonyme (enquêtes / carnets) — laisser vide pour une saisie pêcheur. "
                "Colonnes acceptées mais pas encore enregistrées (à venir).", [
        ("enquete_age", "", False),
        ("enquete_sexe", "", False),
        ("enquete_commune", "", False),
        ("enquete_experience_annees", "", False),
        ("enquete_importance", "principale / secondaire", False),
        ("enquete_membre_club", "oui/non", False),
        ("enquete_sorties_par_an", "", False),
    ]),
    ("Capture", "Une ligne par poisson (ou par lot). Vide si sortie bredouille.", [
        ("capture_espece", "Espèce (référentiel)", True),
        ("capture_longueur_cm", "Longueur en cm (contrôle des tailles aberrantes)", False),
        ("capture_poids_g", "Poids en grammes", False),
        ("capture_conservation", "oui/non", True),
        ("capture_nombre", "Nombre d'individus (lot) — défaut 1", False),
        ("capture_classe_taille", "Classe de taille pour un lot (ex. 10-15)", False),
        ("capture_prelevement", "oui/non", False),
        ("capture_marque", "écaille, génétique…", False),
        ("capture_pathologies", "Observations", False),
    ]),
]

# Valeurs acceptées pour les colonnes codées (aide à la préparation du fichier).
IMPORT_CODED_VALUES = [
    ("collection_method", ["saisie_pecheur", "enquete", "carnet_volontaire", "carnet_obligatoire"]),
    ("mode_peche", ["bateau", "float tube", "kayak", "à pied", "belly boat", "du bord", "rive"]),
    ("enquete_importance", ["principale", "secondaire"]),
    ("bredouille · capture_conservation · capture_prelevement · enquete_membre_club", ["oui", "non"]),
]


@admin.register(ImportJob)
class ImportJobAdmin(AquaModelAdmin):
    """Consultation des imports + lancement via le bouton « Importer un CSV ».
    Un import n'est jamais saisi à la main (pas d'ajout/édition d'objet)."""
    list_display = ("file_name", "statut", "total", "inserted", "rejected", "created_on")
    list_filter = ("status", "collection_method")
    search_fields = ("file_name", "file_hash")
    date_hierarchy = "created_on"
    inlines = (ImportRowErrorInline,)
    readonly_fields = ("file_name", "file_hash", "collection_method", "status",
                       "total", "inserted", "rejected", "created_by", "created_on")
    actions_list = ("importer_csv", "telecharger_modele")

    @display(description="Statut", label={
        "DONE": "success", "DONE_WITH_ERRORS": "warning", "FAILED": "danger", "PENDING": "info"})
    def statut(self, obj):
        return obj.status

    def has_add_permission(self, request):
        return False

    @action(description="Importer un CSV", url_path="importer-csv", icon="upload_file",
            permissions=["backoffice.add_importjob"])
    def importer_csv(self, request):
        if request.method == "POST":
            form = CsvImportForm(request.POST, request.FILES)
            if form.is_valid():
                fichier = form.cleaned_data["fichier"]
                result = ImportService().run(
                    fichier.read(), filename=fichier.name, mode=form.cleaned_data["mode"],
                )
                if result.duplicate:
                    messages.warning(request, "Ce fichier a déjà été importé — import ignoré (idempotence).")
                else:
                    log_activity(request.user, "csv.import", entity_type="import_job",
                                 entity_id=result.import_id, status=result.status,
                                 inserted=result.inserted, rejected=result.rejected)
                    level = messages.SUCCESS if result.status == "DONE" else messages.WARNING
                    messages.add_message(
                        request, level,
                        f"Import {result.status} : {result.inserted} session(s) insérée(s), "
                        f"{result.rejected} rejetée(s).")
                return redirect(reverse("admin:backoffice_importjob_change", args=[result.import_id]))
        else:
            form = CsvImportForm()
        context = {
            **self.admin_site.each_context(request),
            "title": "Importer un CSV",
            "form": form,
            "opts": self.model._meta,
            "groupes_colonnes": IMPORT_COLUMN_GROUPS,
            "valeurs_codees": IMPORT_CODED_VALUES,
            "nb_colonnes": len(EXPECTED_HEADER),
        }
        return TemplateResponse(request, "admin/backoffice/import_csv.html", context)

    @action(description="Télécharger le modèle", url_path="telecharger-modele", icon="download")
    def telecharger_modele(self, request):
        """Sert le modèle CSV officiel (en-tête + exemples) prêt à remplir."""
        contenu = _TEMPLATE_PATH.read_bytes()
        response = HttpResponse(contenu, content_type="text/csv; charset=utf-8")
        response["Content-Disposition"] = 'attachment; filename="modele-import-sessions.csv"'
        return response


# --- Historique (journal d'activité, lecture seule) -------------------------

@admin.register(AuditLog)
class AuditLogAdmin(AquaModelAdmin):
    list_display = ("at", "actor_type", "action", "entity_type", "entity_id")
    list_filter = ("actor_type", "action")
    search_fields = ("action", "entity_type")
    date_hierarchy = "at"
    ordering = ("-at",)

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False
