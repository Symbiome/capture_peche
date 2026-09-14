# Déploiement — serveur dev/recette

Déploie l'ensemble de la stack Fishola / capture_peche (Postgres+PostGIS,
backend Quarkus, front pêcheur, front admin, Maildev) sur le serveur
dev/recette, via Ansible. Le back-office Django n'est pas déployé ici.

Le serveur cible a déjà Docker + le plugin `docker compose` installés. Le
reverse proxy et le mapping domaine -> port sont gérés par **Adventiel** :
ce playbook ne touche à aucun proxy, il build les images et démarre la stack
sur les ports que vous aurez alignés avec leur mapping (voir §2).

## Prérequis

- Être connecté en SSH sur le serveur cible (ce playbook tourne **en local**
  sur ce serveur, pas depuis votre laptop — pas de saut SSH à configurer).
- `ansible-core` installé sur le serveur (`sudo apt install ansible-core` ou
  `pipx install ansible-core`).
- Ce dépôt cloné sur le serveur.

## 1. Récupérer le mot de passe du vault

Les secrets (mots de passe DB, clé JWT, clé Django...) sont dans
`ansible/group_vars/dev/vault.yml`, chiffré avec `ansible-vault` — sûr à
committer tel quel. Seul le mot de passe qui le déchiffre,
`ansible/.vault_pass`, n'est jamais commité (gitignoré) : récupérez-le dans le
gestionnaire de mots de passe de l'équipe (KeePass) et déposez-le à
`ansible/.vault_pass` sur le serveur (permissions `600`).

```bash
chmod 600 ansible/.vault_pass
```

## 2. Vérifier domaines et ports

Éditer `ansible/group_vars/dev/vars.yml` : `app_domain`, `api_domain`,
`admin_domain` et les 3 ports (`backend_port`, `mobile_port`, `admin_port`)
doivent correspondre **exactement** au mapping domaine -> port configuré par
Adventiel sur leur reverse proxy. À confirmer avec eux avant le premier
déploiement.

Pour changer un secret (rotation de mot de passe, etc.) :

```bash
cd ansible
ansible-vault edit group_vars/dev/vault.yml
```

## 3. Lancer le déploiement

```bash
cd ansible
ansible-playbook playbooks/deploy.yml
```

Le playbook :
1. génère `.env` à partir de `group_vars/dev/vars.yml` +
   `group_vars/dev/vault.yml` (jamais édité à la main, régénéré à chaque run) ;
2. régénère les overrides Vite (`fishola-mobile/.env.demo.local`,
   `fishola-admin/.env.demo.local`) avec l'URL d'API publique ;
3. build le backend + les 2 fronts (`mvn clean package`, dans un conteneur
   `maven:3.9-eclipse-temurin-25` — pas besoin de Java/Node sur le serveur) ;
4. build les 3 images Docker (`docker compose build`) ;
5. démarre la stack (`docker compose up -d`).

Rejouable sans risque. Pour forcer un rebuild complet (sans cache Docker ni
Maven) :

```bash
ansible-playbook playbooks/deploy.yml --extra-vars "force_rebuild=true"
```

Les services écoutent sur `127.0.0.1:<port>` uniquement. Si le reverse proxy
d'Adventiel tourne sur une **autre** machine que ce serveur (et non en local
dessus), changer le binding `127.0.0.1:` en `0.0.0.0:` dans
`docker-compose.yml` pour les services concernés, et restreindre l'accès à ces
ports par pare-feu à l'IP de leur proxy.

## 4. Vérifier

```bash
./health_check.sh --local   # vérifie via 127.0.0.1:<port>, sans dépendre du proxy
./health_check.sh           # vérifie via les vrais domaines (une fois le proxy Adventiel actif)
```

## Variables d'environnement

Toute la configuration vit dans `ansible/group_vars/dev/vars.yml` (non
secret) et `ansible/group_vars/dev/vault.yml` (secret, chiffré) — plus de
`.env` à copier/éditer à la main. Les propriétés backend au-delà de celles
déjà couvertes par `docker-compose.yml` (liste complète dans
`doc/MEP-initiale.md`) restent surchargeables via variable d'environnement
Quarkus standard (`fishola.xxx-yyy` → `FISHOLA_XXX_YYY`), à ajouter dans le
service `fishola-backend` de `docker-compose.yml` et dans
`ansible/templates/dotenv.j2` si besoin.

## Limites actuelles

- Superset et Dagster (visés par l'architecture cible, cf. `CLAUDE.md` §2.2)
  ne sont pas encore implémentés dans ce dépôt : cette stack ne couvre que
  l'existant (celui que `start_all.sh` lance en local).
- Pas de sauvegarde automatique de la base incluse ici (cf. `CLAUDE.md` §5.7,
  à mettre en place séparément — `postgres-backup-local` par exemple).
