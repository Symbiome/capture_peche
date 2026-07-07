# Installation — Fishola (outil_capture)

Guide d'installation complet, depuis une machine Linux vierge (Ubuntu/Debian), pour faire tourner en local :
- la base de données PostgreSQL
- le backend Java/Quarkus (`fishola-backend`)
- le front pêcheur Vue.js (`fishola-mobile`)
- le front admin Vue.js (`fishola-admin`)

## 1. Paquets système de base

```bash
sudo apt update
sudo apt install -y git curl ca-certificates gnupg build-essential
```

## 2. Docker (pour PostgreSQL et le mailcatcher)

```bash
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

> Déconnectez-vous/reconnectez-vous (ou `newgrp docker`) pour que l'ajout au groupe `docker` soit pris en compte sans `sudo`.

## 3. Java 25 (LTS) + Maven

Quarkus 3.37 nécessite Java 17 minimum ; on cible ici **Java 25 LTS** (support long terme, recommandé pour un système de production plutôt que la dernière version non-LTS).

```bash
sudo apt install -y openjdk-25-jdk maven
java -version
mvn -version
```

## 4. Node.js 24 (LTS) + npm

Le build pin `nodeVersion v24.18.0` / `npmVersion 11.16.0` (voir `fishola-mobile/pom.xml` et `fishola-admin/pom.xml`) — Node 24 est la ligne **Active LTS**. On installe Node via `nvm` pour matcher cette version précisément.

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
source ~/.bashrc

nvm install 24.18.0
nvm use 24.18.0
npm install -g npm@11.16.0

node -v
npm -v
```

## 5. Récupération du code source

```bash
git clone git@github.com:Symbiome/capture_peche.git outil_capture
cd outil_capture
```

## 6. Build initial du POM parent

```bash
mvn clean install
```

## 7. Démarrer la base de données PostgreSQL

```bash
cd fishola-backend
./start_db.sh
```

Ce script lance un conteneur `postgis/postgis:18-3.6-alpine` (PostgreSQL 18 + PostGIS 3.6) avec la base `fishola`, exposée sur le port hôte `15432`.

Vérifier la connexion (nécessite `psql` installé sur l'hôte) :

```bash
psql postgresql://postgres:whatever@localhost:15432/fishola
```

## 8. Configurer et démarrer le backend

Créer le fichier `.env` à la racine de `fishola-backend` (non versionné) :

```bash
cat > .env <<'EOF'
_DEV_QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:15432/fishola
_DEV_QUARKUS_DATASOURCE_USERNAME=postgres
_DEV_QUARKUS_DATASOURCE_PASSWORD=whatever
EOF
```

(Optionnel) Démarrer un mailcatcher local pour intercepter les mails envoyés en dev :

```bash
docker run -p 41080:80 -p 41025:25 -d --name maildev --rm djfarrelly/maildev
```

Démarrer Quarkus en mode dev :

```bash
mvn clean compile quarkus:dev
```

Le backend écoute sur le port `8080`. Vérifier :

```bash
curl http://localhost:8080/api/v1/status
```

L'IHM du mailcatcher est accessible sur [http://localhost:41080](http://localhost:41080).

## 9. Démarrer le front pêcheur (fishola-mobile)

Dans un nouveau terminal :

```bash
cd outil_capture/fishola-mobile
npm install
npm run serve
```

Application accessible sur [http://localhost:8081](http://localhost:8081).

## 10. Démarrer le front admin (fishola-admin)

Dans un nouveau terminal :

```bash
cd outil_capture/fishola-admin
npm install
npm run serve
```

Application accessible sur [http://localhost:8082](http://localhost:8082).

## 11. Vérification finale

| Service | URL | Attendu |
|---|---|---|
| Backend (statut) | http://localhost:8080/api/v1/status | Réponse JSON OK |
| Front pêcheur | http://localhost:8081 | Page d'accueil Fishola |
| Front admin | http://localhost:8082 | Écran de connexion admin |
| Mailcatcher (optionnel) | http://localhost:41080 | Interface de réception des mails |

## Notes

- Si le port `8080` est déjà utilisé, changer `quarkus.http.port` dans `fishola-backend/src/main/resources/application.properties`, puis reporter ce nouveau port dans le `.env` du front (`VITE__API_DEFAULT_PORT`).
- Les fichiers `.env`, `.env.web`, `.env.demo`, `.env.mobile` sont déjà présents dans `fishola-mobile/` et `fishola-admin/` avec des valeurs par défaut pour le mode dev — inutile de les recréer.
- Les migrations Flyway (`fishola-backend/src/main/resources/db/migration/`) s'appliquent automatiquement au démarrage de Quarkus en mode dev.
