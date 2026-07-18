"""
Modèles du backend « gestion interne ».

Toutes les tables métier appartiennent à la BDD Fishola PARTAGÉE, dont le schéma
est géré par Flyway côté Fishola. Elles sont donc mappées en **managed = False** :
Django les lit et les écrit via l'ORM, mais ne les migre jamais. Seules les tables
framework de Django (auth, admin, sessions, contenttypes) sont gérées par Django.

On ne mappe que les colonnes utiles aux modules admin (§3.1) et saisie opérateur
(§3.3). Les colonnes géométriques (PostGIS) et générées sont volontairement omises
(pas de dépendance GeoDjango/GDAL : l'import résout la localisation en
`water_entity_id`). Chaque champ porte un `verbose_name` FR (libellés de l'admin).
"""
import uuid

from django.db import models
from django.utils import timezone


# --- Référentiels (lecture seule pour la résolution d'import) ---------------

class Species(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    name = models.TextField("Nom")
    export_as = models.TextField("Libellé d'export")
    built_in = models.BooleanField("Intégré au socle", default=False)
    mandatory_size = models.BooleanField("Taille obligatoire", default=True)

    class Meta:
        managed = False
        db_table = "species"
        verbose_name = "Espèce"
        verbose_name_plural = "Espèces"

    def __str__(self):
        return self.name


class Technique(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    name = models.TextField("Nom")
    export_as = models.TextField("Libellé d'export")
    built_in = models.BooleanField("Intégré au socle", default=False)

    class Meta:
        managed = False
        db_table = "technique"
        verbose_name = "Technique"
        verbose_name_plural = "Techniques"

    def __str__(self):
        return self.name


class WaterEntity(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    name = models.TextField("Nom")
    export_as = models.TextField("Libellé d'export")
    water_entity_code = models.CharField("Code entité", max_length=10, null=True, blank=True)
    kind = models.CharField("Type", max_length=16, default="STILL")
    nature = models.TextField("Nature", null=True, blank=True)

    class Meta:
        managed = False
        db_table = "water_entity"
        verbose_name = "Entité hydrographique"
        verbose_name_plural = "Entités hydrographiques"

    def __str__(self):
        return self.name


class Commune(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    insee_com = models.CharField("Code INSEE", max_length=5, unique=True)
    name = models.TextField("Nom")

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
    created_on = models.DateTimeField("Créée le", default=timezone.now)
    owner_id = models.UUIDField("Propriétaire (id)", null=True, blank=True)
    mode = models.CharField("Mode", max_length=32)
    name = models.TextField("Nom")
    day = models.DateField("Date")
    start_time = models.TimeField("Heure de début")
    end_time = models.TimeField("Heure de fin")
    type = models.CharField("Type", max_length=32)
    water_entity = models.ForeignKey(
        WaterEntity, on_delete=models.DO_NOTHING, db_column="water_entity_id",
        verbose_name="Entité hydrographique",
    )
    source = models.CharField("Canal de saisie", max_length=32)
    hidden = models.BooleanField("Masquée", default=False)
    # Méthode de collecte métier (import CSV #12), distincte du canal `source`.
    collection_method = models.CharField("Méthode de collecte", max_length=32, default="saisie_pecheur")

    class Meta:
        managed = False
        db_table = "trip"
        verbose_name = "Sortie"
        verbose_name_plural = "Sorties"

    def __str__(self):
        return f"{self.name} — {self.day}"


class Catch(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4)
    created_on = models.DateTimeField("Créée le", default=timezone.now)
    trip = models.ForeignKey(
        Trip, on_delete=models.DO_NOTHING, db_column="trip_id", related_name="catches",
        verbose_name="Sortie",
    )
    catch_time = models.TimeField("Heure de capture", null=True, blank=True)
    species = models.ForeignKey(Species, on_delete=models.DO_NOTHING, db_column="species_id",
                                verbose_name="Espèce")
    technique = models.ForeignKey(Technique, on_delete=models.DO_NOTHING, db_column="technique_id",
                                  verbose_name="Technique")
    size = models.IntegerField("Taille (cm)", null=True, blank=True)
    weight = models.IntegerField("Poids (g)", null=True, blank=True)
    kept = models.BooleanField("Conservée")
    # Lots (import #12) : nombre d'individus + classe de taille.
    quantity = models.IntegerField("Nombre (lot)", default=1)
    size_class = models.CharField("Classe de taille", max_length=32, null=True, blank=True)
    description = models.TextField("Description / pathologies", null=True, blank=True)

    class Meta:
        managed = False
        db_table = "catch"
        verbose_name = "Capture"
        verbose_name_plural = "Captures"


# --- Bornes de taille aberrante (stade métier de l'import) ------------------

class SpeciesSizeBounds(models.Model):
    species = models.OneToOneField(
        Species, on_delete=models.DO_NOTHING, db_column="species_id", primary_key=True,
        verbose_name="Espèce",
    )
    min_size_cm = models.IntegerField("Taille min (cm)", null=True, blank=True)
    max_size_cm = models.IntegerField("Taille max (cm)", null=True, blank=True)

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
    file_name = models.TextField("Fichier", null=True, blank=True)
    file_hash = models.TextField("Empreinte (SHA-256)", unique=True)
    collection_method = models.CharField("Méthode de collecte", max_length=32, null=True, blank=True)
    status = models.TextField("Statut")
    total = models.IntegerField("Total", default=0)
    inserted = models.IntegerField("Insérées", default=0)
    rejected = models.IntegerField("Rejetées", default=0)
    created_by = models.UUIDField("Lancé par (id)", null=True, blank=True)
    created_on = models.DateTimeField("Lancé le", default=timezone.now)

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
        ImportJob, on_delete=models.DO_NOTHING, db_column="import_id", related_name="errors",
        verbose_name="Import",
    )
    line = models.IntegerField("Ligne")
    column_name = models.TextField("Colonne", null=True, blank=True)
    stage = models.TextField("Étage")
    code = models.TextField("Code")
    message = models.TextField("Message", null=True, blank=True)

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
    actor_type = models.CharField("Type d'acteur", max_length=16)
    actor_id = models.UUIDField("Acteur (id)", null=True, blank=True)
    action = models.CharField("Action", max_length=64)
    entity_type = models.CharField("Type d'entité", max_length=64, null=True, blank=True)
    entity_id = models.UUIDField("Entité (id)", null=True, blank=True)
    at = models.DateTimeField("Horodatage", default=timezone.now)
    details = models.JSONField("Détails", default=dict)

    class Meta:
        managed = False
        db_table = "audit_log"
        verbose_name = "Entrée de journal"
        verbose_name_plural = "Journal d'activité"

    def __str__(self):
        return f"{self.at:%Y-%m-%d %H:%M} · {self.actor_type} · {self.action}"
