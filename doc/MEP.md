# Guide de mise en production en l'application FISHOLA

## Base de données

L'application est prévue pour tourner sur PostgreSQL.
Nous recommandons d'utiliser la dernière version disponible (12) toutefois l'application peut fonctionner avec des versions plus anciennes si le déploiement d'une version 12 n'est pas envisageable.
Pour la mise en prod, il suffit de créer la base de données.
Il n'est pas nécessaire d'initialiser le schéma de base de données, c'est l'application qui va s'en charger.

Pour information, les scripts d'initialisation de la base qui seront automatiquement appliqués au démarrage de l'application sont contenus dans le [dossier migration](/fishola-backend/src/main/resources/db/migration).

## Backend

Le livrable du backend contient :
 - un JAR `fishola-backend-VERSION-runner.jar` ;
 - un dossier `lib` contenant les dépendances ;
 - un dossier `config` contenant la configuration de l'application.

### Configuration du backend

Le dossier `config` doit contenir un fichier `application.properties`.
Ce fichier est lu au démarrage de l'application.

Il y a 5 propriétés obligatoires à définir sans quoi l'application ne peut pas démarrer :
 - `quarkus.datasource.jdbc.url` : L'URL JDBC de la base de données telle que `jdbc:postgresql://HOST:PORT/DATABASE`. Exemple : `jdbc:postgresql://docker_pg:5432/fishola`
 - `quarkus.datasource.username` : Le nom d'utilisateur pour la base de données
 - `quarkus.datasource.password` : Le mot de passe pour la base de données
 - `fishola.jwt-secret` : La clé utilisée pour les token JWT. C'est une chaîne de caractère arbitraire. Dès que cette chaîne change, les token JWT générés ne seront plus valides et les utilisateurs seront donc tous déconnectés de l'application ;
 - `fishola.smtp-host` : Hôte d'accès au serveur SMTP pour l'envoi de mails.

Il est fortement recommandé de renseigner également la propriété `fishola.backend-base-url` qui permet au backend de savoir quelle est son adresse publique (pour l'envoi de mails entre autres).

Au-delà de ces 5 propriétés, il y a beaucoup de propriétés pour ajuster la configuration du backend :

Proriété | Description | Valeur par défaut
-------- | ----------- | -----------------
quarkus.datasource.jdbc.url | L'URL JDBC de la base de données telle que `jdbc:postgresql://HOST:PORT/DATABASE` | -
quarkus.datasource.username | Le nom d'utilisateur pour la base de données | -
quarkus.datasource.password | Le mot de passe pour la base de données | -
quarkus.datasource.jdbc.min-size | Taille minimale du pool de connexions JDBC | 3
quarkus.datasource.jdbc.max-size | Taille maximale du pool de connexions JDBC | 20
quarkus.http.port | Port sur lequel le backend doit tourner | 8080
quarkus.http.http2 | Active ou non l'utilisation de HTTP/2 | false
quarkus.http.limits.max-body-size | Taille maximale des requêtes HTTP | 20M
fishola.jwt-secret | La clé utilisée pour les token JWT | -
fishola.jwt-lifetime-hours | Durée (en heures) des tokens JWT | 24
fishola.Jwt-renewal-hours | Durée (en heures) pendant laquelle les renouvellements de tokens JWT sont acceptés | 168
fishola.password-hash-cost | Cout exponentiel de la génération du HASH pour les mot de passe (bcrypt). | 12
fishola.trip-modifiable-mours | Durée (en heures) pendant laquelle les sorties sont modifiables | 168
fishola.export-safe-hours | Durée (en heures) pendant laquelle les sorties ne sont pas inclues dans le CSV d'export | 168
fishola.backend-base-url | URL d'accès au backend | - 
fishola.mail-from | Adresse apparaissant comme expéditeur des emails | fishola@codelutin.com
fishola.smtp-host | Hôte d'accès au serveur SMTP | -
fishola.smtp-port | Port d'accès au serveur SMTP | 25
fishola.smtp-username | Nom d'utilisateur pour le serveur SMTP (si nécessaire) | -
fishola.smtp-password | Mot de passe pour le serveur SMTP (si nécessaire) | -
fishola.smtp-starttls | Si le SMTP requiert starttls | false
fishola.async-emails | Est-ce que l'envoi d'email se fait en ascynchrone | true
fishola.async-emails-every | Durée entre chaque salve d'envoi de mails | 30s
fishola.async-emails-retention-minutes | Durée (en minutes) de conservations des emails en erreur | 60
fishola.raw-image-quality | Qualité des images stockées en base (Pourcentage) | .90f
fishola.pictures-preview-folder-path | Chemin où sont stockées les miniatures | /tmp/fishola-pictures
fishola.feedback-mail-to | Adresse de destination pour les feedbacks | fishola-lutins@list.forge.codelutin.com
fishola.auto-verify-accounts | Faut-il vérifier automatiquement les nouveaux comptes ? | false
fishola.admin-password | Mot de passe de l'interface d'admin | -
fishola.dashboard-only-current-year | Permet de restreindre l'affichage à l'année en cours sur le tableau de bord | true
fishola.key-figures-timeout-hours | Temps d'expiration (heures) des chiffres clés et données affichées sur la page d'accueil | 24
quarkus.log.console.enable | Faut-il activer les logs en console ? | false
quarkus.log.file.enable | Faut-il activer les logs en fichier ? | true
quarkus.log.file.path | Chemin + nom du fichier de logs | fishola-backend.log
quarkus.log.file.rotation.file-suffix | Suffixe utilisé pour la rotation des fichiers de logs | .yyyy-MM-dd
quarkus.log.file.rotation.max-backup-index | Nombre max de fichiers de rotation à retenir pour 1 journée | 10
quarkus.http.access-log.enabled | Faut-il inclure dans les logs les appels au backend ? | true

Plus d'informations sur
la [configuration du serveur HTTP](https://quarkus.io/guides/all-config#quarkus-vertx-http_quarkus-vertx-http) et
la [configuration des logs](https://quarkus.io/guides/all-config#quarkus-core_quarkus.log.file).

### Démarrage du backend

Il est recommandé d'utiliser le dernier JDK disponible (15).
Toutefois si son utilisation n'est pas envisageable, il est possible de d'utiliser un JDK 11.

Une fois le JDK installé, il faut lancer l'application depuis le dossier contenant le JAR , le dossier lib et le dossier config.

```bash
java -jar fishola-backend-VERSION-runner.jar
```

Par défaut, les logs de l'application sont dans un fichier `fishola-backend.log`.
Il est possible de vérifier que l'application fonctionne via l'URL : `http://SERVER:PORT/api/v1/status`

C'est peu recommandé mais il est possible de spécifier des paramètres directement au démarrage. Exemple avec un changement de port :

```bash
java -Dquarkus.http.port=5555 -jar fishola-backend-VERSION-runner.jar
```

### Exposition

Il est nécessaire que le backend soit accessible en HTTPS.
Bien que cette partie ne relève pas de la configuration du backend elle est nécessaire au déploiement en production pour que les applications mobiles puissent accéder au backend.

Il est nécessaire que le backend soit déployé à la racine du nom de domaine car des fichiers sont exposés selon certaines conventions (fixées par iOs).
Une fois le déploiement fait il convient de vérifier que le fichier `https://DOMAINE/.well-known/apple-app-site-association` est bien accessible. 

### Stockage

L'application stocke les informations dans la base de données (y compris les photos des capture).

Toutefois, le backend a besoin d'un emplacement pour stocker les miniatures des photos des captures.
Ce dossier est purgable à volonté : l'application va automatiquement recréer la miniature à la volée si elle n'est trouvée dans le dossier.
Ce dossier est configurable via l'option `fishola.pictures-preview-folder-path` dont la valeur par défaut est `/tmp/fishola-pictures`.

### Load balancing

Le backend de FISHOLA est stateless.
Il n'y a pas de cache partagé non plus.
Cela permet un déploiement de plusieurs instances en simultané pour répartir la charge.

Par exemple, démarrage de 3 instances du backend sur les ports 5555, 6666 et 7777 :

```bash
java -Dquarkus.http.port=5555 -jar fishola-backend-VERSION-runner.jar
java -Dquarkus.http.port=6666 -jar fishola-backend-VERSION-runner.jar
java -Dquarkus.http.port=7777 -jar fishola-backend-VERSION-runner.jar
```

La configuration suivante avec HAProxy permet de déployer un ELB qui s'adapte à la disponibilité de chacune des instances du backend :

```
frontend localnodes
	bind *:8080
	mode http
	default_backend nodes

backend nodes
	mode http
	balance roundrobin
	option forwardfor
	option httpchk HEAD /api/v1/status
	server backend1 127.0.0.1:5555 check
	server backend2 127.0.0.1:6666 check
	server backend3 127.0.0.1:7777 check
```

Il suffit ensuite d'appeler le backend via le port 8080, tel que `http://localhost:8080/api/v1/status`

## Applications mobile

Il n'est pas nécessaire de déployer la version web de l'application mobile puisque ce sont les applications Android & iOs directement déployées sur les Store qui assurent ce rôle.

Toutefois c'est possible, il suffit pour cela d'exposer le contenu du dossier `dist-production` dans un serveur HTTP (Apache/nginx/...).
Attention, la partie web n'est pas configurable comme le backend, la configuration se fait en amont de la création du dossier `dist-production`.

Si l'application web est déployée il est recommandé de le faire sur un nom de domaine différent du backend afin que les 2 puissent être déployés à la racine du nom de domaine.
