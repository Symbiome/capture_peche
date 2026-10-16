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

- Installer pip & ansible sur le serveur sans droit sudo.
bash```
curl -o get-pip.py https://bootstrap.pypa.io/get-pip.py
python3 -m venv ~/.venvs/ansible
bash .venvs/ansible/bin/python3 get-pip.py
bash .venvs/ansible/bin/pip install ansible-core
echo 'export PATH="$HOME/.venvs/ansible/bin:$PATH"' >> ~/.bashrc && source ~/.bashrc
```

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

Le vault doit aussi contenir `vault_pgbackrest_cipher_pass`, la phrase de
chiffrement des sauvegardes : sans elle, le playbook s'arrête dès sa première
étape (voir §5 pour la créer).

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
4. build les images Docker (`docker compose build`), dont celle de
   `docker_pg` (PostGIS + pgBackRest, `docker/postgres/Dockerfile`) ;
5. démarre la stack et attend qu'elle soit prête (`docker compose up -d --wait`) ;
6. crée la stanza pgBackRest au premier déploiement, puis vérifie le dépôt et
   l'archivage des WAL (`pgbackrest check`) ;
7. installe les deux tâches cron de sauvegarde dans la crontab de
   l'utilisateur (voir §5).

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

Dans les deux modes, le script vérifie aussi les sauvegardes : dernière
sauvegarde de moins de 26 h et archivage des WAL sans échec (voir §5).

## 5. Sauvegardes (pgBackRest)

### Principe

- Les WAL sont archivés **en continu** dans le dépôt pgBackRest : la base peut
  être restaurée à n'importe quel instant couvert par les sauvegardes.
- Sauvegarde **complète le dimanche à 3 h**, **différentielle du lundi au
  samedi à 3 h** (heure du serveur), lancées par la crontab de l'utilisateur
  qui déploie (`crontab -l`, entrées `#Ansible: fishola - sauvegarde
  pgBackRest …`). Journal : `~/fishola-pgbackrest.log`.
- Dépôt chiffré (AES-256) dans le volume Docker `fishola_pgbackrest`, sur le
  même serveur que la base (voir « À arbitrer »).
- Rétention (`docker/postgres/pgbackrest.conf`) : complètes conservées
  180 jours, 26 différentielles (environ un mois).
- pgBackRest est installé dans `docker_pg` (version épinglée dans
  `docker/postgres/Dockerfile`) : toutes les commandes passent par
  `docker compose exec -T -u postgres docker_pg …`. `-u postgres` est
  obligatoire (`exec` tourne en root par défaut).

### Phrase de chiffrement (avant le premier déploiement)

```bash
openssl rand -base64 48
cd ansible
ansible-vault edit group_vars/dev/vault.yml
# ajouter : vault_pgbackrest_cipher_pass: "<phrase générée>"
```

Enregistrer aussi cette phrase dans le KeePass de l'équipe, **hors du
serveur** : sans elle, les sauvegardes sont illisibles, y compris pour
restaurer après la perte du serveur. Ne pas la changer sur un dépôt existant :
les sauvegardes déjà faites restent chiffrées avec l'ancienne.

### Premier déploiement

- `docker_pg` est recréé (nouvelle image, `archive_mode` qui ne s'active qu'au
  redémarrage) : courte coupure de la base et du backend, à planifier en
  production.
- Le playbook crée la stanza `fishola`, remet à zéro les compteurs
  d'archivage (les tentatives d'avant la stanza ont échoué) puis lance
  `pgbackrest check`.
- Aucune sauvegarde n'existe avant la première nuit : `health_check.sh`
  signale `ÉCHEC Sauvegarde` jusque-là. Pour en faire une tout de suite :

```bash
docker compose exec -T -u postgres docker_pg pgbackrest --stanza=fishola --type=full backup
```

### Consulter

```bash
docker compose exec -T -u postgres docker_pg pgbackrest --stanza=fishola info
tail -n 50 ~/fishola-pgbackrest.log
```

### Restaurer

La base doit être **arrêtée**. La restauration se lance dans un conteneur
ponctuel de la même image (mêmes volumes, même phrase de chiffrement) :

```bash
docker compose stop fishola-backend docker_pg

# a) Dernier état connu (dernière sauvegarde + tous les WAL archivés)
docker compose run --rm --no-deps -u postgres --entrypoint pgbackrest docker_pg \
  --stanza=fishola --delta restore

# b) ou état à un instant donné (ex. juste avant une suppression accidentelle)
docker compose run --rm --no-deps -u postgres --entrypoint pgbackrest docker_pg \
  --stanza=fishola --delta --type=time --target="2026-10-16 14:00:00+02" \
  --target-action=promote restore

docker compose up -d --wait
docker compose exec -T -u postgres docker_pg pgbackrest --stanza=fishola check
```

Au redémarrage, PostgreSQL rejoue les WAL jusqu'à la cible puis repart sur
une nouvelle timeline ; l'archivage et les sauvegardes suivantes continuent
normalement. `--delta` ne réécrit que les fichiers qui diffèrent.
L'avertissement `unknown group 'root' in backup manifest` est sans
conséquence.

Procédure validée sur une copie locale de la stack (restauration à un instant
donné annulant un `DROP TABLE`) ; **à rejouer sur la recette** avant la mise
en production.

### En cas d'échec

- Diagnostic : `docker compose logs --tail 100 docker_pg` (erreurs
  d'`archive-push`), `pgbackrest --stanza=fishola check`, journal cron.
- **Un archivage bloqué accumule les WAL dans `pg_wal` jusqu'à remplir le
  disque et arrêter la base** : à traiter sans attendre (disque plein, droits
  sur le volume, phrase de chiffrement absente du `.env`…).
- `failed_count` ne redescend pas tout seul : une fois la cause corrigée et
  `pgbackrest check` revenu au vert, le remettre à zéro :

```bash
docker compose exec -T -u postgres docker_pg psql -c "SELECT pg_stat_reset_shared('archiver')"
```

### À arbitrer avec Adventiel (Lot 2)

- **Copie hors serveur** : le dépôt est sur la même machine que la base, il
  ne protège donc pas d'une perte du serveur ou du disque. Options : stockage
  S3 en second dépôt (`repo2-type=s3` dans `pgbackrest.conf`), ou serveur
  pgBackRest distant (conteneur `woblerr/pgbackrest` en mode TLS).
- Rétention définitive.
- Canal d'alerte : `health_check.sh` n'est lancé par aucune tâche planifiée,
  à brancher sur leur supervision.
- Fenêtre de redémarrage de `docker_pg` en production.

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
- Sauvegardes (§5) : pas encore de copie hors serveur ni d'alerte
  automatique en cas d'échec, à arbitrer avec Adventiel.
