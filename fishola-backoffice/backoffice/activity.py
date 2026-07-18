"""
Journal d'activité (§3.1 « Historique »).

Écrit dans la table `audit_log` PARTAGÉE avec Fishola. Deux nuances liées au
schéma existant (décisions d'intégration ouvertes, cf. README) :

- `audit_log.actor_type` a un CHECK `IN ('admin','user','system')` → on écrit
  `'admin'` pour tout le staff (opérateurs inclus) ; le rôle/identité précis va
  dans `details`.
- `audit_log.actor_id` est un uuid (ids Fishola), or l'utilisateur Django a un id
  entier → on laisse `actor_id` NULL et on met l'identité (username, groupes)
  dans `details`, tant que le mapping fishola_admin ↔ auth_user n'est pas tranché.
"""
from django.contrib.admin.models import ADDITION, CHANGE, DELETION, LogEntry
from django.db.models.signals import post_save
from django.dispatch import receiver

from .models import AuditLog

_ADMIN_FLAG_TO_ACTION = {ADDITION: "create", CHANGE: "update", DELETION: "delete"}


def log_activity(user, action, entity_type=None, entity_id=None, **extra):
    """Écrit une ligne de journal pour une action d'un utilisateur interne."""
    details = {}
    if user is not None:
        details["user"] = getattr(user, "get_username", lambda: str(user))()
        details["groups"] = list(user.groups.values_list("name", flat=True)) if user.pk else []
    details.update(extra)
    AuditLog.objects.create(
        actor_type="admin",
        actor_id=None,
        action=action,
        entity_type=entity_type,
        entity_id=entity_id,
        details=details,
    )


@receiver(post_save, sender=LogEntry, dispatch_uid="backoffice.mirror_admin_action")
def mirror_admin_action(sender, instance, created, **kwargs):
    """Reflète chaque action de l'admin Django dans le journal partagé `audit_log`."""
    if not created:
        return
    verb = _ADMIN_FLAG_TO_ACTION.get(instance.action_flag, "action")
    entity_type = instance.content_type.model if instance.content_type_id else None
    log_activity(
        instance.user,
        f"admin.{verb}",
        entity_type=entity_type,
        object_id=str(instance.object_id) if instance.object_id else None,
        object=instance.object_repr,
    )
