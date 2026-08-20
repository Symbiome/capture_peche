"""
Service d'import CSV opérateur (#32).

Pipeline en 3 étages :
  1. structurel  — colonnes, types, cohérence intra-ligne (avant toute base) ;
  2. référentiel — résolution espèce / technique / entité hydro (nom + commune) ;
  3. métier      — bornes de taille aberrante, lots, bredouille.

Les sorties (`trip`) sont regroupées par `session_ref` ; chaque ligne de capture
devient un `catch`. Suivi dans `import_job` (idempotence par empreinte SHA-256) et
`import_row_error` (rapport ligne à ligne).

Dépendances à venir (non persistées ici) : colonnes `enquete_*` (profil enquêté →
table `surveyed_angler`, I-F) ; `mode_peche` / `nb_lignes` (enum `fishing_mode` /
`rod_count`, I-G) sont validés mais pas stockés. `espece_ciblee` est validée mais
son rattachement (`trip_expected_species`) n'est pas encore posé.
"""
from __future__ import annotations

import hashlib
from dataclasses import dataclass, field

from django.db import transaction
from django.utils import timezone

from backoffice.models import (
    Catch, ImportJob, ImportRowError, Species, SpeciesSizeBounds, Technique, Trip, WaterEntity,
)
from . import schema
from .parsing import is_blank, parse_bool_ouinon, parse_date, parse_int, parse_time, read_rows


def size_out_of_bounds(bounds, size):
    """Règle métier Q8 PARTAGÉE (import CSV + saisie manuelle) : une taille est
    aberrante si elle sort des bornes `(min, max)` de l'espèce. Une borne `None`
    (non renseignée) ne contraint pas ce côté ; `bounds` ou `size` absents → pas
    d'aberration. Un seul point de vérité pour les deux chemins de saisie."""
    if bounds is None or size is None:
        return False
    lo, hi = bounds
    return (lo is not None and size < lo) or (hi is not None and size > hi)


@dataclass
class RowError:
    line: int
    column: str | None
    stage: str
    code: str
    message: str


@dataclass
class ImportResult:
    import_id: object
    status: str
    total: int
    inserted: int
    rejected: int
    errors: list = field(default_factory=list)
    duplicate: bool = False


class Referential:
    """Résout les libellés du CSV vers des identifiants (via l'ORM). Surchargeable en test."""

    def species_id(self, name):
        s = (Species.objects.filter(name__unaccent__iexact=name).first()
             or Species.objects.filter(export_as__unaccent__iexact=name).first())
        return s.id if s else None

    def technique_id(self, name):
        t = (Technique.objects.filter(name__unaccent__iexact=name).first()
             or Technique.objects.filter(export_as__unaccent__iexact=name).first())
        if t is None and name:
            # Repli : le libellé court du template (« Leurre ») est contenu dans le
            # nom du référentiel (« Pêche aux leurres »). Accepté si non ambigu.
            matches = list(Technique.objects.filter(name__unaccent__icontains=name)[:2])
            t = matches[0] if len(matches) == 1 else None
        return t.id if t else None

    def water_entity_id(self, eau_nom, commune):
        # Résolution par nom (insensible aux accents) ; la désambiguïsation spatiale
        # par commune (homonymes) est un raffinement (pas de GeoDjango ici) — cf. #32.
        w = (WaterEntity.objects.filter(name__unaccent__iexact=eau_nom).first()
             or WaterEntity.objects.filter(export_as__unaccent__iexact=eau_nom).first())
        return w.id if w else None

    def size_bounds(self, species_id):
        b = SpeciesSizeBounds.objects.filter(species_id=species_id).first()
        return (b.min_size_cm, b.max_size_cm) if b else None


class ImportService:
    def __init__(self, referential=None):
        self.ref = referential or Referential()

    # -- validation d'une ligne : renvoie (errors, parsed) -------------------
    def _validate_row(self, line, rec):
        errors = []
        parsed = {}

        # --- Étage 1 : structurel ---
        cm = rec.get("collection_method", "")
        if cm not in schema.COLLECTION_METHODS:
            errors.append(RowError(line, "collection_method", schema.STRUCTUREL,
                                   schema.STRUCT_COLLECTION_METHOD,
                                   f"méthode de collecte inconnue : « {cm} »"))
        parsed["collection_method"] = cm

        try:
            parsed["day"] = parse_date(rec.get("date", ""))
        except ValueError:
            errors.append(RowError(line, "date", schema.STRUCTUREL, schema.STRUCT_DATE,
                                   f"date invalide (JJ/MM/AAAA) : « {rec.get('date', '')} »"))

        times_ok = True
        for col, key in (("heure_debut", "start"), ("heure_fin", "end")):
            try:
                parsed[key] = parse_time(rec.get(col, ""))
            except ValueError:
                times_ok = False
                errors.append(RowError(line, col, schema.STRUCTUREL, schema.STRUCT_TIME,
                                       f"heure invalide (HH:MM) : « {rec.get(col, '')} »"))
        if times_ok and parsed["end"] <= parsed["start"]:
            errors.append(RowError(line, "heure_fin", schema.STRUCTUREL, schema.STRUCT_TIME_ORDER,
                                   "l'heure de fin doit être postérieure à l'heure de début"))

        mode = rec.get("mode_peche", "")
        if not is_blank(mode) and mode.lower() not in schema.FISHING_MODES:
            errors.append(RowError(line, "mode_peche", schema.STRUCTUREL, schema.STRUCT_MODE,
                                   f"mode de pêche inconnu : « {mode} »"))

        try:
            bredouille = parse_bool_ouinon(rec.get("bredouille", "non"))
        except ValueError:
            bredouille = False
            errors.append(RowError(line, "bredouille", schema.STRUCTUREL, schema.STRUCT_BREDOUILLE,
                                   "valeur oui/non attendue"))
        parsed["bredouille"] = bredouille

        if errors:
            return errors, None  # on n'enchaîne pas les étages suivants

        has_capture = not bredouille and not is_blank(rec.get("capture_espece", ""))
        parsed["has_capture"] = has_capture

        # --- Étage 2 : référentiel ---
        parsed["water_entity_id"] = self.ref.water_entity_id(rec.get("eau_nom", ""), rec.get("commune", ""))
        if parsed["water_entity_id"] is None:
            errors.append(RowError(line, "eau_nom", schema.REFERENTIEL, schema.REF_WATER_ENTITY,
                                   f"entité hydrographique non résolue : « {rec.get('eau_nom', '')} » "
                                   f"/ commune « {rec.get('commune', '')} »"))

        parsed["technique_id"] = self.ref.technique_id(rec.get("technique", ""))
        if parsed["technique_id"] is None:
            errors.append(RowError(line, "technique", schema.REFERENTIEL, schema.REF_TECHNIQUE,
                                   f"technique non résolue : « {rec.get('technique', '')} »"))

        parsed["species_id"] = None
        if has_capture:
            parsed["species_id"] = self.ref.species_id(rec.get("capture_espece", ""))
            if parsed["species_id"] is None:
                errors.append(RowError(line, "capture_espece", schema.REFERENTIEL, schema.REF_SPECIES,
                                       f"espèce non résolue : « {rec.get('capture_espece', '')} »"))

        if errors:
            return errors, None

        # --- Étage 3 : métier ---
        if has_capture:
            quantity = 1
            raw_nb = rec.get("capture_nombre", "")
            if not is_blank(raw_nb):
                try:
                    quantity = parse_int(raw_nb)
                except ValueError:
                    quantity = 0
            if quantity < 1:
                errors.append(RowError(line, "capture_nombre", schema.METIER, schema.METIER_QUANTITY,
                                       "le nombre d'individus d'un lot doit être ≥ 1"))
            parsed["quantity"] = quantity

            longueur = None
            if not is_blank(rec.get("capture_longueur_cm", "")):
                try:
                    longueur = parse_int(rec["capture_longueur_cm"])
                except ValueError:
                    errors.append(RowError(line, "capture_longueur_cm", schema.METIER,
                                           schema.METIER_SIZE_ABERRANT, "longueur non numérique"))
            if longueur is not None:
                bounds = self.ref.size_bounds(parsed["species_id"])
                if size_out_of_bounds(bounds, longueur):
                    lo, hi = bounds
                    errors.append(RowError(line, "capture_longueur_cm", schema.METIER,
                                           schema.METIER_SIZE_ABERRANT,
                                           f"taille {longueur} cm hors bornes [{lo}-{hi}] pour l'espèce"))
            parsed["longueur"] = longueur

            weight = None
            if not is_blank(rec.get("capture_poids_g", "")):
                try:
                    weight = parse_int(rec["capture_poids_g"])
                except ValueError:
                    weight = None
            parsed["weight"] = weight

            try:
                parsed["kept"] = parse_bool_ouinon(rec.get("capture_conservation", "non"))
            except ValueError:
                parsed["kept"] = False
            parsed["size_class"] = rec.get("capture_classe_taille", "") or None
            parsed["description"] = rec.get("capture_pathologies", "") or None
        elif bredouille and not is_blank(rec.get("capture_espece", "")):
            errors.append(RowError(line, "bredouille", schema.METIER, schema.METIER_BREDOUILLE,
                                   "sortie déclarée bredouille mais une espèce capturée est renseignée"))

        if errors:
            return errors, None
        return [], parsed

    # -- validation de tout le fichier (pure vis-à-vis de la base sauf self.ref) --
    def validate_all(self, records, start_line=1):
        all_errors = []
        sessions = {}
        line = start_line
        for rec in records:
            line += 1
            errs, parsed = self._validate_row(line, rec)
            if errs:
                all_errors.extend(errs)
                continue
            sref = rec.get("session_ref", "") or f"L{line}"
            grp = sessions.setdefault(sref, {"session": parsed, "records": []})
            grp["records"].append((line, rec, parsed))
        return all_errors, sessions

    # -- exécution complète : parse + validation + persistance + suivi -------
    def run(self, file_bytes, filename, mode="partial", created_by=None):
        file_hash = hashlib.sha256(file_bytes).hexdigest()
        existing = ImportJob.objects.filter(file_hash=file_hash).first()
        if existing:
            return ImportResult(existing.id, existing.status, existing.total,
                                existing.inserted, existing.rejected, duplicate=True)

        header, records = read_rows(file_bytes.decode("utf-8-sig"))
        total = len(records)

        # Étage 0 : en-tête conforme
        if header != schema.EXPECTED_HEADER:
            job = ImportJob.objects.create(file_name=filename, file_hash=file_hash,
                                           status="FAILED", total=total)
            err = RowError(1, None, schema.STRUCTUREL, schema.STRUCT_HEADER,
                           "en-tête non conforme au modèle attendu (28 colonnes)")
            ImportRowError.objects.create(import_job=job, line=err.line, column_name=err.column,
                                          stage=err.stage, code=err.code, message=err.message)
            return ImportResult(job.id, "FAILED", total, 0, 0, errors=[err])

        all_errors, sessions = self.validate_all(records)
        rejected = len({e.line for e in all_errors})
        do_insert = not (mode == "all_or_nothing" and all_errors)
        status = "DONE" if not all_errors else "DONE_WITH_ERRORS"

        inserted = 0
        with transaction.atomic():
            job = ImportJob.objects.create(
                file_name=filename, file_hash=file_hash, status=status,
                total=total, rejected=rejected, created_by=created_by,
            )
            for e in all_errors:
                ImportRowError.objects.create(import_job=job, line=e.line, column_name=e.column,
                                              stage=e.stage, code=e.code, message=e.message)
            if do_insert:
                inserted = self._persist(sessions)
                job.inserted = inserted
                job.save(update_fields=["inserted"])

        return ImportResult(job.id, status, total, inserted, rejected, errors=all_errors)

    def _persist(self, sessions):
        inserted = 0
        for sref, grp in sessions.items():
            s = grp["session"]
            now = timezone.now()
            trip = Trip.objects.create(
                collection_method=s["collection_method"],
                day=s["day"], start_time=s["start"], end_time=s["end"],
                water_entity_id=s["water_entity_id"],
                name=f"Import {sref} {s['day']:%d/%m/%Y}",
                type="Border", mode="Afterwards", source="web",
                hidden=False, owner_id=None, created_on=now,
            )
            for (line, rec, p) in grp["records"]:
                if not p.get("has_capture"):
                    continue
                Catch.objects.create(
                    trip=trip, species_id=p["species_id"], technique_id=s["technique_id"],
                    size=p.get("longueur"), weight=p.get("weight"), kept=p.get("kept", False),
                    quantity=p.get("quantity", 1), size_class=p.get("size_class"),
                    description=p.get("description"), created_on=now,
                )
            inserted += 1
        return inserted
