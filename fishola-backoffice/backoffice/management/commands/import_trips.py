"""Importe un fichier CSV de sorties/captures opérateur (#32).

    python manage.py import_trips <fichier.csv> [--mode partial|all_or_nothing]
"""
import os

from django.core.management.base import BaseCommand, CommandError

from backoffice.imports.service import ImportService


class Command(BaseCommand):
    help = "Importe un CSV de sorties/captures opérateur (pipeline 3 étages)."

    def add_arguments(self, parser):
        parser.add_argument("csv_path", help="Chemin du fichier CSV (séparateur « ; », UTF-8).")
        parser.add_argument("--mode", choices=["partial", "all_or_nothing"], default="partial",
                            help="partial : insère les sessions valides ; all_or_nothing : tout ou rien.")

    def handle(self, *args, **options):
        path = options["csv_path"]
        if not os.path.exists(path):
            raise CommandError(f"Fichier introuvable : {path}")
        with open(path, "rb") as fh:
            data = fh.read()

        result = ImportService().run(data, filename=os.path.basename(path), mode=options["mode"])

        if result.duplicate:
            self.stdout.write(self.style.WARNING(
                f"Fichier déjà importé (import {result.import_id}) — ignoré (idempotence)."))
            return

        style = self.style.SUCCESS if result.status == "DONE" else self.style.WARNING
        self.stdout.write(style(
            f"Import {result.status} — total={result.total} insérés={result.inserted} rejetés={result.rejected}"))
        for e in result.errors[:100]:
            self.stdout.write(f"  L{e.line} [{e.stage}/{e.code}] {e.column or '-'} : {e.message}")
