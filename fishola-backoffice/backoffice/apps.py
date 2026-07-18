from django.apps import AppConfig


class BackofficeConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "backoffice"
    verbose_name = "Gestion interne (admin & opérateurs)"

    def ready(self):
        # Branche le signal qui reflète les actions de l'admin dans `audit_log`.
        from . import activity  # noqa: F401
