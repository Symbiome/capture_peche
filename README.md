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

Sur chaque pipeline il y a une tâche permettant de construire les images Docker contenant l'application : `package:docker-demo`.
Une fois que les images sont construites, elles sont disponibles dans le registry. Il suffit ensuite d'utiliser la tâche `deploy:demo` pour déclencher un rechargement de l'application.
Quelques instannts plus tard, l'application nouvellement déployée sera utilisable à l'adresse : https://fishola.demo.codelutin.com.

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
