"""
Modèles du backend « gestion interne ».

Toutes les tables métier appartiennent à la BDD Fishola PARTAGÉE, dont le schéma
est géré par Flyway côté Fishola. Elles sont donc mappées en **managed = False** :
Django les lit et les écrit via l'ORM, mais ne les migre jamais. Seules les tables
framework de Django (auth, admin, sessions, contenttypes) sont gérées par Django.

On ne mappe que les colonnes utiles aux modules admin (§3.1) et saisie opérateur
(§3.3). Les colonnes géométriques (PostGIS) et générées sont volontairement omises
(pas de dépendance GeoDjango/GDAL : l'import résout la localisation en
`water_entity_id`).
"""
import uuid

from django.db import models
from django.utils import timezone


# --- Référentiels (lecture seule pour la résolution d'import) ---------------

class Species(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    name = models.TextField()
    export_as = models.TextField()
    built_in = models.BooleanField(default=False)
    mandatory_size = models.BooleanField(default=True)

    class Meta:
        managed = False
        db_table = "species"
        verbose_name = "Espèce"
        verbose_name_plural = "Espèces"

    def __str__(self):
        return self.name


class Technique(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    name = models.TextField()
    export_as = models.TextField()
    built_in = models.BooleanField(default=False)

    class Meta:
        managed = False
        db_table = "technique"
        verbose_name = "Technique"
        verbose_name_plural = "Techniques"

    def __str__(self):
        return self.name


class WaterEntity(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    name = models.TextField()
    export_as = models.TextField()
    water_entity_code = models.CharField(max_length=10, null=True, blank=True)
    kind = models.CharField(max_length=16, default="STILL")
    nature = models.TextField(null=True, blank=True)

    class Meta:
        managed = False
        db_table = "water_entity"
        verbose_name = "Entité hydrographique"
        verbose_name_plural = "Entités hydrographiques"

    def __str__(self):
        return self.name


class Commune(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    insee_com = models.CharField(max_length=5, unique=True)
    name = models.TextField()

    class Meta:
        managed = False
        db_table = "commune"
        verbose_name = "Commune"
        verbose_name_plural = "Communes"

    def __str__(self):
        return f"{self.name} ({self.insee_com})"


# --- Données de pêche (écrites par l'import opérateur) ----------------------

class Trip(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    created_on = models.DateTimeField(default=timezone.now)
    owner_id = models.UUIDField(null=True, blank=True)
    mode = models.CharField(max_length=32)
    name = models.TextField()
    day = models.DateField()
    start_time = models.TimeField()
    end_time = models.TimeField()
    type = models.CharField(max_length=32)
    water_entity = models.ForeignKey(
        WaterEntity, on_delete=models.DO_NOTHING, db_column="water_entity_id"
    )
    source = models.CharField(max_length=32)
    hidden = models.BooleanField(default=False)
    # Méthode de collecte métier (import CSV #12), distincte du canal `source`.
    collection_method = models.CharField(max_length=32, default="saisie_pecheur")

    class Meta:
        managed = False
        db_table = "trip"
        verbose_name = "Sortie"
        verbose_name_plural = "Sorties"

    def __str__(self):
        return f"{self.name} — {self.day}"


class Catch(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    created_on = models.DateTimeField(default=timezone.now)
    trip = models.ForeignKey(
        Trip, on_delete=models.DO_NOTHING, db_column="trip_id", related_name="catches"
    )
    catch_time = models.TimeField(null=True, blank=True)
    species = models.ForeignKey(Species, on_delete=models.DO_NOTHING, db_column="species_id")
    technique = models.ForeignKey(Technique, on_delete=models.DO_NOTHING, db_column="technique_id")
    size = models.IntegerField(null=True, blank=True)
    weight = models.IntegerField(null=True, blank=True)
    kept = models.BooleanField()
    # Lots (import #12) : nombre d'individus + classe de taille.
    quantity = models.IntegerField(default=1)
    size_class = models.CharField(max_length=32, null=True, blank=True)
    description = models.TextField(null=True, blank=True)

    class Meta:
        managed = False
        db_table = "catch"
        verbose_name = "Capture"
        verbose_name_plural = "Captures"


# --- Bornes de taille aberrante (stade métier de l'import) ------------------

class SpeciesSizeBounds(models.Model):
    species = models.OneToOneField(
        Species, on_delete=models.DO_NOTHING, db_column="species_id", primary_key=True
    )
    min_size_cm = models.IntegerField(null=True, blank=True)
    max_size_cm = models.IntegerField(null=True, blank=True)

    class Meta:
        managed = False
        db_table = "species_size_bounds"
        verbose_name = "Bornes de taille par espèce"
        verbose_name_plural = "Bornes de taille par espèce"

    def __str__(self):
        return f"{self.species_id} [{self.min_size_cm}-{self.max_size_cm}] cm"


# --- Suivi des imports (idempotence + rapport d'erreurs) --------------------

class ImportJob(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    file_name = models.TextField(null=True, blank=True)
    file_hash = models.TextField(unique=True)
    collection_method = models.CharField(max_length=32, null=True, blank=True)
    status = models.TextField()
    total = models.IntegerField(default=0)
    inserted = models.IntegerField(default=0)
    rejected = models.IntegerField(default=0)
    created_by = models.UUIDField(null=True, blank=True)
    created_on = models.DateTimeField(default=timezone.now)

    class Meta:
        managed = False
        db_table = "import_job"
        verbose_name = "Import"
        verbose_name_plural = "Imports"

    def __str__(self):
        return f"{self.file_name or self.file_hash[:12]} — {self.status}"


class ImportRowError(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    import_job = models.ForeignKey(
        ImportJob, on_delete=models.DO_NOTHING, db_column="import_id", related_name="errors"
    )
    line = models.IntegerField()
    column_name = models.TextField(null=True, blank=True)
    stage = models.TextField()
    code = models.TextField()
    message = models.TextField(null=True, blank=True)

    class Meta:
        managed = False
        db_table = "import_row_error"
        verbose_name = "Erreur d'import"
        verbose_name_plural = "Erreurs d'import"


# --- Journal d'activité PARTAGÉ (§3.1 « Historique ») -----------------------

class AuditLog(models.Model):
    """Journal partagé avec Fishola. Django y écrit les actions admin/opérateur
    (actor_type='admin' — le CHECK côté DB n'autorise que admin|user|system, le
    rôle précis va dans `details`). Fishola y écrit les actions pêcheur."""

    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    actor_type = models.CharField(max_length=16)
    actor_id = models.UUIDField(null=True, blank=True)
    action = models.CharField(max_length=64)
    entity_type = models.CharField(max_length=64, null=True, blank=True)
    entity_id = models.UUIDField(null=True, blank=True)
    at = models.DateTimeField(default=timezone.now)
    details = models.JSONField(default=dict)

    class Meta:
        managed = False
        db_table = "audit_log"
        verbose_name = "Entrée de journal"
        verbose_name_plural = "Journal d'activité"

    def __str__(self):
        return f"{self.at:%Y-%m-%d %H:%M} · {self.actor_type} · {self.action}"
