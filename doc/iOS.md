# Guide de publication sur iOS

## Builder l'application web

S'assurer que Docker est bien lancé (commande espace "Docker")
Se rendre à la racine du projet (e.g. /Users/username/Documents/Git/fishola)
mvn clean package (ou mvn clean package -pl fishola-mobile -DjavaVersion=1.8 pour builder uniquement le mobile)
cd fishola-mobile
mv target/dist-production target/dist (ou mv target/dist-demo target/dist pour demo)
npx cap copy
gist stash (pour supprimer la modif de capacitor.config.json)

Selon à quel serveur vous souhaitez pouvoir vous connecter, il faut vous assurer que le fichier ios/App/App/capacitor.config.json contient bien la bonne url : 
"linuxAndroidStudioPath": "/usr/local/android-studio/bin/studio.sh",
	"server" : {
		"hostname": "fishola.demo.codelutin.com" // demo
        "hostname": "api-fishola.inrae.fr" // prod
	},
"plugins": {

## Vérifier la version
Lancez xcode et ouvrir le workspace Fishola
dans le menu en haut, choisir un modèle de simulateur d'iphone à lancer (e.g. "iPhone 8 plus ios 15.2")
cliquer sur le bouton play pour lancer l'application sur le simulateur
Passé la phase de build, l'application va se lancer dans le simulateur (si le simulateur est masqué, commande espace "Simulator")
Tester l'application, à minima le login pour vérifier les cookies et vérifier le sha1 de commit dans les retours
exemple de login : lagarde.alex@gmail.com / azerty

## Packager la version
Une fois tous les tests effectués, cliquer sur "App" en haut du project explorer à gauche
Dans l'éditeur, incrémenter le numéro de build
Dans la sélection de plateforme (là où vous aviez sélectionné iPhone 8 plus pour les tests), sélectionner "Generic iOS Device" (requis pour pouvoir archiver)
Menu Product > Archive (ça dure une plombe)

## Envoyer la version sur itunesconnect
Une fois l'archive effectuée, l'organizer apparait (vous pouvez le rouvrir avec Window > Organizer)
Sélectionnnez "App", votre archive avec le numéro de build est censée être en haut de la liste
Cliquez sur "Distribute App"
Sélectionnez App Store Connect (choix par défaut) > Next
Upload (choix par défaut) > Next
[Attendre]
Laissez les 3 checkbox cochées (choix par défaut) > Next
Automatically manage signing (choix par défaut) > Next
[Attendre]
Cliquer sur "Upload"

## Publier le build en béta
Se connecter sur applestoreconnect.com avec les identifiants indiqués dans le wiki Fishola
Sélectionner l'application Testflight
Le build que vous avez soumis apparaitra dans l'onglet "Testflight"
Si l'indication "Traitement en cours" apparaît, attendez quelques minutes
L'appelation "attestations manquantes" apparait, cliquez sur "Gérer" puis Oui/Oui/Commencer les tests internes)

A ce stade le build est disponible à tous les testeurs internes.

Pour le publier en béta, cliquez sur "beta" sous testeurs et groupes
A côté de "Builds", cliquer sur le "+" pour ajouter votre build aux builds betas
Ajoutez un texte indiquant les nouveautés

## Publier le build en release
Une fois la béta bien testée
Cliquer sur l'onglet "App Store", puis dans la section build sélectionnez le build souhaité
Assurez vous que la publication est bien en "manuelle" (ça signifie que quand Apple valide le build, c'est vous qui décidez manuellement de le promouvoir)
A la question "votre application utilise-t-elle un un identifiant publicitaire IDFA" répondez "Non"
Cliquez sur soumettre
Quand l'application sera validée, vous recevrez un mail, et vous pourrez publier la version en en clic

## Troubleshooting

Il est possible que la soumission échoue parce que le nom de version ne doit pas être identique à une version publiée. Dans ce cas, éditer le "version name" en plus du build number (e.g. "1.0.2")