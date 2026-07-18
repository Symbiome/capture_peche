"""
Admin « AquaAdmin » (module administrateur §3.1).

Reprend la maquette du mémoire : Utilisateurs (django-auth) / Profils & Droits
(Groups) / Historique (journal `audit_log`), + accès aux données de pêche et aux
imports. L'en-tête « AquaAdmin » est posé dans config/urls.py.

Les modèles métier sont `managed=False` : l'admin les lit (et écrit trip/catch via
l'import). L'`AuditLog` (Historique) est strictement en lecture seule.
"""
from django.contrib import admin

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


# --- Référentiels -----------------------------------------------------------

@admin.register(Species)
class SpeciesAdmin(admin.ModelAdmin):
    list_display = ("name", "export_as", "mandatory_size", "built_in")
    search_fields = ("name", "export_as")
    ordering = ("name",)


@admin.register(Technique)
class TechniqueAdmin(admin.ModelAdmin):
    list_display = ("name", "export_as", "built_in")
    search_fields = ("name", "export_as")
    ordering = ("name",)


@admin.register(WaterEntity)
class WaterEntityAdmin(admin.ModelAdmin):
    list_display = ("name", "kind", "water_entity_code")
    list_filter = ("kind",)
    search_fields = ("name", "export_as", "water_entity_code")
    ordering = ("name",)


@admin.register(Commune)
class CommuneAdmin(admin.ModelAdmin):
    list_display = ("name", "insee_com")
    search_fields = ("name", "insee_com")
    ordering = ("name",)


@admin.register(SpeciesSizeBounds)
class SpeciesSizeBoundsAdmin(admin.ModelAdmin):
    """Bornes de taille aberrante par espèce (paramétrage admin, note Q8)."""
    list_display = ("species", "min_size_cm", "max_size_cm")
    search_fields = ("species__name",)


# --- Données de pêche -------------------------------------------------------

class CatchInline(admin.TabularInline):
    model = Catch
    extra = 0
    fields = ("species", "technique", "size", "weight", "quantity", "size_class", "kept")
    autocomplete_fields = ("species", "technique")


@admin.register(Trip)
class TripAdmin(admin.ModelAdmin):
    list_display = ("name", "day", "water_entity", "collection_method", "source")
    list_filter = ("collection_method", "type", "mode")
    search_fields = ("name",)
    date_hierarchy = "day"
    inlines = (CatchInline,)


# --- Imports (§3.3, rapport) — l'action d'import arrive au palier d ---------

class ImportRowErrorInline(admin.TabularInline):
    model = ImportRowError
    extra = 0
    can_delete = False
    fields = ("line", "stage", "column_name", "code", "message")
    readonly_fields = fields

    def has_add_permission(self, request, obj=None):
        return False


@admin.register(ImportJob)
class ImportJobAdmin(admin.ModelAdmin):
    list_display = ("file_name", "status", "total", "inserted", "rejected", "created_on")
    list_filter = ("status", "collection_method")
    search_fields = ("file_name", "file_hash")
    date_hierarchy = "created_on"
    inlines = (ImportRowErrorInline,)


# --- Historique (journal d'activité, lecture seule) -------------------------

@admin.register(AuditLog)
class AuditLogAdmin(admin.ModelAdmin):
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
