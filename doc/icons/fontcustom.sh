#!/bin/bash

# On commence par s'arrurer de la présence du dossier de sortie
rm -rf output
mkdir output

# Construction d'un Docker avec le paquet Debian
docker build -t codelutin/fontcustom .

# Démarrage du conteneur, génération et arrêt
#docker run --rm --name fc -v $(pwd):/work -ti -d thomaswelton/fontcustom
docker run --rm --name fc -v $(pwd):/work -ti -d codelutin/fontcustom
docker exec -it fc fontcustom compile /work -c /work/fontcustom.yml -o /work/output
docker exec -it fc chown -R 1000.1000 /work/output
docker stop fc

# Installe la font générée
cp output/Fishola-Icons.ttf ../../fishola-mobile/public/fonts/
cp output/Fishola-Icons.svg ../../fishola-mobile/public/fonts/
cp output/Fishola-Icons.woff ../../fishola-mobile/public/fonts/
cp output/Fishola-Icons.woff2 ../../fishola-mobile/public/fonts/
cp output/Fishola-Icons.eot ../../fishola-mobile/public/fonts/

# Installe le CSS généré après l'avoir rectifié
cat output/Fishola-Icons.css | sed 's|url("./Fishola-Icons|url("~/public/fonts/Fishola-Icons|g' > ../../fishola-mobile/src/less/_icons.less

