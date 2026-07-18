"""
Admin « AquaAdmin » (module administrateur §3.1), thémé avec django-unfold.

Reprend la maquette du mémoire : Utilisateurs (badge de profil coloré, statut,
dernière connexion) / Profils & Droits (Groups) / Historique (journal `audit_log`,
lecture seule) + accès aux données de pêche et aux imports. Branding dans settings
(`UNFOLD`) et config/urls.py.
"""
from django.contrib import admin
from django.contrib.auth.admin import GroupAdmin as BaseGroupAdmin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin
from django.contrib.auth.models import Group, User
from django import forms
from django.contrib import messages
from django.shortcuts import redirect
from django.template.response import TemplateResponse
from django.urls import reverse
from unfold.admin import ModelAdmin, TabularInline
from unfold.decorators import action, display
from unfold.forms import AdminPasswordChangeForm, UserChangeForm, UserCreationForm

from .activity import log_activity
from .imports.service import ImportService
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

# Libellés & couleurs de profil (maquette : Administrateur rouge / Opérateur vert).
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

@admin.register(Species)
class SpeciesAdmin(ModelAdmin):
    list_display = ("name", "export_as", "mandatory_size", "built_in")
    search_fields = ("name", "export_as")
    ordering = ("name",)


@admin.register(Technique)
class TechniqueAdmin(ModelAdmin):
    list_display = ("name", "export_as", "built_in")
    search_fields = ("name", "export_as")
    ordering = ("name",)


@admin.register(WaterEntity)
class WaterEntityAdmin(ModelAdmin):
    list_display = ("name", "kind", "water_entity_code")
    list_filter = ("kind",)
    search_fields = ("name", "export_as", "water_entity_code")
    ordering = ("name",)


@admin.register(Commune)
class CommuneAdmin(ModelAdmin):
    list_display = ("name", "insee_com")
    search_fields = ("name", "insee_com")
    ordering = ("name",)


@admin.register(SpeciesSizeBounds)
class SpeciesSizeBoundsAdmin(ModelAdmin):
    """Bornes de taille aberrante par espèce (paramétrage admin, note Q8)."""
    list_display = ("species", "min_size_cm", "max_size_cm")
    search_fields = ("species__name",)


# --- Données de pêche -------------------------------------------------------

class CatchInline(TabularInline):
    model = Catch
    extra = 0
    fields = ("species", "technique", "size", "weight", "quantity", "size_class", "kept")
    autocomplete_fields = ("species", "technique")


@admin.register(Trip)
class TripAdmin(ModelAdmin):
    list_display = ("name", "day", "water_entity", "collection_method", "source")
    list_filter = ("collection_method", "type", "mode")
    search_fields = ("name",)
    date_hierarchy = "day"
    inlines = (CatchInline,)


# --- Imports (§3.3, rapport) ------------------------------------------------

class ImportRowErrorInline(TabularInline):
    model = ImportRowError
    extra = 0
    can_delete = False
    fields = ("line", "stage", "column_name", "code", "message")
    readonly_fields = fields

    def has_add_permission(self, request, obj=None):
        return False


class CsvImportForm(forms.Form):
    fichier = forms.FileField(label="Fichier CSV")
    mode = forms.ChoiceField(
        label="Mode",
        initial="partial",
        choices=[
            ("partial", "Partiel — insère les sessions valides, liste les rejets"),
            ("all_or_nothing", "Tout ou rien — n'insère rien si une erreur"),
        ],
    )


@admin.register(ImportJob)
class ImportJobAdmin(ModelAdmin):
    """Consultation des imports + lancement via le bouton « Importer un CSV ».
    Un import n'est jamais saisi à la main (pas d'ajout/édition d'objet)."""
    list_display = ("file_name", "statut", "total", "inserted", "rejected", "created_on")
    list_filter = ("status", "collection_method")
    search_fields = ("file_name", "file_hash")
    date_hierarchy = "created_on"
    inlines = (ImportRowErrorInline,)
    readonly_fields = ("file_name", "file_hash", "collection_method", "status",
                       "total", "inserted", "rejected", "created_by", "created_on")
    actions_list = ("importer_csv",)

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
        }
        return TemplateResponse(request, "admin/backoffice/import_csv.html", context)


# --- Historique (journal d'activité, lecture seule) -------------------------

@admin.register(AuditLog)
class AuditLogAdmin(ModelAdmin):
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
