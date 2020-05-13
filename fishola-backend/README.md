# Module back de Fishola

## Démarrer la base de données 

Il suffit de lancer le script `start_db.sh` :

```bash
./start_db.sh
```

Il est ensuite possible de se connecter à la base de données avec `psql` (si installée sur l'OS) :

```bash
./psql.sh
```

ou bien par la commande `psql` via Docker : 

```bash
./psql-by-docker.sh
```

La base démarrée est une base PostgreSQL 12 dont le volume est monté dans `/home/postgres-12`.

Pour connaître l'IP de la base de données :

```bash
./.get_db_ip.sh
```

## Démarrer le backend en mode dev

Les variables attendues par l'application pour joindre la bade de données sont :

```properties
%dev.quarkus.datasource.url=jdbc:postgresql://192.168.1.86:5432/fishola
%dev.quarkus.datasource.username=postgres
%dev.quarkus.datasource.password=whatever
```

`%dev` signifie que ces valeurs sont valables uniquement pour le mode `dev`.

Le meilleur moyen de saisir ces variables est de créer un fichier `.env` à la racine du module `fishola-backend` et y ajouter les lignes suivantes :

```properties
_DEV_QUARKUS_DATASOURCE_URL=jdbc:postgresql://192.168.1.86:5432/fishola
_DEV_QUARKUS_DATASOURCE_USERNAME=postgres
_DEV_QUARKUS_DATASOURCE_PASSWORD=whatever
```

Ce fichier n'est pas ajouté au repo Git, chacun est donc libre d'y surcharger les propriétés qu'il souhaite.

Ensuite on démarre Quarkus en mode dev :

```bash
mvn clean compile quarkus:dev
```

Quarkus tourne sur le port `8080`, on peut vérifier que tout va bien grâce au [endpoint status](http://localhost:8080/api/v1/status).

