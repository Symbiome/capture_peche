# Guide de publication sur Android

## Builder l'application web

Se rendre à la racine du projet (e.g. /Users/username/Documents/Git/fishola)
mvn clean package (ou mvn clean package -pl fishola-mobile -DjavaVersion=1.8 pour builder uniquement le mobile)
cd fishola-mobile
mv target/dist-production target/dist (ou mv target/dist-demo target/dist pour demo)
npx cap copy

## Vérifier la version

npx cap open android va ouvrir android studio sur le bon workspace (au path défini dans capacitor.config.json attribut linuxAndroidStudioPath)
vous pouvez cliquer sur la flêche verte en haut à droite pour lancer un simulateur
le mieux est de brancher directement votre téléphone android en mode debug et lancer directement l'application dessus
tester à minima de vous connecter et de vérifier que vous êtes sur le bon serveur (demo ou prod)

## Packager la version

### Via gitlab

Lancer la tâche package:android-demo ou package:android-production et télécharger l'artifact.
Le fichier apk est installable sur n'importe quel appareil android

### En local (où cas où gitlab serait en carafe)

Si vous ne l'avez pas encore fait, allez récupérer les variables gitlab KEYSTORE_FISHOLA_JKS et SIGNING_CONFIG
pour créer les fichiers keystore-fishola.jks et app/signin.properties dans le dossier android

cd android
cat ${KEYSTORE_FISHOLA_JKS} | base64 -d > keystore-fishola.jks
cat ${SIGNING_CONFIG} > app/signin.properties

Une fois ces fichiers crééer, vous pouvez aller dans android studio > Build > Generation signed bundle/apk
Sélectionner "APK" > next
dans keystore path, indiquer l'emplacement du fichier jsk
En mot de passe du fichier, indiquez la valeur de RELEASE_STORE_PASSWORD du fichier signin.properties
En alias, indiquez la valeur de RELEASE_KEY_ALIAS du fichier signin.properties
En mot de passe de l'alais, indiquez la valeur de RELEASE_KEY_PASSWORD du fichier signin.properties

En variant de build, sélectionnez demoRelease (pour demo) ou productionRelease (pour prod)

Une fois le build réussi, un message "APK(s) generated successfully for module 'android.app' with 1 build variant" devrait être suivi d'un lien "locate" pour ouvrir l'explorateur de fichier sur le build généré (un fichier apk)

Les testeurs pourront installer cet apk sur leurs téléphones (vous pouvez par exemple le partager via un lien de partage sur nextcloud)

## Envoyer la version sur le Playstore

Se connecter sur https://play.google.com/console/u/0/signup
avec les identifiants indiqués dans le wiki

Cliquer sur Fishola / Production / Créer une release (ou modifier la release si une release est déjà en cours)

Uploader l'apk obtenu à l'étape précédente

Cliquer sur "Examiner la release" puis "Lancer le dépoiement en production lorsque vous souhaitez que l'apk devienne disponible"