# Mise en production

## Base de données

L'application est prévue pour tourner sur PostgreSQL.
Nous recommandons d'utiliser la dernière version disponible (12) toutefois l'application peut fonctionner avec des versions plus anciennes si le déploiement d'une version 12 n'est pas envisageable.
Pour la mise en prod, il suffit de créer la base de données.
Il n'est pas nécessaire d'initialiser le schéma de base de données, c'est l'application qui va s'en charger.
 
## Backend

Le livrable du backend contient :
 - un JAR `fishola-backend-VERSION-runner.jar` ;
 - un dossier `lib` contenant les dépendances ;
 - un dossier `config` contenant la configuration de l'application.

### Configuration du backend

Le dossier `config` doit contenir un fichier `application.properties`.
Ce fichier est lu au démarrage de l'application.

Il y a 4 propriétés obligatoires à définir sans quoi l'application ne peut pas démarrer :
 - `quarkus.datasource.url`. L'URL JDBC de la base de données telle que `jdbc:postgresql://HOST:PORT/DATABASE`. Exemple : `jdbc:postgresql://docker_pg:5432/fishola`
 - `quarkus.datasource.username`. Le nom d'utilisateur pour la base de données
 - `quarkus.datasource.password`. Le mot de passe pour la base de données
 - `fishola.jwt-secret`. La clé utilisée pour les token JWT. C'est une chaîne de caractère arbitraire. Dès lors que cette chaîne change, les token JWT générés ne seront plus valides et les utilisateurs seront donc tous déconnectés de l'application.

Il est fortement recommandé de renseigner également la propriété `fishola.backend-base-url` qui permet au backend de savoir quelle est sont adresse publique (pour l'envoi de mails entre autres).

Au delà de ces 4 propriétés, il y a beaucoup de propriétés pour ajuster la configuration du backend :

Proriété                 | Description                                                           | Valeur par défaut
------------------------ | --------------------------------------------------------------------- | -----------------
quarkus.datasource.url | L'URL JDBC de la base de données telle que `jdbc:postgresql://HOST:PORT/DATABASE` | -
quarkus.datasource.username | Le nom d'utilisateur pour la base de données | -
quarkus.datasource.password | Le mot de passe pour la base de données | -
quarkus.datasource.driver | Driver utilisé pour l'accès à la BDD | org.postgresql.Driver
quarkus.datasource.min-size=3
quarkus.datasource.max-size=10
fishola.jwt-secret | La clé utilisée pour les token JWT | -
fishola.password-hash-cost | Cout exponentiel de la génération du HASH pour les mot de passe (bcrypt). | 12
fishola.jwt-lifetime-hours | Durée (en heures) des tokens JWT | 24
fishola.Jwt-renewal-hours | Durée (en heures) pendant laquelle les renouvellements de tokens JWT sont acceptés | 168
fishola.trip-modifiable-mours | Durée (en heures) pendant laquelle les sorties sont modifiables | 168
fishola.export-safe-hours | Durée (en heures) pendant laquelle les sorties ne sont pas inclues dans le CSV d'export | 168
fishola.backend-base-url | URL d'accès au backend | - 
fishola.mail-from | Adresse apparaissant comme expéditeur des emails | fishola@codelutin.com
fishola.smtp-username | Nom d'utilisateur pour le serveur SMTP (si nécessaire) | -
fishola.smtp-password | Mot de passe pour le serveur SMTP (si nécessaire) | -
fishola.smtp-starttls | Si le SMTP requiert starttls | false
fishola.smtp-host | Hôte d'accès au serveur SMTP | localhost
fishola.smtp-port | Port d'accès au serveur SMTP | 25
fishola.async-emails | Est-ce que l'envoi d'email se fait en ascynchrone | true
fishola.async-emails-every | Durée entre chaque salve d'envoi de mails | 30s
fishola.async-emails-retention-minutes | Durée (en minutes) de conservations des emails en erreur | 60
fishola.raw-image-quality | Qualité des images stockées en base | .90f
fishola.pictures-preview-folder-path | Chemin où sont stockées les images de preview | /tmp/fishola-pictures
fishola.feedback-mail-to | Adresse de destination pour les feedbacks | fishola-lutins@list.forge.codelutin.com
fishola.auto-verify-accounts | Faut-il vérifier automatiquement les nouveaux comptes | false
