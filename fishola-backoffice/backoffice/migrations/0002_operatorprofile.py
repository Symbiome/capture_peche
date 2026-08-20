import django.db.models.deletion
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ("backoffice", "0001_initial"),
    ]

    operations = [
        migrations.CreateModel(
            name="OperatorProfile",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("default_collection_method", models.CharField(
                    blank=True,
                    choices=[("enquete", "Enquête"), ("carnet_volontaire", "Carnet volontaire"),
                             ("carnet_obligatoire", "Carnet obligatoire")],
                    help_text="Verrouille la méthode à la saisie manuelle. Vide = choix libre (liste restreinte).",
                    max_length=32, verbose_name="Méthode de recueil par défaut")),
                ("user", models.OneToOneField(
                    on_delete=django.db.models.deletion.CASCADE, related_name="operator_profile",
                    to=settings.AUTH_USER_MODEL, verbose_name="Compte")),
            ],
            options={
                "verbose_name": "Profil opérateur",
                "verbose_name_plural": "Profils opérateur",
            },
        ),
    ]
