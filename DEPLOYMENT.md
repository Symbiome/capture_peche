# Déploiement — serveurs de recette et de production

Déploie l'ensemble de la stack Fishola / capture_peche (Postgres+PostGIS,
backend Quarkus, front pêcheur, front admin, Maildev en recette) via
Ansible. Le back-office Django n'est pas déployé ici.

Deux environnements, même code, même `docker-compose.yml`, mêmes templates :
seules les variables Ansible changent (`ansible/inventories/<env>/`).

| | Recette | Production |
|---|---|---|
| Branche déployée | `develop` | `main` |
| Inventaire | `-i inventories/recette` | `-i inventories/production` |
| Mot de passe du vault | `ansible/.vault_pass_recette` (`--vault-id recette@…`) | `ansible/.vault_pass_prod` (`--vault-id prod@…`) |
| Domaines | `*-recette.carnet-peche.fr` | `peche`, `api-peche`, `admin-peche`, `dashboard-peche` `.carnet-peche.fr` |
| Mention dans l'app (`VITE__ENV_NAME`) | `recette` | aucune |
| Mails | Maildev (profil compose `recette`), aucun mail réel | vrai SMTP, mot de passe dans le vault |
| Secrets (DB, JWT, admin, pgBackRest) | propres à la recette | propres à la production |

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

Les secrets (mots de passe DB, clé JWT, mot de passe admin, phrase
pgBackRest, SMTP en production) sont dans
`ansible/inventories/<env>/group_vars/all/vault.yml`, chiffré avec
`ansible-vault` — sûr à committer tel quel. Chaque environnement a **son
propre mot de passe de vault** : avoir accès à la recette ne donne pas accès
aux secrets de production. Ces mots de passe ne sont jamais commités
(gitignorés) : récupérez celui de l'environnement dans le gestionnaire de
mots de passe de l'équipe (KeePass) et déposez-le sur le serveur
(permissions `600`) :

```bash
# serveur de recette
chmod 600 ansible/.vault_pass_recette
# serveur de production
chmod 600 ansible/.vault_pass_prod
```

Migration du serveur de recette (avant #282, un seul mot de passe
`ansible/.vault_pass`) : le renommer, son contenu ne change pas.

```bash
mv ansible/.vault_pass ansible/.vault_pass_recette
```

Le vault doit aussi contenir `vault_pgbackrest_cipher_pass`, la phrase de
chiffrement des sauvegardes : sans elle, le playbook s'arrête dès sa première
étape (voir §6 pour la créer).

## 2. Vérifier domaines, ports et mail

Éditer `ansible/inventories/<env>/group_vars/all/vars.yml` : `app_domain`,
`api_domain`, `admin_domain` et les 3 ports (`backend_port`, `mobile_port`,
`admin_port`) doivent correspondre **exactement** au mapping domaine -> port
configuré par Adventiel sur leur reverse proxy. À confirmer avec eux avant
le premier déploiement.

En production, renseigner aussi le SMTP (`smtp_host`, `smtp_port`,
`smtp_username`, `smtp_from`, `feedback_mail_to` dans `vars.yml`,
`vault_smtp_password` dans `vault.yml`). Tant qu'une valeur vaut `A_DEFINIR`,
le playbook s'arrête avant toute action en la nommant.

Pour changer un secret (rotation de mot de passe, etc.) :

```bash
cd ansible
ansible-vault edit --vault-id recette@.vault_pass_recette inventories/recette/group_vars/all/vault.yml
ansible-vault edit --vault-id prod@.vault_pass_prod inventories/production/group_vars/all/vault.yml
```

Ne jamais copier un secret d'un environnement à l'autre : un secret JWT
commun rendrait un jeton de recette valable en production.

## 3. Mettre à jour le code

Le playbook déploie le code présent sur le serveur, il ne fait ni checkout ni
pull (il modifierait le dépôt d'où il s'exécute). Avant chaque déploiement,
extraire et mettre à jour la branche de l'environnement :

```bash
git checkout develop && git pull   # serveur de recette
git checkout main && git pull      # serveur de production
```

Si une autre branche est extraite, le playbook s'arrête dès sa deuxième
étape, avant toute action (ex. `develop` sur le serveur de production).

## 4. Lancer le déploiement

```bash
cd ansible
# serveur de recette
ansible-playbook -i inventories/recette --vault-id recette@.vault_pass_recette playbooks/deploy.yml
# serveur de production
ansible-playbook -i inventories/production --vault-id prod@.vault_pass_prod playbooks/deploy.yml
```

`ansible.cfg` ne définit ni inventaire ni mot de passe de vault par défaut :
sans `-i`, aucun hôte ne correspond et rien n'est déployé.

Le playbook :
1. vérifie l'inventaire, la branche extraite et l'absence de valeur
   `A_DEFINIR`, et s'arrête sinon ;
2. génère `.env` à partir de `inventories/<env>/group_vars/all/vars.yml` +
   `vault.yml` (jamais édité à la main, régénéré à chaque run), dont
   `COMPOSE_PROFILES` (`recette` démarre Maildev, vide en production) ;
3. régénère les overrides Vite (`fishola-mobile/.env.demo.local`,
   `fishola-admin/.env.demo.local`) avec l'URL d'API publique et la mention
   d'environnement. Vite inscrit ces valeurs dans le JavaScript compilé :
   chaque serveur compile sa propre version, une image de recette ne peut pas
   être promue telle quelle en production ;
4. build le backend + les 2 fronts (`mvn clean package`, dans un conteneur
   `maven:3.9-eclipse-temurin-25` — pas besoin de Java/Node sur le serveur) ;
5. build les images Docker (`docker compose build`), dont celle de
   `docker_pg` (PostGIS + pgBackRest, `docker/postgres/Dockerfile`) ;
6. démarre la stack et attend qu'elle soit prête (`docker compose up -d --wait`) ;
7. crée la stanza pgBackRest au premier déploiement, puis vérifie le dépôt et
   l'archivage des WAL (`pgbackrest check`) ;
8. installe les deux tâches cron de sauvegarde dans la crontab de
   l'utilisateur (voir §6).

Rejouable sans risque. Pour forcer un rebuild complet (sans cache Docker ni
Maven) :

```bash
ansible-playbook -i inventories/recette --vault-id recette@.vault_pass_recette \
  playbooks/deploy.yml --extra-vars "force_rebuild=true"
```

Les services écoutent sur `127.0.0.1:<port>` uniquement. Si le reverse proxy
d'Adventiel tourne sur une **autre** machine que ce serveur (et non en local
dessus), changer le binding `127.0.0.1:` en `0.0.0.0:` dans
`docker-compose.yml` pour les services concernés, et restreindre l'accès à ces
ports par pare-feu à l'IP de leur proxy.

## 5. Vérifier

```bash
./health_check.sh --local   # vérifie via 127.0.0.1:<port>, sans dépendre du proxy
./health_check.sh           # vérifie via les vrais domaines (une fois le proxy Adventiel actif)
```

Dans les deux modes, le script vérifie aussi les sauvegardes : dernière
sauvegarde de moins de 26 h et archivage des WAL sans échec (voir §6).
Maildev n'est vérifié qu'en recette (`--local`).

## 6. Sauvegardes (pgBackRest)

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
ansible-vault edit --vault-id recette@.vault_pass_recette inventories/recette/group_vars/all/vault.yml
# ajouter : vault_pgbackrest_cipher_pass: "<phrase générée>"
```

La production a déjà sa propre phrase, générée avec ses autres secrets (#282).

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

Toute la configuration vit dans `ansible/inventories/<env>/group_vars/all/vars.yml`
(non secret) et `vault.yml` (secret, chiffré) — plus de `.env` à
copier/éditer à la main. Les propriétés backend au-delà de celles
déjà couvertes par `docker-compose.yml` (liste complète dans
`doc/MEP-initiale.md`) restent surchargeables via variable d'environnement
Quarkus standard (`fishola.xxx-yyy` → `FISHOLA_XXX_YYY`), à ajouter dans le
service `fishola-backend` de `docker-compose.yml` et dans
`ansible/templates/dotenv.j2` si besoin.

## Limites actuelles

- Superset et Dagster (visés par l'architecture cible, cf. `CLAUDE.md` §2.2)
  ne sont pas encore implémentés dans ce dépôt : cette stack ne couvre que
  l'existant (celui que `start_all.sh` lance en local).
- Sauvegardes (§6) : pas encore de copie hors serveur ni d'alerte
  automatique en cas d'échec, à arbitrer avec Adventiel.
