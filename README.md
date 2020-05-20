# Fishola

Application smartphone en sciences participatives pour la pêche amateur sur les lacs de l’observatoire OLA

## Pré-requis

Les prérequis sont :
* OpenJDK 14+
* Maven 3.6.2+
* Node v12.16.1
* NPM 6.13.4
* Docker

À noter qu'il est possible de construire le front sans installer `Node` & `NPM`, mais il ne sera pas possible de
démarrer le projet en mode dev

## Démarer le projet (mode dev)

Il est recommandé de lancer une première fois la compilation pour installer le POM parent :

```bash
mvn clean install
```

Ensuite il faut démarrer :
* [le backend (avec sa base de données)](/fishola-backend/README.md)
* [le front](/fishola-mobile/README.md)

## Déploiement sur démo

Il faut commencer par builder le projet pour la prod :

```bash
mvn clean install
```

Pour la suite on suppose qu'on a dans `~/.ssh/config` une config qui porte le nom `jaipur-demo4`.

Envoi du projet via `rsync` :

```bash
rsync -rlptD --del * jaipur-demo4:/var/local/demo4/inrae/fishola/build/fishola/  --exclude='**/node*'
```

Déploiement :

```bash
ssh jaipur-demo4 "cd /var/local/demo4/inrae/fishola && ./restart.sh"
```

