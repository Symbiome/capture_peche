# Module back de Fishola

## Démarrer la base de données 

Il suffit de lancer le script `start_db.sh` :

```bash
./start_db.sh
```

Il est ensuite possible de se connecter à la base de données avec `psql` :

```bash
./psql.sh
```

La base démarrée est une base PostgreSQL 11 dont le volume est monté dans `/home/postgres-11`.

Pour connaître l'IP de la base de données :

```bash
./.get_db_ip.sh
```

## Démarrer le backend en mode dev

Le backend s'attend à joindre la base de données par le nom d'host `docker_pg`.
Si ce n'est donc pas déjà fait, il faut ajouter une ligne dans `/etc/hosts` telle que :

```
172.17.0.2	docker_pg
```

Ensuite on démarre Quarkus en mode dev :

```bash
mvn clean compile quarkus:dev
```

Quarkus tourne sur le port `8080`, on peut vérifier que tout va bien grâce au [endpoint status](http://localhost:8080/api/v1/status).
