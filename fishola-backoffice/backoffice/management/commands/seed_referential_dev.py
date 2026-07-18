"""Charge un jeu de référentiel minimal (espèces, techniques, météo, états de
relâche) dans la base partagée — pour le **développement / la recette**.

Les migrations sont *schema-only* : sur une base fraîche, les menus de saisie
sont vides, ce qui bloque la création d'une sortie et la résolution des imports.
Ce script pose un jeu réaliste (eau douce, lacs alpins). Il est **idempotent**
(n'insère que si la table cible est vide) et ne remplace pas le référentiel
officiel de l'UFBRMC.

    python manage.py seed_referential_dev
"""
from pathlib import Path

from django.core.management.base import BaseCommand
from django.db import connection

SQL_PATH = Path(__file__).resolve().parent.parent.parent / "seeds" / "referential_dev.sql"


class Command(BaseCommand):
    help = "Charge un référentiel de démonstration (dev/recette) dans la base partagée."

    def handle(self, *args, **options):
        sql = SQL_PATH.read_text(encoding="utf-8")
        with connection.cursor() as cursor:
            cursor.execute(sql)
        # Compte rendu (les tables peuvent déjà avoir été peuplées → idempotent).
        with connection.cursor() as cursor:
            counts = {}
            for table in ("species", "technique", "weather", "released_fish_state"):
                cursor.execute(f"select count(*) from public.{table}")
                counts[table] = cursor.fetchone()[0]
        resume = ", ".join(f"{t}={n}" for t, n in counts.items())
        self.stdout.write(self.style.SUCCESS(f"Référentiel de démonstration en place ({resume})."))
