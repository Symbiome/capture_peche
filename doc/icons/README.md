# Génération d'une font d'icônes à partir de SVGs

On utilise [FontCustom](https://github.com/FontCustom/fontcustom).

Pour ne pas avoir à installer Ruby, on créé une image Docker contenand le paquet.

## Usage

    ./fontcustom.sh

Le script va lire l'ensemble des fichiers `.svg` présents dans le dossier, les convertir en font puis installer cette
font dans le projet.

Le résultat peut être vu dans `output/Fishola-Icons-preview.html`


