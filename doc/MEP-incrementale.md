# Déploiement d'une version stable en prod

## Préparation du livrable

Aller sur le pipeline du tag (exemple https://gitlab.nuiton.org/inrae/fishola/-/jobs/128799)
Télécharger les artefacts 

Décompresser le zip et ouvrir un terminal dedans.


Exécuter les commandes suivantes :

```bash
export VERSION=1.2.2
mkdir fishola-${VERSION}
mv fishola-backend/target/quarkus-app fishola-${VERSION}/fishola-backend-${VERSION}
mv fishola-mobile/target/dist-web fishola-${VERSION}/fishola-web-${VERSION}
mv fishola-admin/target/dist-production fishola-${VERSION}/fishola-web-${VERSION}/admin
zip -rq fishola-${VERSION}.zip fishola-${VERSION}
```

Le ZIP fishola-${VERSION}.zip est à uploader sur le serveur de prod
```bash
scp fishola-1.2.2.zip fishola-prod:/home/tomcat/deploiements/fishola
```

## Mise en place de la nouvelle version sur le serveur

Une fois sur le serveur, il faut installer les fichiers au bon endroit :

```bash
export VERSION=1.2.2
cd deploiements/fishola
unzip -q fishola-${VERSION}.zip
mv ~/deploiements/fishola/fishola-${VERSION}/fishola-backend-${VERSION} ~/docker-fishola/src/docker/compose/fishola-backend/api/
cd ~/docker-fishola/src/docker/compose/fishola-backend/api
rm current && ln -s fishola-backend-${VERSION} current
mv ~/deploiements/fishola/fishola-${VERSION}/fishola-web-${VERSION} ~/docker-fishola/src/docker/compose/nginx/
cd ~/docker-fishola/src/docker/compose/nginx/
rm html && ln -s fishola-web-${VERSION} html
```

Puis construire la stack :

```bash
cd ~/docker-fishola/src/docker/compose
docker-compose build --no-cache
```

## Déploiement effectif

Enfin déployer :

```bash
docker-compose down
docker-compose up -d
```

## Sauvegarde des modifications

Et commit/push les changements sur la forge mia

## Mise en production du back-office


Aller sur le pipeline du tag (exemple https://gitlab.nuiton.org/inrae/fishola/-/jobs/128799)
Télécharger les artefacts 

Décompresser le zip et ouvrir un terminal dedans.

Copier le contenu du dossier fishola-admin/target/dist-production 
dans demo4:/var/local/demo4/inrae/fishola-production/assets/fishola-admin-production (n'hésitez pas à faire un backup avant ^^)
depuis le dossier ~/demo4/inrae/fishola-production
docker-compose down
docker-compose build --no-cache
docker-compose up-d



