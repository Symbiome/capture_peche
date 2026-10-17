#!/bin/bash

# On commence par s'arrurer de la présence du dossier de sortie
rm -rf output
mkdir output

docker run --rm --name fc -v $(pwd):/work -ti -d thomaswelton/fontcustom
docker exec -it fc fontcustom compile /work -c /work/fontcustom.yml -o /work/output
docker stop fc

# Installe la font générée
cp output/Carnet-Icons.ttf ../../fishola-mobile/public/fonts/
cp output/Carnet-Icons.svg ../../fishola-mobile/public/fonts/
cp output/Carnet-Icons.woff ../../fishola-mobile/public/fonts/
# cp output/Carnet-Icons.woff2 ../../fishola-mobile/public/fonts/
cp output/Carnet-Icons.eot ../../fishola-mobile/public/fonts/

# Installe le CSS généré après l'avoir rectifié
cat output/Carnet-Icons.css \
  | sed 's|url("./Carnet-Icons|url("fonts/Carnet-Icons|g' \
  > ../../fishola-mobile/src/less/_icons.less

sudo rm -rf output
