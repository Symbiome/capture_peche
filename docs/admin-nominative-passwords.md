# Runbook — Mots de passe admin nominatifs (#55)

Procédure de migration des comptes **administrateurs nationaux** d'un mot de passe
**partagé** vers des mots de passe **nominatifs** (imputabilité RGPD), puis de retrait
sûr du repli partagé.

## Contexte

Historiquement, tous les admins nationaux s'authentifiaient avec un **unique mot de
passe partagé** (`fishola.admin-password` dans `application.properties`, `azerty` en
`%dev`) → aucune imputabilité : impossible de savoir *qui* a agi, alors que le journal
d'audit (`audit_log`) enregistre un `actor_id`.

Depuis #55 (PR #74), le backend :

- vérifie **d'abord le hash bcrypt propre au compte** (tous rôles, national inclus) —
  `AdminDao.verifyPassword` / `verifyBcrypt` ;
- ne garde le mot de passe partagé qu'en **repli déprécié** (`verifySharedNationalPassword`,
  log `warn` à chaque usage), **non-breaking** ;
- expose **`PUT /api/v1/admin/password`** (ancien + nouveau) pour poser/roter un mot de
  passe nominatif — l'ancien accepté peut être le mot de passe **partagé** (via le repli),
  ce qui permet la bascule sans blocage.

## Étape 1 — Provisionner chaque compte national

Pour **chaque** admin national, poser un mot de passe nominatif (l'« ancien » est le mot
de passe partagé tant qu'aucun nominatif n'a été posé). Deux voies équivalentes :

- **Back-office** : l'écran « Changer mon mot de passe »
  (`fishola-admin/src/views/account/ChangePassword.vue`, menu compte) appelle l'endpoint.
- **cURL** (une fois connecté, cookie de session admin) :

```bash
curl -X PUT https://<host>/api/v1/admin/password \
  -H "Content-Type: application/json" \
  -b "<cookie de session admin>" \
  -d '{"oldPassword":"<mot de passe partagé ou actuel>","newPassword":"<nouveau nominatif>"}'
# 204 = OK ; 400 = ancien incorrect ou nouveau non conforme à la politique
```

## Étape 2 — Vérifier la couverture

S'assurer que **tous** les comptes nationaux ont un mot de passe nominatif : dans les logs,
plus aucun `warn` « Auth admin national via mot de passe PARTAGE (deprecie, #55) » lors des
connexions. Tant qu'il en reste, **ne pas** passer à l'étape 3.

## Étape 3 — Retirer le repli partagé

**Seulement après l'étape 2** (sinon les comptes non provisionnés sont **verrouillés**).

1. Dans `AdminDao.verifyPassword`, supprimer l'appel de repli (le bloc
   `if (isNationalAdmin) { return verifySharedNationalPassword(plain); }`) et la méthode
   `verifySharedNationalPassword` — ne garder que la vérif bcrypt (`verifyBcrypt`).
2. Supprimer la config `fishola.admin-password` (`%dev.fishola.admin-password`,
   éventuels `%demo`/prod) et la méthode `FisholaConfiguration.adminPassword()`.

**Pas de backfill Flyway** : le mot de passe partagé est **spécifique à l'environnement**
(`%dev` = `azerty`) ; une migration l'injecterait en prod. Le provisioning est un acte
d'exploitation (étape 1), pas une migration de schéma.

## Rollback

Si un compte se retrouve verrouillé après l'étape 3 (provisioning incomplet), réintroduire
temporairement le repli (revert du commit de retrait) le temps de provisionner, puis rejouer
l'étape 3.

## Reste (hors ce runbook)

- *(option)* « mot de passe oublié » admin par e-mail (calqué sur le flux pêcheur,
  `SecurityResource.request-password-reset`).
- Test automatisé de l'endpoint (succès / ancien faux → 400 / nouveau non conforme → 400) :
  scénarios prêts sur la branche `feat/admin-nominative-passwords` (`AdminResourceTest`), à
  reprendre une fois le seed de recette en place (#67).
