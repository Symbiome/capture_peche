# Fishola

Application smartphone en sciences participatives pour la pêche amateur sur les lacs de l’observatoire OLA

## Pré-requis

Les prérequis sont :
* OpenJDK 15+
* Maven 3.6.2+
* Node v12.18.3
* NPM 6.14.6
* Docker

À noter qu'il est possible de construire le front sans installer `Node` & `NPM`, mais il ne sera pas possible de
démarrer le projet en mode dev.

## Démarer le projet (mode dev)

Il est recommandé de lancer une première fois la compilation pour installer le POM parent :

```bash
mvn clean install
```

Ensuite il faut démarrer :
* [le backend (avec sa base de données)](/fishola-backend/README.md)
* [le front](/fishola-mobile/README.md)
* [l'interface d'administration](/fishola-admin/README.md)


## Déploiement sur démo

Sur chaque pipeline il y a une tâche permettant de construire les images Docker contenant l'application : `package:docker-demo`.
Une fois que les images sont construites, elles sont disponibles dans le registry. Il suffit ensuite d'utiliser la tâche `deploy:demo` pour déclencher un rechargement de l'application.
Quelques instants plus tard, l'application nouvellement déployée sera utilisable à l'adresse : https://fishola-mobile.demo.codelutin.com.


### Releaser la version

Se mettre sur `develop` et initier le process de release :

```bash
mvn gitflow:release-start
```

Il faut maintenant vérifier la version avec la commande :

```bash
mvn clean verify -DperformRelease
```

Une fois la vérification terminée, il est possible qu'il y ait des modifications à faire pour corriger le build.

Si tout va bien (`BUILD SUCCESS`) examinez les fichiers modifiés et les commiter le cas échéant (Exemple : headers de fichiers générés, etc.).

Enfin, terminer la release avec la commande :

```bash
mvn gitflow:release-finish
```  

Celle-ci va merger la branche sur `develop`, créer le tag et mettre à jour `master`.

De façon automatisée, le Gitlab CI va construire l'image Docker associée au tag.

## Mise en production

L'application FISHOLA est composée de 5 éléments :
 - 1 base de donnée PostgreSQL
 - 1 serveur SMTP
 - 1 backend en JAVA
 - 1 application mobile Android
 - 1 application mobile iOs

Le déploiement est détaillé dans le [Guide de mise en production](/doc/MEP.md)

## Licence

L'application FISHOLA est sous licence [GNU Affero General Public License v3](/LICENSE.txt).
