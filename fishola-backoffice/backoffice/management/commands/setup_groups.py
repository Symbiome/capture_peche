"""
Crée/met à jour les profils staff (Groups django-auth) et leurs droits.

Matrice de droits best-effort par profil (à affiner avec l'exploitant).
À lancer APRÈS `migrate` (les permissions Django doivent exister).

    python manage.py setup_groups

Le périmètre par entité hydro (scope régional) n'est PAS porté par les permissions
Django — il devra être appliqué au niveau des vues/querysets (à cadrer, cf. #56).
"""
from django.contrib.auth.models import Group, Permission
from django.core.management.base import BaseCommand

GESTION = "backoffice"

REFERENTIELS = ["species", "technique", "waterentity", "commune", "speciessizebounds"]
DONNEES = ["trip", "catch"]
IMPORT = ["importjob", "importrowerror"]


def _perms(app, models, actions):
    return [(app, f"{a}_{m}") for m in models for a in actions]


# Profil -> liste de (app_label, codename), ou "ALL" pour l'admin national.
ROLE_PERMS = {
    # Opérateur : saisie (enquêtes/carnets) + import, référentiels en lecture,
    # consultation du journal. Pas de suppression, pas de gestion de comptes.
    "operator": (
        _perms(GESTION, REFERENTIELS, ["view"])
        + _perms(GESTION, DONNEES, ["add", "change", "view"])
        + _perms(GESTION, IMPORT, ["add", "view"])
        + [(GESTION, "view_auditlog")]
    ),
    # Admin régional : opérateur + suppression données + gestion des comptes
    # (le scope régional n'est pas porté par les perms Django, cf. entête).
    "admin_regional": (
        _perms(GESTION, REFERENTIELS, ["view", "change"])
        + _perms(GESTION, DONNEES, ["add", "change", "delete", "view"])
        + _perms(GESTION, IMPORT, ["add", "view"])
        + [(GESTION, "view_auditlog")]
        + _perms("auth", ["user"], ["add", "change", "view"])
    ),
    # Admin national : tous les droits (superutilisateur applicatif).
    "admin_national": "ALL",
}


class Command(BaseCommand):
    help = "Crée/met à jour les profils staff (operator, admin_regional, admin_national) et leurs droits."

    def handle(self, *args, **options):
        for role, spec in ROLE_PERMS.items():
            group, created = Group.objects.get_or_create(name=role)
            state = "créé" if created else "mis à jour"

            if spec == "ALL":
                qs = Permission.objects.filter(content_type__app_label__in=[GESTION, "auth"])
                group.permissions.set(qs)
                self.stdout.write(self.style.SUCCESS(
                    f"[{role}] {state} — {qs.count()} permissions (tous droits backoffice + auth)"))
                continue

            wanted, missing = [], []
            for app_label, codename in spec:
                perm = (Permission.objects
                        .filter(content_type__app_label=app_label, codename=codename)
                        .first())
                if perm is not None:
                    wanted.append(perm)
                else:
                    missing.append(f"{app_label}.{codename}")

            group.permissions.set(wanted)
            self.stdout.write(self.style.SUCCESS(f"[{role}] {state} — {len(wanted)} permissions"))
            if missing:
                self.stdout.write(self.style.WARNING(
                    f"    permissions introuvables (as-tu lancé `migrate` ?) : {', '.join(missing)}"))
