# Génération d'une font d'icônes à partir de SVGs

On utilise [FontCustom](https://github.com/FontCustom/fontcustom).

Pour ne pas avoir à installer `Ruby`, on créé une image `Docker` contenant le paquet.

## Usage

```bash
./fontcustom.sh
```

Le script va :
- Créer l'image Docker ;
- lire l'ensemble des fichiers `.svg` présents dans le dossier ;
- les convertir en font ;
- installer cette font dans le projet.

Le résultat peut être vu dans `output/Fishola-Icons-preview.html`

Il suffit ensuite d'utiliser les icônes dans le HTML tel que :

```html
<i class="icon-fish"/>
```

## Configuration

La génération est configurable dans le fichier [fontcustom.yml](/doc/icons/fontcustom.yml).

Plus d'informations pour la configuration [sur GitHub](https://github.com/FontCustom/fontcustom/blob/master/lib/fontcustom/templates/fontcustom.yml).
